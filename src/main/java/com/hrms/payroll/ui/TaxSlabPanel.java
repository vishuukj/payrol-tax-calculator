package com.hrms.payroll.ui;

import com.hrms.payroll.dao.TaxSlabDAO;
import com.hrms.payroll.model.TaxSlab;
import com.hrms.payroll.service.AppContext;
import com.hrms.payroll.ui.components.PrimaryButton;
import com.hrms.payroll.ui.components.RoundedPanel;
import com.hrms.payroll.utils.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Lets HR admins view and edit the progressive tax slabs used by
 * SlabBasedTaxCalculator. Changes here take effect immediately for every
 * future payroll calculation since the calculator reads slabs live from
 * the database.
 */
public class TaxSlabPanel extends JPanel {

    private final TaxSlabDAO taxSlabDAO = AppContext.getInstance().taxSlabDAO;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Slab Name", "Min Amount", "Max Amount", "Tax %"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return col > 0; }
    };

    public TaxSlabPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.PAGE_BG);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);

        loadSlabs();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("Tax Calculator — Slab Configuration");
        title.setFont(UITheme.H1);
        title.setForeground(UITheme.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        JLabel hint = new JLabel("Leave Max Amount blank for the top, unbounded slab (\"and above\")");
        hint.setFont(UITheme.SMALL);
        hint.setForeground(UITheme.TEXT_SECONDARY);
        header.add(hint, BorderLayout.SOUTH);

        return header;
    }

    private JComponent buildTableCard() {
        RoundedPanel card = new RoundedPanel(16);
        card.setCardBackground(Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTable table = new JTable(model);
        table.setFont(UITheme.BODY);
        table.setRowHeight(36);
        table.getTableHeader().setFont(UITheme.BODY_BOLD);
        table.getTableHeader().setBackground(UITheme.PRIMARY);
        table.getTableHeader().setForeground(Color.WHITE);

        card.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        actions.setOpaque(false);

        JButton addRowBtn = new JButton("+ Add Slab");
        addRowBtn.setFont(UITheme.BODY_BOLD);
        addRowBtn.addActionListener(e -> model.addRow(new Object[]{"New Slab", "0", "", "0"}));

        PrimaryButton saveBtn = new PrimaryButton("Save Tax Slabs", UITheme.PRIMARY);
        saveBtn.addActionListener(e -> saveSlabs());

        actions.add(addRowBtn);
        actions.add(saveBtn);
        card.add(actions, BorderLayout.SOUTH);

        return card;
    }

    private void loadSlabs() {
        model.setRowCount(0);
        for (TaxSlab slab : taxSlabDAO.findAllActiveOrdered()) {
            model.addRow(new Object[]{
                    slab.getSlabName(),
                    slab.getMinAmount().toPlainString(),
                    slab.getMaxAmount() == null ? "" : slab.getMaxAmount().toPlainString(),
                    slab.getTaxPercent().toPlainString()
            });
        }
    }

    private void saveSlabs() {
        try {
            java.util.List<TaxSlab> slabs = new java.util.ArrayList<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                String name = String.valueOf(model.getValueAt(i, 0));
                BigDecimal min = new BigDecimal(String.valueOf(model.getValueAt(i, 1)));
                String maxText = String.valueOf(model.getValueAt(i, 2)).trim();
                BigDecimal max = maxText.isEmpty() ? null : new BigDecimal(maxText);
                BigDecimal pct = new BigDecimal(String.valueOf(model.getValueAt(i, 3)));
                slabs.add(new TaxSlab(name, min, max, pct));
            }
            taxSlabDAO.replaceAll(slabs);
            JOptionPane.showMessageDialog(this, "Tax slabs saved. New payroll calculations will use these brackets.",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
            loadSlabs();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric amounts and percentages.",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save tax slabs: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
