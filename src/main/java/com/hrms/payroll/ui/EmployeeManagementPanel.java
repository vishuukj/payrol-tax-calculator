package com.hrms.payroll.ui;

import com.hrms.payroll.controller.EmployeeController;
import com.hrms.payroll.model.Employee;
import com.hrms.payroll.model.User;
import com.hrms.payroll.service.AppContext;
import com.hrms.payroll.service.ValidationException;
import com.hrms.payroll.ui.components.PrimaryButton;
import com.hrms.payroll.ui.components.RoundedPanel;
import com.hrms.payroll.utils.CurrencyFormatter;
import com.hrms.payroll.utils.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Employee Management screen: search bar, filter controls, a striped data
 * table, and Add / Edit / Delete / View actions wired through EmployeeController.
 */
public class EmployeeManagementPanel extends JPanel {

    private final EmployeeController controller = new EmployeeController(AppContext.getInstance().employeeService);
    private final User currentUser;

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField searchField = new JTextField();
    private List<Employee> currentRows;

    private static final String[] COLUMNS = {
            "Emp ID", "Name", "Department", "Designation", "Joining Date", "Basic Salary", "Email", "Phone"
    };

    public EmployeeManagementPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout());
        setBackground(UITheme.PAGE_BG);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        add(buildHeader(), BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        styleTable();

        RoundedPanel tableCard = new RoundedPanel(16);
        tableCard.setCardBackground(Color.WHITE);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);

        add(tableCard, BorderLayout.CENTER);

        loadData(controller.listAll());
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 14));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("Employee Management");
        title.setFont(UITheme.H1);
        title.setForeground(UITheme.TEXT_PRIMARY);
        header.add(title, BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        searchField.setFont(UITheme.BODY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        searchField.putClientProperty("JTextField.placeholderText", "Search by ID, name, department, designation, or email...");
        searchField.addActionListener(e -> loadData(controller.search(searchField.getText())));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton filterBtn = new JButton("Filters");
        styleGhostButton(filterBtn);
        filterBtn.addActionListener(e -> showFilterDialog());

        PrimaryButton addBtn = new PrimaryButton("+  Add Employee", UITheme.PRIMARY);
        addBtn.addActionListener(e -> openEmployeeForm(null));

        actions.add(filterBtn);
        actions.add(addBtn);

        toolbar.add(searchField, BorderLayout.CENTER);
        toolbar.add(actions, BorderLayout.EAST);
        header.add(toolbar, BorderLayout.CENTER);

        return header;
    }

    private void styleGhostButton(JButton btn) {
        btn.setFont(UITheme.BODY_BOLD);
        btn.setForeground(UITheme.PRIMARY);
        btn.setBackground(Color.WHITE);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.PRIMARY, 1, true),
                new EmptyBorder(8, 16, 8, 16)));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void styleTable() {
        table.setFont(UITheme.BODY);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setFont(UITheme.BODY_BOLD);
        table.getTableHeader().setBackground(UITheme.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.setSelectionBackground(new Color(224, 229, 250));
        table.setSelectionForeground(UITheme.TEXT_PRIMARY);

        DefaultTableCellRenderer stripedRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                             boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : UITheme.ROW_ALT);
                }
                setBorder(new EmptyBorder(0, 12, 0, 12));
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(stripedRenderer);
        }

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) openEmployeeForm(currentRows.get(row));
                }
            }
        });

        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit");
        JMenuItem deleteItem = new JMenuItem("Delete");
        JMenuItem viewItem = new JMenuItem("View Details");
        contextMenu.add(viewItem);
        contextMenu.add(editItem);
        contextMenu.add(deleteItem);

        editItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) openEmployeeForm(currentRows.get(row));
        });
        viewItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) showDetails(currentRows.get(row));
        });
        deleteItem.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) handleDelete(currentRows.get(row));
        });
        table.setComponentPopupMenu(contextMenu);
    }

    private void loadData(List<Employee> employees) {
        this.currentRows = employees;
        tableModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        for (Employee e : employees) {
            tableModel.addRow(new Object[]{
                    e.getEmployeeCode(), e.getFullName(), e.getDepartmentName(), e.getDesignation(),
                    e.getJoiningDate().format(fmt), CurrencyFormatter.format(e.getBasicSalary()),
                    e.getEmail(), e.getPhone()
            });
        }
    }

    private void openEmployeeForm(Employee existing) {
        EmployeeFormDialog dialog = new EmployeeFormDialog(
                SwingUtilities.getWindowAncestor(this), existing);
        dialog.setVisible(true);
        if (dialog.wasSaved()) {
            try {
                Employee emp = dialog.getResult();
                Integer userId = currentUser.getUserId();
                if (existing == null) {
                    controller.addEmployee(emp, userId);
                    toast("Employee added successfully.");
                } else {
                    controller.updateEmployee(emp, userId, "Updated via Employee Management screen");
                    toast("Employee updated successfully.");
                }
                loadData(controller.listAll());
            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not save employee: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDelete(Employee emp) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Remove " + emp.getFullName() + " from active employees?\nTheir payroll history will be preserved.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            controller.deleteEmployee(emp.getEmployeeId(), currentUser.getUserId());
            loadData(controller.listAll());
            toast("Employee removed.");
        }
    }

    private void showDetails(Employee e) {
        String msg = String.format("""
                Employee ID: %s
                Name: %s
                Gender: %s
                Email: %s
                Phone: %s
                Department: %s
                Designation: %s
                Joining Date: %s
                Basic Salary: %s
                Bonus: %s
                Allowances: %s
                PF %%: %s
                Other Deductions: %s
                """,
                e.getEmployeeCode(), e.getFullName(), e.getGender(), e.getEmail(), e.getPhone(),
                e.getDepartmentName(), e.getDesignation(), e.getJoiningDate(),
                CurrencyFormatter.format(e.getBasicSalary()), CurrencyFormatter.format(e.getBonus()),
                CurrencyFormatter.format(e.getAllowances()), e.getPfPercent(),
                CurrencyFormatter.format(e.getOtherDeductions()));
        JOptionPane.showMessageDialog(this, msg, "Employee Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showFilterDialog() {
        JTextField minSalary = new JTextField();
        JTextField maxSalary = new JTextField();
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Min Salary:"));
        panel.add(minSalary);
        panel.add(new JLabel("Max Salary:"));
        panel.add(maxSalary);

        int result = JOptionPane.showConfirmDialog(this, panel, "Filter by Salary Range",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                var min = new java.math.BigDecimal(minSalary.getText().isBlank() ? "0" : minSalary.getText());
                var max = new java.math.BigDecimal(maxSalary.getText().isBlank() ? "99999999" : maxSalary.getText());
                loadData(controller.filterBySalaryRange(min, max));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers.", "Invalid input", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void toast(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}
