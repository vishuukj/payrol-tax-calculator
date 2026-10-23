package payroll;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class DashboardServer {
    private DashboardServer() {}

    public static HttpServer start(PayrollRepository repository, int port) throws IOException {
        byte[] html;
        try (var stream = DashboardServer.class.getResourceAsStream("/dashboard.html")) {
            if (stream == null) throw new IOException("Dashboard resource missing");
            html = stream.readAllBytes();
        }
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", exchange -> {
            try {
                int status = 200;
                byte[] body;
                String type = "text/plain; charset=utf-8";
                String path = exchange.getRequestURI().getPath();
                if (!exchange.getRequestMethod().equals("GET")) {
                    status = 405;
                    exchange.getResponseHeaders().set("Allow", "GET");
                    body = "Method not allowed".getBytes(StandardCharsets.UTF_8);
                } else if (path.equals("/api/payroll")) {
                    type = "application/json; charset=utf-8";
                    try {
                        var records = repository.findAll();
                        body = Json.stringify(Map.of("summary", Payroll.summarize(records), "records", records)).getBytes(StandardCharsets.UTF_8);
                    } catch (Exception exception) {
                        status = 500;
                        body = "{\"error\":\"Unable to load payroll records\"}".getBytes(StandardCharsets.UTF_8);
                        System.err.println("Dashboard query failed: " + exception.getMessage());
                    }
                } else if (path.equals("/") || path.equals("/dashboard.html")) {
                    type = "text/html; charset=utf-8";
                    body = html;
                } else {
                    status = 404;
                    body = "Not found".getBytes(StandardCharsets.UTF_8);
                }
                exchange.getResponseHeaders().set("Content-Type", type);
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.sendResponseHeaders(status, body.length);
                exchange.getResponseBody().write(body);
            } finally { exchange.close(); }
        });
        server.start();
        return server;
    }
}
