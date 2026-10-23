package payroll;

import java.nio.file.Files;
import java.net.URI;
import java.net.http.*;
import java.util.List;

public final class PayrollTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void invalid(Runnable action) {
        try { action.run(); throw new AssertionError("Invalid input accepted"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    private static String csrf(String json) {
        var matcher = java.util.regex.Pattern.compile("\"csrfToken\":\"([A-Za-z0-9_-]+)\"").matcher(json);
        if (!matcher.find()) throw new AssertionError("CSRF token missing");
        return matcher.group(1);
    }
    public static void main(String[] args) throws Exception {
        var result = Payroll.calculate(" Alice ", 5000, .2, 250);
        check(result.employee_name().equals("Alice"), "Trim name");
        check(result.gross_pay() == 5250 && result.tax_amount() == 1050 && result.net_pay() == 4200, "Calculation");
        check(Payroll.calculate("Zero", 0, 0, 0).net_pay() == 0, "Zero salary");
        check(Payroll.calculate("Full", 100, 1, 0).net_pay() == 0, "Full tax");
        invalid(() -> Payroll.calculate(" ", 1, .2, 0));
        invalid(() -> Payroll.calculate("A", -1, .2, 0));
        invalid(() -> Payroll.calculate("A", 1, .2, -1));
        invalid(() -> Payroll.calculate("A", 1, 1.1, 0));
        invalid(() -> Payroll.calculate("A", Double.NaN, .2, 0));
        invalid(() -> Payroll.calculate("A", 1, Double.NaN, 0));
        invalid(() -> Payroll.calculate("A", Double.POSITIVE_INFINITY, .2, 0));
        check(Payroll.summarize(List.of()).get("largest_employee") == null, "Empty summary");
        check(Json.stringify("a\"\\\n\t").equals("\"a\\\"\\\\\\n\\t\""), "JSON escaping");
        var sample = List.<java.util.Map<String, Object>>of(
            java.util.Map.of("employee_name", "Alice", "gross_pay", 5250.0, "tax_amount", 1050.0, "net_pay", 4200.0),
            java.util.Map.of("employee_name", "Bob", "gross_pay", 6800.0, "tax_amount", 1360.0, "net_pay", 5440.0));
        check(Payroll.summarize(sample).get("average_net").equals(4820.0), "Summary average");
        check(Payroll.summarize(sample).get("total_gross").equals(12050.0), "Summary gross");
        check(Payroll.summarize(sample).get("largest_employee").equals("Bob"), "Summary largest");
        if (args.length == 1 && args[0].equals("--core-only")) {
            System.out.println(checks + " core checks passed; SQLite/HTTP integration not run.");
            return;
        }
        var directory = Files.createTempDirectory("java-payroll-test");
        var database = directory.resolve("test.db");
        try {
            var repository = new PayrollRepository(database);
            check(repository.save(result) == 1, "Generated id");
            repository.save(Payroll.calculate("Bob <script> \" ", 6500, .2, 300));
            var rows = new PayrollRepository(database).findAll();
            check(rows.size() == 2 && rows.get(0).get("id").equals(2), "Persistence and ordering");
            var summary = Payroll.summarize(rows);
            check(summary.get("total_gross").equals(12050.0), "Total gross");
            check(summary.get("total_tax").equals(2410.0), "Total tax");
            check(summary.get("average_net").equals(4820.0), "Average net");
            check(summary.get("largest_employee").equals(rows.get(0).get("employee_name")), "Largest employee");
            var server = DashboardServer.start(repository, 0);
            try {
                String base = "http://127.0.0.1:" + server.getAddress().getPort();
                var cookieManager = new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL);
                var client = HttpClient.newBuilder().cookieHandler(cookieManager).build();
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll")).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 401, "Payroll requires login");
                var initialSession = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/session")).build(), HttpResponse.BodyHandlers.ofString());
                check(initialSession.body().contains("\"setupRequired\":true"), "First-run account setup");
                String csrf = csrf(initialSession.body());
                var setup = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/setup"))
                    .header("X-CSRF-Token", csrf).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("username=testadmin&password=test-password-123")).build(), HttpResponse.BodyHandlers.ofString());
                check(setup.statusCode() == 201 && setup.body().contains("\"authenticated\":true"), "Create workspace account");
                check(setup.headers().firstValue("Set-Cookie").orElse("").contains("HttpOnly; SameSite=Strict"), "Protected session cookie");
                csrf = csrf(setup.body());
                var response = client.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll")).build(), HttpResponse.BodyHandlers.ofString());
                check(response.statusCode() == 200 && response.body().contains("\"total_records\":2"), "Dashboard JSON");
                check(response.body().contains("\"average_net\":4820.0"), "Dashboard summary");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/")).build(), HttpResponse.BodyHandlers.ofString()).body().contains("Payroll overview"), "HTML resource");
                for (String asset : List.of("dashboard.css", "dashboard.js", "payroll-api.js")) {
                    var resource = client.send(HttpRequest.newBuilder(URI.create(base + "/" + asset)).build(), HttpResponse.BodyHandlers.ofString());
                    check(resource.statusCode() == 200 && !resource.body().isBlank(), "Static asset: " + asset);
                    check(resource.headers().firstValue("Content-Type").orElse("").startsWith(asset.endsWith(".css") ? "text/css" : "text/javascript"), "Asset MIME type: " + asset);
                }
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/missing")).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 404, "Unknown route");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll")).header("X-CSRF-Token", csrf).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 415, "Require form content type");
                String form = "employee_name=New+Employee&monthly_salary=1000&bonus=100&tax_rate=0.2";
                var created = client.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll"))
                    .header("X-CSRF-Token", csrf).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
                check(created.statusCode() == 201 && created.body().contains("\"id\":3"), "Create payroll API");
                check(repository.findAll().get(0).get("net_pay").equals(880.0), "Server calculates net pay");
                var changed = client.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll/3"))
                    .header("X-CSRF-Token", csrf).header("Content-Type", "application/x-www-form-urlencoded").PUT(HttpRequest.BodyPublishers.ofString(form.replace("1000", "2000"))).build(), HttpResponse.BodyHandlers.ofString());
                check(changed.statusCode() == 200 && repository.findAll().get(0).get("net_pay").equals(1680.0), "Update and recalculate");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll"))
                    .header("X-CSRF-Token", csrf).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form.replace("1000", "-1"))).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 400, "Reject invalid salary");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll/3"))
                    .header("Origin", "https://example.com").header("X-CSRF-Token", csrf).DELETE().build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 403, "Reject cross-origin mutation");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll/3")).header("X-CSRF-Token", csrf).DELETE().build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200, "Delete payroll API");
                check(repository.findAll().size() == 2, "Deletion persisted");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll/3")).header("X-CSRF-Token", csrf).DELETE().build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 404, "Missing record status");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/setup"))
                    .header("X-CSRF-Token", csrf).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("username=other&password=test-password-456")).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 409, "Cannot replace account with setup");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/logout")).header("X-CSRF-Token", csrf).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200, "Logout");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll")).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 401, "Logout revokes access");
                csrf = csrf(client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/session")).build(), HttpResponse.BodyHandlers.ofString()).body());
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                    .header("X-CSRF-Token", csrf).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("username=testadmin&password=incorrect")).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 401, "Reject wrong password");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                    .header("X-CSRF-Token", csrf).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("username=testadmin&password=test-password-123")).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200, "Login with saved credentials");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll")).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200, "Login restores payroll access");
                var anonymous = HttpClient.newHttpClient();
                check(anonymous.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll/1")).header("X-CSRF-Token", csrf).DELETE().build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 401, "Anonymous writes rejected");
            } finally { server.stop(0); }
        } finally { Files.deleteIfExists(database); Files.deleteIfExists(directory); }
        checks += SecurityTest.run();
        System.out.println(checks + " checks passed.");
    }
}
