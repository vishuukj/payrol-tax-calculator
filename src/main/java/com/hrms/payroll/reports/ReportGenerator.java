package com.hrms.payroll.reports;

import com.hrms.payroll.model.Employee;
import com.hrms.payroll.model.Payroll;
import com.hrms.payroll.utils.CurrencyFormatter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds tabular report datasets (Employee / Payroll / Tax / Department) and
 * writes them out as PDF, Excel, or CSV depending on what the user picks in
 * the Reports screen.
 */
public class ReportGenerator {

    public enum ExportFormat { PDF, EXCEL, CSV }

    private final ExcelExporter excelExporter = new ExcelExporter();
    private final CsvExporter csvExporter = new CsvExporter();

    // ---------- Report dataset builders ----------

    public List<String> employeeReportHeaders() {
        return List.of("Employee ID", "Name", "Department", "Designation", "Joining Date", "Basic Salary", "Email", "Phone");
    }

    public List<List<Object>> employeeReportRows(List<Employee> employees) {
        List<List<Object>> rows = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        for (Employee e : employees) {
            rows.add(List.of(e.getEmployeeCode(), e.getFullName(), e.getDepartmentName(), e.getDesignation(),
                    e.getJoiningDate().format(fmt), e.getBasicSalary(), e.getEmail(), e.getPhone()));
        }
        return rows;
    }

    public List<String> payrollReportHeaders() {
        return List.of("Employee ID", "Name", "Pay Period", "Gross Salary", "PF Deduction", "Income Tax", "Other Deductions", "Net Salary");
    }

    public List<List<Object>> payrollReportRows(List<Payroll> payrolls) {
        List<List<Object>> rows = new ArrayList<>();
        for (Payroll p : payrolls) {
            rows.add(List.of(p.getEmployeeCode(), p.getEmployeeName(), p.getPayPeriod(), p.getGrossSalary(),
                    p.getPfDeduction(), p.getIncomeTax(), p.getOtherDeductions(), p.getNetSalary()));
        }
        return rows;
    }

    public List<String> taxReportHeaders() {
        return List.of("Employee ID", "Name", "Pay Period", "Taxable Income (Monthly Gross - PF)", "Income Tax Paid");
    }

    public List<List<Object>> taxReportRows(List<Payroll> payrolls) {
        List<List<Object>> rows = new ArrayList<>();
        for (Payroll p : payrolls) {
            BigDecimal taxable = p.getGrossSalary().subtract(p.getPfDeduction());
            rows.add(List.of(p.getEmployeeCode(), p.getEmployeeName(), p.getPayPeriod(), taxable, p.getIncomeTax()));
        }
        return rows;
    }

    public List<String> departmentReportHeaders() {
        return List.of("Department", "Total Gross Payroll");
    }

    public List<List<Object>> departmentReportRows(Map<String, BigDecimal> departmentPayroll) {
        List<List<Object>> rows = new ArrayList<>();
        departmentPayroll.forEach((dept, total) -> rows.add(List.of(dept, total)));
        return rows;
    }

    // ---------- Export dispatch ----------

    public void export(String title, List<String> headers, List<List<Object>> rows,
                        ExportFormat format, String outputPath) throws IOException {
        switch (format) {
            case EXCEL -> excelExporter.export(title, headers, rows, outputPath);
            case CSV -> csvExporter.export(headers, rows, outputPath);
            case PDF -> exportPdf(title, headers, rows, outputPath);
        }
    }

    private void exportPdf(String title, List<String> headers, List<List<Object>> rows, String outputPath) throws IOException {
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 40, 30);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(45, 55, 145));
            Paragraph titlePara = new Paragraph(title, titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(16);
            document.add(titlePara);

            PdfPTable table = new PdfPTable(headers.size());
            table.setWidthPercentage(100);

            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(63, 81, 181));
                cell.setPadding(6);
                table.addCell(cell);
            }

            Font cellFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
            boolean alt = false;
            for (List<Object> row : rows) {
                Color bg = alt ? new Color(240, 242, 250) : Color.WHITE;
                for (Object value : row) {
                    String text = (value instanceof BigDecimal bd) ? CurrencyFormatter.format(bd) : String.valueOf(value);
                    PdfPCell cell = new PdfPCell(new Phrase(text, cellFont));
                    cell.setBackgroundColor(bg);
                    cell.setPadding(5);
                    table.addCell(cell);
                }
                alt = !alt;
            }

            document.add(table);
        } catch (DocumentException e) {
            throw new IOException("Failed to generate PDF report: " + e.getMessage(), e);
        } finally {
            document.close();
        }
    }
}
