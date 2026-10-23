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
                var client = HttpClient.newHttpClient();
                var response = client.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll")).build(), HttpResponse.BodyHandlers.ofString());
                check(response.statusCode() == 200 && response.body().contains("\"total_records\":2"), "Dashboard JSON");
                check(response.body().contains("\"average_net\":4820.0"), "Dashboard summary");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/")).build(), HttpResponse.BodyHandlers.ofString()).body().contains("Payroll & Tax Dashboard"), "HTML resource");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/missing")).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 404, "Unknown route");
                check(client.send(HttpRequest.newBuilder(URI.create(base + "/api/payroll")).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 405, "Read-only API");
            } finally { server.stop(0); }
        } finally { Files.deleteIfExists(database); Files.deleteIfExists(directory); }
        System.out.println(checks + " checks passed.");
    }
}
