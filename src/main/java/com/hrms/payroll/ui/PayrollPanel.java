package com.hrms.payroll.ui;

import com.hrms.payroll.model.Employee;
import com.hrms.payroll.model.Payroll;
import com.hrms.payroll.model.User;
import com.hrms.payroll.reports.SalarySlipGenerator;
import com.hrms.payroll.service.AppContext;
import com.hrms.payroll.service.PayrollService;
import com.hrms.payroll.service.PayrollService.SalaryBreakdown;
import com.hrms.payroll.ui.components.PrimaryButton;
import com.hrms.payroll.ui.components.RoundedPanel;
import com.hrms.payroll.utils.CurrencyFormatter;
import com.hrms.payroll.utils.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Payroll Calculator screen: pick an employee and pay period, see a live
 * salary-slip style breakdown (Gross / PF / Tax / Net), then either save
 * the payroll record to the database or export it as a PDF payslip.
 * Also supports bulk "Run Payroll for All Employees" for the period.
 */
public class PayrollPanel extends JPanel {

    private final PayrollService payrollService = AppContext.getInstance().payrollService;
    private final User currentUser;

    private final JComboBox<Employee> employeeCombo;
    private final JTextField payPeriodField = new JTextField(PayrollService.currentPayPeriod());
    private final DefaultTableModel breakdownModel = new DefaultTableModel(new String[]{"Component", "Amount"}, 0);
    private final JLabel netSalaryLabel = new JLabel("--");

    private SalaryBreakdown currentBreakdown;

    public PayrollPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout());
        setBackground(UITheme.PAGE_BG);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        List<Employee> employees = AppContext.getInstance().employeeService.findAll();
        employeeCombo = new JComboBox<>(employees.toArray(new Employee[0]));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JComponent buildHeader() {
        JLabel title = new JLabel("Payroll Calculator");
        title.setFont(UITheme.H1);
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        return title;
    }

    private JComponent buildBody() {
        JPanel body = new JPanel(new BorderLayout(20, 20));
        body.setOpaque(false);

        body.add(buildControlsCard(), BorderLayout.NORTH);
        body.add(buildSlipCard(), BorderLayout.CENTER);
        return body;
    }

    private JComponent buildControlsCard() {
        RoundedPanel card = new RoundedPanel(16);
        card.setCardBackground(Color.WHITE);
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 14));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        card.add(labeled("Employee", employeeCombo, 260));
        card.add(labeled("Pay Period (YYYY-MM)", payPeriodField, 160));

        PrimaryButton calcBtn = new PrimaryButton("Calculate", UITheme.PRIMARY);
        calcBtn.addActionListener(e -> calculate());

        PrimaryButton saveBtn = new PrimaryButton("Save Payroll", UITheme.SUCCESS);
        saveBtn.addActionListener(e -> savePayroll());

        JButton pdfBtn = new JButton("Export Salary Slip (PDF)");
        pdfBtn.setFont(UITheme.BODY_BOLD);
        pdfBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pdfBtn.addActionListener(e -> exportSlip());

        JButton runAllBtn = new JButton("Run Payroll for All");
        runAllBtn.setFont(UITheme.BODY_BOLD);
        runAllBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        runAllBtn.addActionListener(e -> runForAll());

        card.add(calcBtn);
        card.add(saveBtn);
        card.add(pdfBtn);
        card.add(runAllBtn);

        return card;
    }

    private JComponent labeled(String label, JComponent field, int width) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(UITheme.SMALL);
        l.setForeground(UITheme.TEXT_SECONDARY);
        field.setPreferredSize(new Dimension(width, 34));
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildSlipCard() {
        RoundedPanel card = new RoundedPanel(16);
        card.setCardBackground(Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Salary Slip Preview");
        title.setFont(UITheme.H3);
        card.add(title, BorderLayout.NORTH);

        JTable table = new JTable(breakdownModel) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table.setFont(UITheme.BODY);
        table.setRowHeight(32);
        table.getTableHeader().setFont(UITheme.BODY_BOLD);
        table.getTableHeader().setBackground(UITheme.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(0, 260));
        scroll.setBorder(BorderFactory.createEmptyBorder(14, 0, 14, 0));
        card.add(scroll, BorderLayout.CENTER);

        JPanel netPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        netPanel.setOpaque(false);
        JLabel netCaption = new JLabel("Net Salary Payable:  ");
        netCaption.setFont(UITheme.H3);
        netSalaryLabel.setFont(UITheme.fontBold(20));
        netSalaryLabel.setForeground(UITheme.SUCCESS);
        netPanel.add(netCaption);
        netPanel.add(netSalaryLabel);
        card.add(netPanel, BorderLayout.SOUTH);

        return card;
    }

    private void calculate() {
        Employee emp = (Employee) employeeCombo.getSelectedItem();
        if (emp == null) return;
        try {
            currentBreakdown = payrollService.calculate(emp);
            breakdownModel.setRowCount(0);
            breakdownModel.addRow(new Object[]{"Basic Salary", CurrencyFormatter.format(emp.getBasicSalary())});
            breakdownModel.addRow(new Object[]{"Bonus", CurrencyFormatter.format(emp.getBonus())});
            breakdownModel.addRow(new Object[]{"Allowances", CurrencyFormatter.format(emp.getAllowances())});
            breakdownModel.addRow(new Object[]{"Gross Salary", CurrencyFormatter.format(currentBreakdown.grossSalary())});
            breakdownModel.addRow(new Object[]{"PF Deduction (" + emp.getPfPercent() + "%)", CurrencyFormatter.format(currentBreakdown.pfDeduction())});
            breakdownModel.addRow(new Object[]{"Income Tax", CurrencyFormatter.format(currentBreakdown.incomeTax())});
            breakdownModel.addRow(new Object[]{"Other Deductions", CurrencyFormatter.format(currentBreakdown.otherDeductions())});
            netSalaryLabel.setText(CurrencyFormatter.format(currentBreakdown.netSalary()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not calculate payroll: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void savePayroll() {
        Employee emp = (Employee) employeeCombo.getSelectedItem();
        if (emp == null) return;
        String period = payPeriodField.getText().trim();
        if (!period.matches("\\d{4}-\\d{2}")) {
            JOptionPane.showMessageDialog(this, "Pay period must be in YYYY-MM format.", "Invalid Period", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Payroll saved = payrollService.generateAndSave(emp.getEmployeeId(), period, currentUser.getUserId());
            JOptionPane.showMessageDialog(this,
                    "Payroll saved for " + emp.getFullName() + " — Net Salary: " + CurrencyFormatter.format(saved.getNetSalary()),
                    "Payroll Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save payroll: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportSlip() {
        if (currentBreakdown == null) {
            calculate();
            if (currentBreakdown == null) return;
        }
        JFileChooser chooser = new JFileChooser();
        Employee emp = currentBreakdown.employee();
        chooser.setSelectedFile(new File(emp.getEmployeeCode() + "_" + payPeriodField.getText().trim() + "_payslip.pdf"));
        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) return;

        try {
            new SalarySlipGenerator().generate(currentBreakdown, payPeriodField.getText().trim(),
                    chooser.getSelectedFile().getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Salary slip exported successfully.", "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runForAll() {
        String period = payPeriodField.getText().trim();
        if (!period.matches("\\d{4}-\\d{2}")) {
            JOptionPane.showMessageDialog(this, "Pay period must be in YYYY-MM format.", "Invalid Period", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Run payroll for ALL active employees for " + period + "?",
                "Confirm Bulk Payroll Run", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            List<Payroll> results = payrollService.runPayrollForAll(period, currentUser.getUserId());
            JOptionPane.showMessageDialog(this, "Payroll generated for " + results.size() + " employees.",
                    "Bulk Payroll Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Bulk payroll run failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
