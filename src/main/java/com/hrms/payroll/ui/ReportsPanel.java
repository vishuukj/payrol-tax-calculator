package com.hrms.payroll.ui;

import com.hrms.payroll.model.Employee;
import com.hrms.payroll.model.Payroll;
import com.hrms.payroll.reports.ReportGenerator;
import com.hrms.payroll.reports.ReportGenerator.ExportFormat;
import com.hrms.payroll.service.AppContext;
import com.hrms.payroll.ui.components.PrimaryButton;
import com.hrms.payroll.ui.components.RoundedPanel;
import com.hrms.payroll.utils.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Reports screen: pick a report type and export format, then save the
 * generated file to disk. Employee/Payroll/Tax/Department reports are all
 * built from live data via ReportGenerator.
 */
public class ReportsPanel extends JPanel {

    private final ReportGenerator reportGenerator = new ReportGenerator();

    private final JComboBox<String> reportType = new JComboBox<>(
            new String[]{"Employee Report", "Payroll Report", "Tax Report", "Department Report"});
    private final JComboBox<ExportFormat> format = new JComboBox<>(ExportFormat.values());

    public ReportsPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.PAGE_BG);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        JLabel title = new JLabel("Reports");
        title.setFont(UITheme.H1);
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        add(buildCard(), BorderLayout.CENTER);
    }

    private JComponent buildCard() {
        RoundedPanel card = new RoundedPanel(16);
        card.setCardBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(24, 28, 24, 28));
        card.setMaximumSize(new Dimension(560, 260));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Generate a report from live payroll data and export it in your preferred format.");
        subtitle.setFont(UITheme.BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(0, 0, 18, 0));
        card.add(subtitle);

        card.add(fieldRow("Report Type", reportType));
        card.add(Box.createVerticalStrut(14));
        card.add(fieldRow("Export Format", format));
        card.add(Box.createVerticalStrut(20));

        PrimaryButton exportBtn = new PrimaryButton("Generate & Export", UITheme.PRIMARY);
        exportBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportBtn.addActionListener(e -> handleExport());
        card.add(exportBtn);

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrapper.setOpaque(false);
        wrapper.add(card);
        return wrapper;
    }

    private JComponent fieldRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(480, 60));
        JLabel l = new JLabel(label);
        l.setFont(UITheme.BODY_BOLD);
        field.setPreferredSize(new Dimension(300, 34));
        row.add(l, BorderLayout.NORTH);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private void handleExport() {
        String type = (String) reportType.getSelectedItem();
        ExportFormat fmt = (ExportFormat) format.getSelectedItem();
        String extension = switch (fmt) {
            case PDF -> ".pdf";
            case EXCEL -> ".xlsx";
            case CSV -> ".csv";
        };

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(type.replace(" ", "_") + extension));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String path = chooser.getSelectedFile().getAbsolutePath();
        if (!path.endsWith(extension)) path += extension;

        try {
            List<String> headers;
            List<List<Object>> rows;

            switch (type) {
                case "Employee Report" -> {
                    List<Employee> employees = AppContext.getInstance().employeeService.findAll();
                    headers = reportGenerator.employeeReportHeaders();
                    rows = reportGenerator.employeeReportRows(employees);
                }
                case "Payroll Report" -> {
                    List<Payroll> payrolls = AppContext.getInstance().payrollDAO.findAll();
                    headers = reportGenerator.payrollReportHeaders();
                    rows = reportGenerator.payrollReportRows(payrolls);
                }
                case "Tax Report" -> {
                    List<Payroll> payrolls = AppContext.getInstance().payrollDAO.findAll();
                    headers = reportGenerator.taxReportHeaders();
                    rows = reportGenerator.taxReportRows(payrolls);
                }
                default -> {
                    var deptPayroll = AppContext.getInstance().analyticsService.departmentWisePayroll();
                    headers = reportGenerator.departmentReportHeaders();
                    rows = reportGenerator.departmentReportRows(deptPayroll);
                }
            }

            reportGenerator.export(type, headers, rows, fmt, path);
            JOptionPane.showMessageDialog(this, "Report exported to:\n" + path, "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export report: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
