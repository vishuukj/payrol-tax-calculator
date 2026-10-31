package payroll;

import java.nio.file.Path;
import java.util.*;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try { run(args); }
        catch (Exception exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }

    static void run(String[] args) throws Exception {
        Map<String, String> options = parse(args);
        if (args.length == 0 || options.containsKey("--help")) { help(); return; }
        double rate = Double.parseDouble(options.getOrDefault("--tax-rate", "0.2"));
        if (options.containsKey("--tax-only")) {
            var result = Payroll.calculate("Tax calculation", Double.parseDouble(options.get("--tax-only")), rate, 0);
            System.out.printf(Locale.ROOT, "Tax breakdown:%nGross Amount: %.2f%nTax Rate: %.2f%nTax Amount: %.2f%nNet Amount: %.2f%n",
                    result.gross_pay(), rate, result.tax_amount(), result.net_pay());
            return;
        }
        if (!options.containsKey("--list") && !options.containsKey("--summary") && !options.containsKey("--dashboard")
                && (!options.containsKey("--employee") || !options.containsKey("--salary")))
            throw new IllegalArgumentException("--employee and --salary are required; use --help for usage");
        int port = Integer.parseInt(options.getOrDefault("--port", System.getenv().getOrDefault("PORT", "8000")));
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Port must be between 1 and 65535");
        var repository = new PayrollRepository(Path.of(options.getOrDefault("--db", System.getenv().getOrDefault("DATABASE_PATH", "payroll.db"))));
        if (options.containsKey("--dashboard")) {
            var server = DashboardServer.start(repository, port);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
            System.out.println("Dashboard server running at http://127.0.0.1:" + port);
        } else if (options.containsKey("--list")) {
            var records = repository.findAll();
            if (records.isEmpty()) System.out.println("No payroll records found.");
            for (var record : records) System.out.printf(Locale.ROOT, "ID=%s | Employee=%s | Gross=%.2f | Tax=%.2f | Net=%.2f%n",
                    record.get("id"), record.get("employee_name"), record.get("gross_pay"), record.get("tax_amount"), record.get("net_pay"));
        } else if (options.containsKey("--summary")) {
            System.out.println(Json.stringify(Payroll.summarize(repository.findAll())));
        } else {
            var result = Payroll.calculate(options.get("--employee"), Double.parseDouble(options.get("--salary")), rate,
                    Double.parseDouble(options.getOrDefault("--bonus", "0")));
            repository.save(result);
            System.out.printf(Locale.ROOT, "Payroll breakdown:%nEmployee: %s%nMonthly Salary: %.2f%nBonus: %.2f%nGross Pay: %.2f%nTax Rate: %.2f%nTax Amount: %.2f%nNet Pay: %.2f%n",
                    result.employee_name(), result.monthly_salary(), result.bonus(), result.gross_pay(), rate, result.tax_amount(), result.net_pay());
        }
    }

    private static Map<String, String> parse(String[] args) {
        var flags = Set.of("--help", "--list", "--summary", "--dashboard");
        var values = Set.of("--employee", "--salary", "--bonus", "--tax-rate", "--tax-only", "--db", "--port");
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if (options.containsKey(key)) throw new IllegalArgumentException("Duplicate option: " + key);
            if (flags.contains(key)) options.put(key, "true");
            else if (values.contains(key)) {
                if (++i == args.length || args[i].startsWith("--")) throw new IllegalArgumentException("Missing value for " + key);
                options.put(key, args[i]);
            } else throw new IllegalArgumentException("Unknown option: " + key);
        }
        long modes = flags.stream().filter(key -> !key.equals("--help") && options.containsKey(key)).count()
                + (options.containsKey("--tax-only") ? 1 : 0);
        if (modes > 1) throw new IllegalArgumentException("Choose one of --list, --summary, --dashboard or --tax-only");
        return options;
    }

    private static void help() {
        System.out.println("""
            Payroll & Tax Calculator (Java 17+)
            --employee NAME --salary AMOUNT [--bonus AMOUNT] [--tax-rate RATE]
            --list                  List saved payroll records
            --summary               Show payroll analytics
            --tax-only AMOUNT       Calculate tax without saving
            --dashboard [--port N]  Serve dashboard (default port 8000)
            --db PATH               SQLite file (default ./payroll.db)
            --help                  Show this help
            """);
    }
}
