package com.hrms.payroll.ui;

import com.hrms.payroll.dao.impl.ActivityLogDAO;
import com.hrms.payroll.model.User;
import com.hrms.payroll.service.AnalyticsService;
import com.hrms.payroll.service.AppContext;
import com.hrms.payroll.ui.components.RoundedPanel;
import com.hrms.payroll.ui.components.StatCard;
import com.hrms.payroll.utils.CurrencyFormatter;
import com.hrms.payroll.utils.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Landing page after login: greeting header, six KPI stat cards, and a
 * recent-activity feed pulled from the activity_log table.
 */
public class DashboardPanel extends JPanel {

    private final AnalyticsService analyticsService = AppContext.getInstance().analyticsService;
    private final ActivityLogDAO activityLogDAO = AppContext.getInstance().activityLogDAO;

    private final JPanel cardsRow = new JPanel(new GridLayout(2, 3, 18, 18));
    private final DefaultListModel<String> activityModel = new DefaultListModel<>();

    public DashboardPanel(User user) {
        setLayout(new BorderLayout());
        setBackground(UITheme.PAGE_BG);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        add(buildHeader(user), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);
        cardsRow.setOpaque(false);
        center.add(cardsRow, BorderLayout.NORTH);
        center.add(buildActivityPanel(), BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        refresh();
    }

    private JComponent buildHeader(User user) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 22, 0));

        JLabel title = new JLabel("Welcome back, " + user.getFullName().split(" ")[0] + " \uD83D\uDC4B");
        title.setFont(UITheme.H1);
        title.setForeground(UITheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Here's what's happening across payroll today, "
                + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));
        subtitle.setFont(UITheme.BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel textCol = new JPanel();
        textCol.setOpaque(false);
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.add(title);
        textCol.add(subtitle);

        header.add(textCol, BorderLayout.WEST);
        return header;
    }

    private JComponent buildActivityPanel() {
        RoundedPanel card = new RoundedPanel(16);
        card.setCardBackground(Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("Recent Activity");
        title.setFont(UITheme.H3);
        title.setForeground(UITheme.TEXT_PRIMARY);
        card.add(title, BorderLayout.NORTH);

        JList<String> activityList = new JList<>(activityModel);
        activityList.setFont(UITheme.BODY);
        activityList.setFixedCellHeight(28);
        activityList.setBorder(new EmptyBorder(10, 0, 0, 0));

        JScrollPane scroll = new JScrollPane(activityList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(0, 260));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    public void refresh() {
        AnalyticsService.DashboardSnapshot snap = analyticsService.getDashboardSnapshot();

        cardsRow.removeAll();
        cardsRow.add(new StatCard("\uD83D\uDC65", "Total Employees", String.valueOf(snap.totalEmployees()), UITheme.PRIMARY));
        cardsRow.add(new StatCard("\uD83D\uDCB0", "Monthly Payroll", CurrencyFormatter.format(snap.monthlyPayroll()), UITheme.ACCENT_PURPLE));
        cardsRow.add(new StatCard("\uD83E\uDDFE", "Total Tax Collected", CurrencyFormatter.format(snap.totalTaxCollected()), UITheme.WARNING));
        cardsRow.add(new StatCard("\uD83D\uDCC8", "Average Salary", CurrencyFormatter.format(snap.averageSalary()), UITheme.ACCENT_TEAL));
        cardsRow.add(new StatCard("\u2B06", "Highest Salary", CurrencyFormatter.format(snap.highestSalary()), UITheme.SUCCESS));
        cardsRow.add(new StatCard("\u2B07", "Lowest Salary", CurrencyFormatter.format(snap.lowestSalary()), UITheme.DANGER));
        cardsRow.revalidate();
        cardsRow.repaint();

        activityModel.clear();
        List<String> recent = activityLogDAO.recent(15);
        if (recent.isEmpty()) {
            activityModel.addElement("No activity recorded yet.");
        } else {
            recent.forEach(activityModel::addElement);
        }
    }
}
