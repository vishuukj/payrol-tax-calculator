package payroll;

import com.sun.net.httpserver.HttpExchange;
import java.net.URI;
import java.util.List;
import java.util.Locale;

/** Explicit host and origin boundaries for local and hosted operation. */
final class RequestSecurity {
    private RequestSecurity() {}

    static boolean allowedHost(HttpExchange exchange) {
        List<String> hosts = exchange.getRequestHeaders().get("Host");
        if (hosts == null || hosts.size() != 1) return false;
        String host = hosts.get(0).toLowerCase(Locale.ROOT);
        String renderHost = System.getenv("RENDER_EXTERNAL_HOSTNAME");
        if (renderHost != null && host.equals(renderHost.toLowerCase(Locale.ROOT))) return true;
        int port = exchange.getLocalAddress().getPort();
        return host.equals("127.0.0.1:" + port) || host.equals("localhost:" + port);
    }

    static boolean allowedOrigin(HttpExchange exchange) {
        if (!allowedHost(exchange)) return false;
        String site = exchange.getRequestHeaders().getFirst("Sec-Fetch-Site");
        if ("cross-site".equals(site)) return false;
        List<String> origins = exchange.getRequestHeaders().get("Origin");
        // Non-browser clients may omit Origin, but must still supply the CSRF token.
        if (origins == null) return true;
        if (origins.size() != 1) return false;
        try {
            URI origin = URI.create(origins.get(0));
            String publicOrigin = System.getenv("PUBLIC_ORIGIN");
            if (publicOrigin != null && publicOrigin.startsWith("https://")
                    && origins.get(0).equals(publicOrigin)) return true;
            return origin.getScheme().equals("http") && origin.getUserInfo() == null
                    && origin.getQuery() == null && origin.getFragment() == null
                    && (origin.getPath() == null || origin.getPath().isEmpty())
                    && origins.get(0).equals("http://" + exchange.getRequestHeaders().getFirst("Host"));
        } catch (IllegalArgumentException | NullPointerException exception) { return false; }
    }

    static String secureCookie() {
        return System.getenv("RENDER_EXTERNAL_HOSTNAME") != null ? "; Secure" : "";
    }

    static void addHeaders(HttpExchange exchange) {
        var headers = exchange.getResponseHeaders();
        headers.set("Cache-Control", "no-store");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        headers.set("Content-Security-Policy", "default-src 'self'; script-src 'self'; "
                + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; object-src 'none'; "
                + "base-uri 'none'; frame-ancestors 'none'; form-action 'self'; connect-src 'self'");
    }
}
