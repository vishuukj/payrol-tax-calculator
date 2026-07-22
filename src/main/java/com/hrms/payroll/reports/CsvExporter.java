package com.hrms.payroll.reports;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/** Simple, dependency-free CSV writer (RFC 4180 quoting) for report exports. */
public class CsvExporter {

    public void export(List<String> headers, List<List<Object>> rows, String outputPath) throws IOException {
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(toCsvLine(headers));
            for (List<Object> row : rows) {
                List<String> asStrings = row.stream().map(v -> v == null ? "" : v.toString()).toList();
                writer.write(toCsvLine(asStrings));
            }
        }
    }

    private String toCsvLine(List<?> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            String v = String.valueOf(values.get(i));
            if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
                v = "\"" + v.replace("\"", "\"\"") + "\"";
            }
            sb.append(v);
            if (i < values.size() - 1) sb.append(",");
        }
        sb.append("\n");
        return sb.toString();
    }
}
