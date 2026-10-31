package payroll;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DashboardServer {
    private DashboardServer() {}
    public static HttpServer start(PayrollRepository repository, int port) throws IOException {
        return start(repository, port, java.time.Clock.systemUTC());
    }

    static HttpServer start(PayrollRepository repository, int port, java.time.Clock clock) throws IOException {
        AuthService auth;
        try { auth = new AuthService(repository, clock); }
        catch (java.sql.SQLException exception) { throw new IOException("Unable to initialize workspace account", exception); }
        byte[] html;
        try (var stream = DashboardServer.class.getResourceAsStream("/dashboard.html")) {
            if (stream == null) throw new IOException("Dashboard resource missing");
            html = stream.readAllBytes();
        }
        Map<String, byte[]> assets = new HashMap<>();
        for (String name : List.of("dashboard.css", "dashboard.js", "payroll-api.js")) {
            try (var stream = DashboardServer.class.getResourceAsStream("/" + name)) {
                if (stream == null) throw new IOException("Dashboard resource missing: " + name);
                assets.put("/" + name, stream.readAllBytes());
            }
        }
        var server = HttpServer.create(new InetSocketAddress(System.getenv().getOrDefault("BIND_HOST", "127.0.0.1"), port), 0);
        server.createContext("/", exchange -> {
            try {
                if (!RequestSecurity.allowedHost(exchange)) {
                    json(exchange, 403, Map.of("error", "Request host is not allowed."));
                    return;
                }
                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();
                boolean collection = path.equals("/api/payroll");
                boolean item = path.matches("/api/payroll/[1-9][0-9]*");
                if (path.startsWith("/api/auth/")) {
                    auth.handle(exchange);
                } else if (collection || item) {
                    if (auth.username(exchange) == null) {
                        json(exchange, 401, Map.of("error", "Please sign in to access payroll records."));
                        return;
                    }
                    if (method.equals("GET") && collection) {
                        var records = repository.findAll();
                        json(exchange, 200, Map.of("summary", Payroll.summarize(records), "records", records));
                    } else if ((collection && method.equals("POST")) || (item && Set.of("PUT", "DELETE").contains(method))) {
                        if (!auth.validMutation(exchange)) {
                            json(exchange, 403, Map.of("error", "Security check failed. Refresh the page and try again."));
                            return;
                        }
                        long id = item ? Long.parseLong(path.substring(path.lastIndexOf('/') + 1)) : 0;
                        if (method.equals("DELETE")) {
                            boolean deleted = repository.delete(id);
                            json(exchange, deleted ? 200 : 404, deleted ? Map.of("deleted", id) : Map.of("error", "Record not found"));
                        } else {
                            String type = exchange.getRequestHeaders().getFirst("Content-Type");
                            if (type == null || !type.split(";")[0].strip().equalsIgnoreCase("application/x-www-form-urlencoded")) {
                                json(exchange, 415, Map.of("error", "Send form-encoded payroll fields")); return;
                            }
                            byte[] bytes = exchange.getRequestBody().readNBytes(16385);
                            if (bytes.length > 16384) { json(exchange, 413, Map.of("error", "Request too large")); return; }
                            Map<String, String> fields = new HashMap<>();
                            for (String pair : new String(bytes, StandardCharsets.UTF_8).split("&")) {
                                String[] parts = pair.split("=", 2);
                                String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                                if (fields.put(key, parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "") != null)
                                    throw new IllegalArgumentException("Duplicate field: " + key);
                            }
                            String name = fields.get("employee_name");
                            if (name != null && name.length() > 200) throw new IllegalArgumentException("Employee name must be at most 200 characters");
                            var record = Payroll.calculate(name, number(fields, "monthly_salary"), number(fields, "tax_rate"), number(fields, "bonus"));
                            if (collection) json(exchange, 201, Map.of("id", repository.save(record)));
                            else {
                                boolean updated = repository.update(id, record);
                                json(exchange, updated ? 200 : 404, updated ? Map.of("id", id) : Map.of("error", "Record not found"));
                            }
                        }
                    } else {
                        exchange.getResponseHeaders().set("Allow", collection ? "GET, POST" : "PUT, DELETE");
                        json(exchange, 405, Map.of("error", "Method not allowed"));
                    }
                } else if (assets.containsKey(path)) {
                    if (method.equals("GET")) {
                        String type = path.endsWith(".css") ? "text/css; charset=utf-8" : "text/javascript; charset=utf-8";
                        send(exchange, 200, type, assets.get(path));
                    } else {
                        exchange.getResponseHeaders().set("Allow", "GET");
                        json(exchange, 405, Map.of("error", "Method not allowed"));
                    }
                } else if (path.equals("/") || path.equals("/dashboard.html")) {
                    if (method.equals("GET")) send(exchange, 200, "text/html; charset=utf-8", html);
                    else { exchange.getResponseHeaders().set("Allow", "GET"); json(exchange, 405, Map.of("error", "Method not allowed")); }
                } else json(exchange, 404, Map.of("error", "Not found"));
            } catch (IllegalArgumentException exception) {
                json(exchange, 400, Map.of("error", exception.getMessage() == null ? "Invalid payroll fields" : exception.getMessage()));
            } catch (Exception exception) {
                // Never log request bodies, cookies, hashes, SQL values, or raw exception messages.
                String incident = UUID.randomUUID().toString();
                System.err.println("Payroll request failed; incident=" + incident + "; type=" + exception.getClass().getSimpleName());
                json(exchange, 500, Map.of("error", "Unable to complete request. Please retry."));
            } finally { exchange.close(); }
        });
        server.start();
        return server;
    }
    private static double number(Map<String, String> fields, String key) {
        try { return Double.parseDouble(fields.getOrDefault(key, "")); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("Invalid number: " + key); }
    }
    static void json(HttpExchange exchange, int status, Object value) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", Json.stringify(value).getBytes(StandardCharsets.UTF_8));
    }
    private static void send(HttpExchange exchange, int status, String type, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        RequestSecurity.addHeaders(exchange);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }
}
