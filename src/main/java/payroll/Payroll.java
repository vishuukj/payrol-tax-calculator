package payroll;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Payroll {
    private Payroll() {}

    public record Breakdown(String employee_name, double monthly_salary, double bonus,
            double gross_pay, double tax_rate, double tax_amount, double net_pay, double total_pay) {}

    public static Breakdown calculate(String employee, double salary, double rate, double bonus) {
        if (employee == null || employee.isBlank()) throw new IllegalArgumentException("Employee name must not be empty");
        requireAmount(salary, "Salary");
        requireAmount(bonus, "Bonus");
        if (!Double.isFinite(rate) || rate < 0 || rate > 1) throw new IllegalArgumentException("Tax rate must be between 0 and 1");
        double gross = salary + bonus;
        requireAmount(gross, "Gross pay");
        double tax = gross * rate;
        return new Breakdown(employee.strip(), salary, bonus, gross, rate, tax, gross - tax, gross);
    }

    private static void requireAmount(double value, String label) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException(label + " must be finite and non-negative");
    }

    public static Map<String, Object> summarize(List<Map<String, Object>> records) {
        double gross = 0, tax = 0, net = 0, largest = -1;
        Object employee = null;
        for (var record : records) {
            double current = ((Number) record.get("gross_pay")).doubleValue();
            gross += current;
            tax += ((Number) record.get("tax_amount")).doubleValue();
            net += ((Number) record.get("net_pay")).doubleValue();
            if (current > largest) { largest = current; employee = record.get("employee_name"); }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_records", records.size());
        summary.put("total_gross", round(gross));
        summary.put("total_tax", round(tax));
        summary.put("average_net", records.isEmpty() ? 0.0 : round(net / records.size()));
        summary.put("largest_employee", employee);
        return summary;
    }

    private static double round(double value) {
        return java.math.BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_EVEN).doubleValue();
    }
}
