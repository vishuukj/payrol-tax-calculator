package payroll;

import com.sun.net.httpserver.HttpExchange;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** One local workspace account, with persistent password hashes and expiring sessions. */
final class AuthService {
    private static final int ITERATIONS = 210_000;
    private static final int SESSION_SECONDS = 8 * 60 * 60;
    private static final String COOKIE = "payroll_session";
    private final PayrollRepository repository;
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private final boolean localLoginBypass = Boolean.getBoolean("payroll.localLoginBypass");
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private int failures;
    private Instant blockedUntil = Instant.EPOCH;
    private record Session(String username, String csrfToken, Instant expires) {}
    private record Account(String username, byte[] salt, byte[] hash) {}

    AuthService(PayrollRepository repository) throws SQLException {
        this(repository, Clock.systemUTC());
    }

    AuthService(PayrollRepository repository, Clock clock) throws SQLException {
        this.repository = repository;
        this.clock = clock;
        try (var connection = repository.connect(); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS workspace_account (
                    id INTEGER PRIMARY KEY CHECK (id=1), username TEXT NOT NULL,
                    password_salt TEXT NOT NULL, password_hash TEXT NOT NULL
                )
                """);
        }
    }

    String username(HttpExchange exchange) {
        Session session = session(exchange);
        return session == null ? null : session.username();
    }

    private Session session(HttpExchange exchange) {
        sessions.entrySet().removeIf(entry -> !entry.getValue().expires().isAfter(clock.instant()));
        return sessions.get(token(exchange));
    }

    boolean validMutation(HttpExchange exchange) {
        if (!RequestSecurity.allowedOrigin(exchange)) return false;
        Session session = session(exchange);
        List<String> tokens = exchange.getRequestHeaders().get("X-CSRF-Token");
        return session != null && tokens != null && tokens.size() == 1
                && tokens.get(0).matches("[A-Za-z0-9_-]{43}")
                && MessageDigest.isEqual(session.csrfToken().getBytes(StandardCharsets.US_ASCII),
                        tokens.get(0).getBytes(StandardCharsets.US_ASCII));
    }

    synchronized void handle(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if (path.equals("/api/auth/session") && method.equals("GET")) {
            Session session = session(exchange);
            if (session == null) session = issueSession(exchange, null);
            String user = session.username();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("authenticated", user != null);
            response.put("setupRequired", !localLoginBypass && account() == null);
            response.put("localLoginBypass", localLoginBypass);
            response.put("username", user);
            response.put("csrfToken", session.csrfToken());
            DashboardServer.json(exchange, 200, response);
            return;
        }
        if (!Set.of("/api/auth/setup", "/api/auth/login", "/api/auth/logout").contains(path)) {
            DashboardServer.json(exchange, 404, Map.of("error", "Not found")); return;
        }
        if (!method.equals("POST")) {
            exchange.getResponseHeaders().set("Allow", "POST");
            DashboardServer.json(exchange, 405, Map.of("error", "Method not allowed")); return;
        }
        if (!validMutation(exchange)) {
            DashboardServer.json(exchange, 403, Map.of("error", "Security check failed. Refresh the page and try again.")); return;
        }
        if (path.equals("/api/auth/logout")) {
            sessions.remove(token(exchange));
            exchange.getResponseHeaders().add("Set-Cookie", COOKIE + "=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0" + RequestSecurity.secureCookie());
            DashboardServer.json(exchange, 200, Map.of("authenticated", false)); return;
        }
        if (clock.instant().isBefore(blockedUntil)) {
            exchange.getResponseHeaders().set("Retry-After", "30");
            DashboardServer.json(exchange, 429, Map.of("error", "Too many attempts. Please wait 30 seconds and retry.")); return;
        }
        var fields = readForm(exchange);
        String username = fields.getOrDefault("username", "").strip().toLowerCase(Locale.ROOT);
        String password = fields.getOrDefault("password", "");
        if (localLoginBypass) {
            if (username.isBlank() || password.isEmpty())
                throw new IllegalArgumentException("Enter a username and password.");
            Session session = issueSession(exchange, username);
            DashboardServer.json(exchange, 200,
                    Map.of("authenticated", true, "username", username, "csrfToken", session.csrfToken()));
            return;
        }
        Account account = account();
        if (path.equals("/api/auth/setup")) {
            if (account != null) { DashboardServer.json(exchange, 409, Map.of("error", "An account already exists. Please sign in.")); return; }
            if (!username.matches("[a-z0-9_.-]{3,64}")) throw new IllegalArgumentException("Username must have 3–64 letters, numbers, dots, underscores, or hyphens.");
            if (password.length() < 8 || password.length() > 256) throw new IllegalArgumentException("Use a password between 8 and 256 characters.");
            byte[] salt = new byte[16]; random.nextBytes(salt);
            byte[] hash = hash(password, salt);
            try (var connection = repository.connect(); var statement = connection.prepareStatement(
                    "INSERT INTO workspace_account (id, username, password_salt, password_hash) VALUES (1, ?, ?, ?)")) {
                statement.setString(1, username);
                statement.setString(2, Base64.getEncoder().encodeToString(salt));
                statement.setString(3, Base64.getEncoder().encodeToString(hash));
                statement.executeUpdate();
            }
        } else {
            if (account == null) { DashboardServer.json(exchange, 409, Map.of("error", "Create your workspace account first.")); return; }
            if (password.length() > 256 || !MessageDigest.isEqual(account.hash(), hash(password, account.salt())) || !account.username().equals(username)) {
                if (++failures >= 5) { blockedUntil = clock.instant().plusSeconds(30); failures = 0; }
                DashboardServer.json(exchange, 401, Map.of("error", "Incorrect username or password.")); return;
            }
        }
        failures = 0;
        Session session = issueSession(exchange, username);
        DashboardServer.json(exchange, path.endsWith("setup") ? 201 : 200,
                Map.of("authenticated", true, "username", username, "csrfToken", session.csrfToken()));
    }

    private Session issueSession(HttpExchange exchange, String username) {
        sessions.remove(token(exchange));
        // Bound browser sessions, including anonymous setup/login sessions.
        if (sessions.size() >= 100) sessions.entrySet().stream()
                .min(Comparator.comparing(entry -> entry.getValue().expires()))
                .ifPresent(entry -> sessions.remove(entry.getKey()));
        String token = randomToken();
        Session session = new Session(username, randomToken(), clock.instant().plusSeconds(SESSION_SECONDS));
        sessions.put(token, session);
        exchange.getResponseHeaders().add("Set-Cookie", COOKIE + "=" + token
                + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + SESSION_SECONDS + RequestSecurity.secureCookie());
        return session;
    }

    private String randomToken() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Account account() throws SQLException {
        try (var connection = repository.connect(); var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT username, password_salt, password_hash FROM workspace_account WHERE id=1")) {
            if (!result.next()) return null;
            return new Account(result.getString(1), Base64.getDecoder().decode(result.getString(2)), Base64.getDecoder().decode(result.getString(3)));
        }
    }

    private static String token(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        String found = "";
        if (cookies != null) for (String header : cookies) for (String part : header.split(";")) {
            String[] pair = part.strip().split("=", 2);
            if (pair.length == 2 && pair[0].equals(COOKIE)) {
                String value = pair[1];
                value = value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                        ? value.substring(1, value.length() - 1) : value;
                if (!found.isEmpty() || !value.matches("[A-Za-z0-9_-]{43}")) return "";
                found = value;
            }
        }
        return found;
    }

    private static byte[] hash(String password, byte[] salt) throws Exception {
        var spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String type = exchange.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !type.split(";")[0].strip().equalsIgnoreCase("application/x-www-form-urlencoded"))
            throw new IllegalArgumentException("Send form-encoded account fields.");
        byte[] bytes = exchange.getRequestBody().readNBytes(16385);
        if (bytes.length > 16384) throw new IllegalArgumentException("Request too large.");
        Map<String, String> fields = new HashMap<>();
        for (String pair : new String(bytes, StandardCharsets.UTF_8).split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            if (fields.put(key, parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "") != null)
                throw new IllegalArgumentException("Duplicate field: " + key);
        }
        return fields;
    }
}
