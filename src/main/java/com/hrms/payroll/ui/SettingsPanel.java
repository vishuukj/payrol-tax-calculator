package com.hrms.payroll.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.hrms.payroll.service.BackupRestoreService;
import com.hrms.payroll.ui.components.PrimaryButton;
import com.hrms.payroll.ui.components.RoundedPanel;
import com.hrms.payroll.utils.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

/**
 * Settings screen: Light/Dark mode toggle (FlatLaf theme swap), database
 * backup/restore, and a quick keyboard-shortcuts reference card.
 */
public class SettingsPanel extends JPanel {

    private final BackupRestoreService backupRestoreService = new BackupRestoreService();

    public SettingsPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.PAGE_BG);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        JLabel title = new JLabel("Settings");
        title.setFont(UITheme.H1);
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(buildThemeCard());
        column.add(Box.createVerticalStrut(18));
        column.add(buildBackupCard());
        column.add(Box.createVerticalStrut(18));
        column.add(buildShortcutsCard());

        add(column, BorderLayout.CENTER);
    }

    private JComponent buildThemeCard() {
        RoundedPanel card = cardShell("Appearance");
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        row.setOpaque(false);

        JButton lightBtn = new JButton("\u2600  Light Mode");
        JButton darkBtn = new JButton("\uD83C\uDF19  Dark Mode");
        for (JButton b : new JButton[]{lightBtn, darkBtn}) {
            b.setFont(UITheme.BODY_BOLD);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        lightBtn.addActionListener(e -> switchTheme(new FlatLightLaf()));
        darkBtn.addActionListener(e -> switchTheme(new FlatDarkLaf()));

        row.add(lightBtn);
        row.add(darkBtn);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private void switchTheme(LookAndFeel laf) {
        try {
            UIManager.setLookAndFeel(laf);
            for (Window w : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(w);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not switch theme: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JComponent buildBackupCard() {
        RoundedPanel card = cardShell("Database Backup & Restore");

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        row.setOpaque(false);

        PrimaryButton backupBtn = new PrimaryButton("Backup Database", UITheme.SUCCESS);
        backupBtn.addActionListener(e -> handleBackup());

        JButton restoreBtn = new JButton("Restore from File...");
        restoreBtn.setFont(UITheme.BODY_BOLD);
        restoreBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        restoreBtn.addActionListener(e -> handleRestore());

        row.add(backupBtn);
        row.add(restoreBtn);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private void handleBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Choose a folder to save the backup");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            File backupFile = backupRestoreService.backup(chooser.getSelectedFile().getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Backup saved to:\n" + backupFile.getAbsolutePath(),
                    "Backup Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Backup failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRestore() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a backup .sql file to restore");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Restoring will overwrite current data in all tables. This cannot be undone. Continue?",
                "Confirm Restore", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            backupRestoreService.restore(chooser.getSelectedFile());
            JOptionPane.showMessageDialog(this, "Database restored successfully. Please restart the application.",
                    "Restore Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Restore failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JComponent buildShortcutsCard() {
        RoundedPanel card = cardShell("Keyboard Shortcuts");
        JTextArea area = new JTextArea(
                "Enter          Submit the focused form / search box\n" +
                "Ctrl + F        Focus the search field (Employees screen)\n" +
                "Ctrl + N        Add new employee (Employees screen)\n" +
                "Double-click    Edit the selected employee row\n" +
                "Right-click     Open row context menu (View / Edit / Delete)");
        area.setEditable(false);
        area.setOpaque(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        card.add(area, BorderLayout.CENTER);
        return card;
    }

    private RoundedPanel cardShell(String title) {
        RoundedPanel card = new RoundedPanel(16);
        card.setCardBackground(Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        card.setMaximumSize(new Dimension(640, 160));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(title);
        label.setFont(UITheme.H3);
        card.add(label, BorderLayout.NORTH);
        return card;
    }
}
