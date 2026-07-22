package com.hrms.payroll.ui;

import com.hrms.payroll.model.Department;
import com.hrms.payroll.model.Employee;
import com.hrms.payroll.service.AppContext;
import com.hrms.payroll.ui.components.PrimaryButton;
import com.hrms.payroll.utils.UITheme;
import com.hrms.payroll.utils.ValidationUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Modal dialog used for both "Add Employee" and "Edit Employee". Performs
 * client-side field validation before allowing Save, in addition to the
 * server-side validation in EmployeeService.
 */
public class EmployeeFormDialog extends JDialog {

    private final JTextField firstName = new JTextField();
    private final JTextField lastName = new JTextField();
    private final JComboBox<Employee.Gender> gender = new JComboBox<>(Employee.Gender.values());
    private final JTextField email = new JTextField();
    private final JTextField phone = new JTextField();
    private final JComboBox<Department> department;
    private final JTextField designation = new JTextField();
    private final JTextField joiningDate = new JTextField(); // yyyy-MM-dd
    private final JTextField basicSalary = new JTextField();
    private final JTextField bonus = new JTextField("0");
    private final JTextField allowances = new JTextField("0");
    private final JTextField pfPercent = new JTextField("12");
    private final JTextField otherDeductions = new JTextField("0");
    private final JLabel errorLabel = new JLabel(" ");

    private final Employee existing;
    private boolean saved = false;
    private Employee result;

    public EmployeeFormDialog(Window owner, Employee existing) {
        super(owner, existing == null ? "Add Employee" : "Edit Employee", ModalityType.APPLICATION_MODAL);
        this.existing = existing;

        List<Department> departments = AppContext.getInstance().departmentDAO.findAll();
        department = new JComboBox<>(departments.toArray(new Department[0]));

        setSize(560, 640);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        add(buildForm(), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        if (existing != null) prefill(existing);
    }

    private JComponent buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(24, 28, 10, 28));
        form.setBackground(Color.WHITE);

        JLabel title = new JLabel(existing == null ? "New Employee" : "Edit Employee Details");
        title.setFont(UITheme.H2);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(title);
        form.add(Box.createVerticalStrut(16));

        form.add(row("First Name", firstName, "Last Name", lastName));
        form.add(row("Gender", gender, "Email", email));
        form.add(row("Phone", phone, "Department", department));
        form.add(row("Designation", designation, "Joining Date (yyyy-mm-dd)", joiningDate));
        form.add(row("Basic Salary", basicSalary, "Bonus", bonus));
        form.add(row("Allowances", allowances, "PF Percent", pfPercent));
        form.add(row("Other Deductions", otherDeductions, null, null));

        errorLabel.setForeground(UITheme.DANGER);
        errorLabel.setFont(UITheme.SMALL);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        form.add(errorLabel);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    private JComponent row(String label1, JComponent field1, String label2, JComponent field2) {
        JPanel row = new JPanel(new GridLayout(1, field2 == null ? 1 : 2, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row.setBorder(new EmptyBorder(6, 0, 6, 0));
        row.add(labeled(label1, field1));
        if (field2 != null) row.add(labeled(label2, field2));
        return row;
    }

    private JComponent labeled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(UITheme.BODY_BOLD);
        l.setForeground(UITheme.TEXT_PRIMARY);
        if (field instanceof JTextField tf) {
            tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.BORDER, 1, true), new EmptyBorder(6, 8, 6, 8)));
        }
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER));

        JButton cancel = new JButton("Cancel");
        cancel.setFont(UITheme.BODY);
        cancel.addActionListener(e -> dispose());

        PrimaryButton save = new PrimaryButton(existing == null ? "Add Employee" : "Save Changes", UITheme.PRIMARY);
        save.addActionListener(e -> handleSave());

        bar.add(cancel);
        bar.add(save);
        return bar;
    }

    private void prefill(Employee e) {
        firstName.setText(e.getFirstName());
        lastName.setText(e.getLastName());
        gender.setSelectedItem(e.getGender());
        email.setText(e.getEmail());
        phone.setText(e.getPhone());
        for (int i = 0; i < department.getItemCount(); i++) {
            if (department.getItemAt(i).getDepartmentId() == e.getDepartmentId()) {
                department.setSelectedIndex(i);
                break;
            }
        }
        designation.setText(e.getDesignation());
        joiningDate.setText(e.getJoiningDate().toString());
        basicSalary.setText(e.getBasicSalary().toPlainString());
        bonus.setText(e.getBonus().toPlainString());
        allowances.setText(e.getAllowances().toPlainString());
        pfPercent.setText(e.getPfPercent().toPlainString());
        otherDeductions.setText(e.getOtherDeductions().toPlainString());
    }

    private void handleSave() {
        errorLabel.setText(" ");
        try {
            Employee emp = (existing != null) ? existing : new Employee();

            if (!ValidationUtils.isNotBlank(firstName.getText())) throw new IllegalArgumentException("First name is required.");
            if (!ValidationUtils.isNotBlank(lastName.getText())) throw new IllegalArgumentException("Last name is required.");
            if (!ValidationUtils.isValidEmail(email.getText())) throw new IllegalArgumentException("Enter a valid email address.");
            if (!ValidationUtils.isValidPhone(phone.getText())) throw new IllegalArgumentException("Enter a valid phone number (7-15 digits).");
            if (!ValidationUtils.isNotBlank(designation.getText())) throw new IllegalArgumentException("Designation is required.");

            LocalDate jDate;
            try {
                jDate = LocalDate.parse(joiningDate.getText().trim());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Joining date must be in yyyy-mm-dd format.");
            }

            BigDecimal basic = parseMoney(basicSalary.getText(), "Basic salary");
            BigDecimal bonusVal = parseMoney(bonus.getText(), "Bonus");
            BigDecimal allowancesVal = parseMoney(allowances.getText(), "Allowances");
            BigDecimal pf = parseMoney(pfPercent.getText(), "PF percent");
            BigDecimal deductions = parseMoney(otherDeductions.getText(), "Other deductions");

            emp.setFirstName(firstName.getText().trim());
            emp.setLastName(lastName.getText().trim());
            emp.setGender((Employee.Gender) gender.getSelectedItem());
            emp.setEmail(email.getText().trim());
            emp.setPhone(phone.getText().trim());
            Department dept = (Department) department.getSelectedItem();
            emp.setDepartmentId(dept.getDepartmentId());
            emp.setDesignation(designation.getText().trim());
            emp.setJoiningDate(jDate);
            emp.setBasicSalary(basic);
            emp.setBonus(bonusVal);
            emp.setAllowances(allowancesVal);
            emp.setPfPercent(pf);
            emp.setOtherDeductions(deductions);

            this.result = emp;
            this.saved = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            errorLabel.setText(ex.getMessage());
        }
    }

    private BigDecimal parseMoney(String text, String fieldName) {
        try {
            BigDecimal value = new BigDecimal(text.trim());
            if (value.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(fieldName + " cannot be negative.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    public boolean wasSaved() { return saved; }
    public Employee getResult() { return result; }
}
