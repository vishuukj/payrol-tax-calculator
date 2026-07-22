package com.hrms.payroll.reports;

import com.hrms.payroll.model.Employee;
import com.hrms.payroll.service.PayrollService.SalaryBreakdown;
import com.hrms.payroll.utils.CurrencyFormatter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Generates a professional, printable PDF salary slip for one employee /
 * one pay period, using OpenPDF (an actively-maintained LGPL fork of iText 4).
 */
public class SalarySlipGenerator {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(45, 55, 145));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY);
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
    private static final Font NET_FONT = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(20, 120, 70));
    private static final Color BRAND_COLOR = new Color(63, 81, 181);

    public void generate(SalaryBreakdown breakdown, String payPeriod, String outputPath) throws IOException {
        Employee emp = breakdown.employee();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            // ---- Company header ----
            Paragraph company = new Paragraph("HRMS Payroll & Tax Calculator", TITLE_FONT);
            company.setAlignment(Element.ALIGN_CENTER);
            document.add(company);

            Paragraph subtitle = new Paragraph("Payslip for " + payPeriod,
                    new Font(Font.HELVETICA, 12, Font.ITALIC, Color.GRAY));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // ---- Employee details table ----
            PdfPTable empTable = new PdfPTable(2);
            empTable.setWidthPercentage(100);
            empTable.setWidths(new float[]{1, 1});

            addKeyValue(empTable, "Employee Name", emp.getFullName());
            addKeyValue(empTable, "Employee ID", emp.getEmployeeCode());
            addKeyValue(empTable, "Department", emp.getDepartmentName());
            addKeyValue(empTable, "Designation", emp.getDesignation());
            addKeyValue(empTable, "Joining Date", emp.getJoiningDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            addKeyValue(empTable, "Pay Period", payPeriod);
            document.add(empTable);

            Paragraph spacer = new Paragraph(" ");
            document.add(spacer);

            // ---- Earnings / Deductions breakdown ----
            PdfPTable breakdownTable = new PdfPTable(2);
            breakdownTable.setWidthPercentage(100);
            breakdownTable.setWidths(new float[]{1, 1});
            breakdownTable.setSpacingBefore(10);

            addSectionHeader(breakdownTable, "Earnings");
            addSectionHeader(breakdownTable, "Deductions");

            addLine(breakdownTable, "Basic Salary", CurrencyFormatter.format(emp.getBasicSalary()));
            addLine(breakdownTable, "PF Deduction", CurrencyFormatter.format(breakdown.pfDeduction()));

            addLine(breakdownTable, "Bonus", CurrencyFormatter.format(emp.getBonus()));
            addLine(breakdownTable, "Income Tax", CurrencyFormatter.format(breakdown.incomeTax()));

            addLine(breakdownTable, "Allowances", CurrencyFormatter.format(emp.getAllowances()));
            addLine(breakdownTable, "Other Deductions", CurrencyFormatter.format(breakdown.otherDeductions()));

            addLine(breakdownTable, "Gross Salary", CurrencyFormatter.format(breakdown.grossSalary()));
            addTotalDeductionsRow(breakdownTable, breakdown);

            document.add(breakdownTable);

            // ---- Net salary highlight ----
            Paragraph netPara = new Paragraph(
                    "\nNet Salary Payable: " + CurrencyFormatter.format(breakdown.netSalary()), NET_FONT);
            netPara.setAlignment(Element.ALIGN_RIGHT);
            netPara.setSpacingBefore(15);
            document.add(netPara);

            Paragraph footer = new Paragraph(
                    "\n\nThis is a system-generated payslip and does not require a signature.",
                    new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

        } catch (DocumentException e) {
            throw new IOException("Failed to generate salary slip PDF: " + e.getMessage(), e);
        } finally {
            document.close();
        }
    }

    private void addTotalDeductionsRow(PdfPTable table, SalaryBreakdown b) {
        addLine(table, "Total Deductions",
                CurrencyFormatter.format(b.pfDeduction().add(b.incomeTax()).add(b.otherDeductions())));
    }

    private void addKeyValue(PdfPTable table, String key, String value) {
        PdfPCell k = new PdfPCell(new Phrase(key, LABEL_FONT));
        k.setBorder(Rectangle.NO_BORDER);
        k.setPadding(4);
        PdfPCell v = new PdfPCell(new Phrase(value, VALUE_FONT));
        v.setBorder(Rectangle.NO_BORDER);
        v.setPadding(4);
        table.addCell(k);
        table.addCell(v);
    }

    private void addSectionHeader(PdfPTable table, String title) {
        PdfPCell cell = new PdfPCell(new Phrase(title, HEADER_FONT));
        cell.setBackgroundColor(BRAND_COLOR);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addLine(PdfPTable table, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label + ":  " + value, VALUE_FONT));
        l.setPadding(5);
        l.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(l);
    }
}
