package com.hrms.payroll.ui;

import com.hrms.payroll.model.User;
import com.hrms.payroll.service.AppContext;
import com.hrms.payroll.utils.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The main HRMS shell shown after login: a dark sidebar with icon+label nav
 * items on the left, and a CardLayout content area on the right holding
 * each feature screen (Dashboard, Employees, Payroll, Tax, Analytics,
 * Reports, Settings, Profile, About).
 */
public class MainDashboardFrame extends JFrame {

    private final User currentUser;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private String activeNav = "Dashboard";

    private DashboardPanel dashboardPanel;

    public MainDashboardFrame(User user) {
        this.currentUser = user;

        setTitle("Payroll & Tax Calculator — HR Console");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);
        add(buildContentArea(), BorderLayout.CENTER);

        setActiveNav("Dashboard");
    }

    // ------------------------------------------------------------------
    // Sidebar
    // ------------------------------------------------------------------
    private JComponent buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UITheme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(new EmptyBorder(20, 0, 20, 0));

        JLabel brand = new JLabel("  \uD83D\uDCBC  PayrollHR");
        brand.setFont(UITheme.fontBold(18));
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setBorder(new EmptyBorder(0, 14, 24, 0));
        sidebar.add(brand);

        String[][] items = {
                {"Dashboard", "\uD83C\uDFE0"},
                {"Employees", "\uD83D\uDC65"},
                {"Payroll", "\uD83D\uDCB0"},
                {"Tax Calculator", "\uD83E\uDDFE"},
                {"Analytics", "\uD83D\uDCCA"},
                {"Reports", "\uD83D\uDCC4"},
                {"Settings", "\u2699"},
                {"Profile", "\uD83D\uDC64"},
                {"About", "\u2139"},
        };

        for (String[] item : items) {
            JButton btn = createNavButton(item[1], item[0]);
            navButtons.put(item[0], btn);
            sidebar.add(btn);
        }

        sidebar.add(Box.createVerticalGlue());

        JButton logoutBtn = createNavButton("\u23FB", "Logout");
        logoutBtn.setForeground(new Color(255, 140, 140));
        sidebar.add(logoutBtn);

        JLabel userTag = new JLabel("  Signed in as " + currentUser.getFullName());
        userTag.setFont(UITheme.SMALL);
        userTag.setForeground(new Color(160, 165, 190));
        userTag.setAlignmentX(Component.LEFT_ALIGNMENT);
        userTag.setBorder(new EmptyBorder(10, 4, 0, 0));
        sidebar.add(userTag);

        return sidebar;
    }

    private JButton createNavButton(String icon, String label) {
        JButton btn = new JButton("  " + icon + "   " + label);
        btn.setFont(UITheme.fontPlain(14));
        btn.setForeground(UITheme.SIDEBAR_TEXT);
        btn.setBackground(UITheme.SIDEBAR_BG);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(12, 10, 12, 10));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(230, 44));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!label.equals(activeNav)) btn.setBackground(UITheme.SIDEBAR_HOVER);
            }
            @Override public void mouseExited(MouseEvent e) {
                if (!label.equals(activeNav)) btn.setBackground(UITheme.SIDEBAR_BG);
            }
        });

        btn.addActionListener(e -> {
            if ("Logout".equals(label)) {
                handleLogout();
            } else {
                setActiveNav(label);
            }
        });
        return btn;
    }

    private void setActiveNav(String label) {
        if (navButtons.containsKey(activeNav)) {
            navButtons.get(activeNav).setBackground(UITheme.SIDEBAR_BG);
        }
        activeNav = label;
        if (navButtons.containsKey(label)) {
            navButtons.get(label).setBackground(UITheme.PRIMARY);
        }
        cardLayout.show(contentPanel, label);
        if ("Dashboard".equals(label) && dashboardPanel != null) {
            dashboardPanel.refresh();
        }
    }

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            AppContext.getInstance().authService.logout();
            new LoginFrame().setVisible(true);
            dispose();
        }
    }

    // ------------------------------------------------------------------
    // Content area
    // ------------------------------------------------------------------
    private JComponent buildContentArea() {
        contentPanel.setBackground(UITheme.PAGE_BG);

        dashboardPanel = new DashboardPanel(currentUser);
        contentPanel.add(wrapScrollable(dashboardPanel), "Dashboard");
        contentPanel.add(wrapScrollable(new EmployeeManagementPanel(currentUser)), "Employees");
        contentPanel.add(wrapScrollable(new PayrollPanel(currentUser)), "Payroll");
        contentPanel.add(wrapScrollable(new TaxSlabPanel()), "Tax Calculator");
        contentPanel.add(wrapScrollable(new AnalyticsPanel()), "Analytics");
        contentPanel.add(wrapScrollable(new ReportsPanel()), "Reports");
        contentPanel.add(wrapScrollable(new SettingsPanel()), "Settings");
        contentPanel.add(wrapScrollable(new ProfilePanel(currentUser)), "Profile");
        contentPanel.add(wrapScrollable(new AboutPanel()), "About");

        return contentPanel;
    }

    private JComponent wrapScrollable(JComponent inner) {
        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(UITheme.PAGE_BG);
        return scroll;
    }
}
