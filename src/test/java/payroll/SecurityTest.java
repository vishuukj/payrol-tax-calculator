package payroll;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Security regressions use a disposable workspace and a controllable clock. */
public final class SecurityTest {
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static final class TestClock extends Clock {
        Instant time = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return time; }
    }
    private static String token(String json) {
        var match = java.util.regex.Pattern.compile("\"csrfToken\":\"([A-Za-z0-9_-]+)\"").matcher(json);
        if (!match.find()) throw new AssertionError("Expected CSRF token");
        return match.group(1);
    }
    private static HttpResponse<String> request(HttpClient client, String url, String method, String cookie, String csrf, String body, String origin) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(url));
        if (cookie != null) request.header("Cookie", cookie);
        if (csrf != null) request.header("X-CSRF-Token", csrf);
        if (origin != null) request.header("Origin", origin);
        if (!method.equals("GET")) request.header("Content-Type", "application/x-www-form-urlencoded");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    public static int run() throws Exception {
        checks = 0;
        Path directory = Files.createTempDirectory("payroll-security-");
        Path database = directory.resolve("security.db");
        var repository = new PayrollRepository(database);
        var clock = new TestClock();
        var server = DashboardServer.start(repository, 0, clock);
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        var client = HttpClient.newHttpClient();
        String secret = "unique-password-not-for-logs-734";
        try {
            for (String method : List.of("GET", "POST", "PUT", "DELETE")) {
                String endpoint = Set.of("GET","POST").contains(method) ? "/api/payroll" : "/api/payroll/1";
                check(request(client,base+endpoint,method,null,null,"",null).statusCode()==401,"Anonymous " + method + " rejected");
            }
            var before = request(client,base+"/api/auth/session","GET",null,null,null,null);
            String initialCookie = before.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
            String initialCsrf = token(before.body());
            String credentials = "username=securityadmin&password="+secret;
            check(request(client,base+"/api/auth/setup","POST",initialCookie,null,credentials,null).statusCode()==403,"Account setup requires CSRF");
            check(request(client,base+"/api/auth/setup","POST",initialCookie,initialCsrf,credentials,"https://evil.example").statusCode()==403,"Cross-site setup rejected");
            var setup = request(client,base+"/api/auth/setup","POST",initialCookie,initialCsrf,credentials,base);
            check(setup.statusCode()==201,"Account setup with CSRF succeeds");
            String cookie = setup.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
            String csrf = token(setup.body());
            check(!cookie.equals(initialCookie) && !csrf.equals(initialCsrf),"Login rotates session and CSRF token");
            check(request(client,base+"/api/payroll","GET",initialCookie,null,null,null).statusCode()==401,"Pre-login session cannot access payroll");
            String record = "employee_name=Sensitive+Employee&monthly_salary=12345.67&bonus=0&tax_rate=0.2";
            for (String method : List.of("POST","PUT","DELETE")) {
                String endpoint=method.equals("POST")?"/api/payroll":"/api/payroll/1";
                check(request(client,base+endpoint,method,cookie,null,record,null).statusCode()==403,"Missing CSRF on " + method);
                check(request(client,base+endpoint,method,cookie,"x".repeat(43),record,null).statusCode()==403,"Forged CSRF on " + method);
                check(request(client,base+endpoint,method,cookie,csrf,record,"https://evil.example").statusCode()==403,"Foreign origin on " + method);
            }
            check(request(client,base+"/api/auth/logout","POST",cookie,null,"",base).statusCode()==403,"Logout requires CSRF");
            check(request(client,base+"/api/auth/login","POST",cookie,null,credentials,base).statusCode()==403,"Login requires CSRF");
            check(request(client,base+"/api/payroll","POST",cookie,initialCsrf,record,base).statusCode()==403,"Old CSRF token rejected");
            check(request(client,base+"/api/payroll","POST",cookie,csrf,record,base).statusCode()==201,"Authorized mutation succeeds");
            check(request(client,base+"/api/payroll","GET","payroll_session=not-a-valid-token",null,null,null).statusCode()==401,"Malformed session rejected");
            check(request(client,base+"/api/payroll","GET","payroll_session="+"x".repeat(43),null,null,null).statusCode()==401,"Forged session rejected");
            check(request(client,base+"/api/payroll","GET",cookie+"; "+cookie,null,null,null).statusCode()==401,"Duplicate session cookies rejected");
            var data = request(client,base+"/api/payroll","GET",cookie,null,null,null);
            check(!data.body().contains("password_hash")&&!data.body().contains(secret),"Payroll API excludes account secrets");
            check(data.headers().firstValue("Cache-Control").orElse("").contains("no-store"),"Sensitive responses not cached");
            check(data.headers().firstValue("Content-Security-Policy").orElse("").contains("frame-ancestors 'none'"),"CSP protects embedding");
            check(data.headers().firstValue("X-Frame-Options").orElse("").equals("DENY"),"Framing denied");
            try (var connection=repository.connect(); var statement=connection.createStatement(); var result=statement.executeQuery("SELECT password_hash, password_salt FROM workspace_account")) {
                result.next(); check(!result.getString(1).equals(secret)&&Base64.getDecoder().decode(result.getString(1)).length==32,"Only password hash persisted");
                check(Base64.getDecoder().decode(result.getString(2)).length==16,"Random password salt persisted");
            }
            try (var socket=new Socket("127.0.0.1",server.getAddress().getPort())) {
                socket.setSoTimeout(3000);
                socket.getOutputStream().write("GET /api/auth/session HTTP/1.1\r\nHost: evil.example\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                String status=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII)).readLine();
                check(status.contains("403"),"Untrusted Host rejected");
            }
            clock.time=clock.time.plusSeconds(8*3600+1);
            check(request(client,base+"/api/payroll","GET",cookie,null,null,null).statusCode()==401,"Expired session rejected without waiting");
            check(request(client,base+"/api/payroll/1","DELETE",cookie,csrf,"",base).statusCode()==401,"Expired session cannot modify records");
            var anonymous=request(client,base+"/api/auth/session","GET",null,null,null,null);
            String anonCookie=anonymous.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
            var login=request(client,base+"/api/auth/login","POST",anonCookie,token(anonymous.body()),credentials,base);
            check(login.statusCode()==200,"Login after expiry succeeds");
            cookie=login.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];csrf=token(login.body());
            // Cause an internal SQL error; diagnostics must never contain request or account data.
            try(var connection=repository.connect();var statement=connection.createStatement()){statement.executeUpdate("DROP TABLE payroll_records");}
            var logs=new ByteArrayOutputStream();var stderr=System.err;
            HttpResponse<String> failed;
            try(var capture=new PrintStream(logs,true,StandardCharsets.UTF_8)){
                System.setErr(capture);
                failed=request(client,base+"/api/payroll","POST",cookie,csrf,record,base);
            }finally{System.setErr(stderr);}
            String diagnostic=logs.toString(StandardCharsets.UTF_8);
            check(failed.statusCode()==500 && failed.body().contains("Unable to complete request"),"Internal errors are generic");
            check(diagnostic.contains("incident=") && !diagnostic.contains(secret) && !diagnostic.contains("Sensitive Employee")
                    && !diagnostic.contains("12345.67") && !diagnostic.contains(csrf) && !diagnostic.contains(cookie.split("=")[1])
                    && !diagnostic.contains("DROP TABLE") && !diagnostic.contains("no such table"),"Diagnostics redact secrets and SQL details");
            check(request(client,base+"/api/auth/logout","POST",cookie,csrf,"",base).statusCode()==200,"CSRF-protected logout succeeds");
            check(request(client,base+"/api/auth/session","GET",cookie,null,null,null).body().contains("\"authenticated\":false"),"Revoked session stays signed out");
            System.out.println(checks+" security checks passed.");
            return checks;
        } finally {server.stop(0);Files.deleteIfExists(database);Files.deleteIfExists(directory);}
    }
}
