package com.hrms.payroll.ui;

import com.hrms.payroll.service.AnalyticsService;
import com.hrms.payroll.service.AppContext;
import com.hrms.payroll.ui.components.RoundedPanel;
import com.hrms.payroll.utils.UITheme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Analytics dashboard: department-wise payroll (bar), employee distribution
 * by department (pie), salary distribution (pie), and the monthly payroll
 * trend (line) — all backed by live queries via AnalyticsService.
 */
public class AnalyticsPanel extends JPanel {

    private final AnalyticsService analyticsService = AppContext.getInstance().analyticsService;

    public AnalyticsPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.PAGE_BG);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        JLabel title = new JLabel("Analytics & Insights");
        title.setFont(UITheme.H1);
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 20, 20));
        grid.setOpaque(false);
        grid.add(chartCard("Department-wise Payroll", buildDepartmentBarChart()));
        grid.add(chartCard("Employee Distribution", buildEmployeeDistributionPie()));
        grid.add(chartCard("Salary Distribution", buildSalaryDistributionPie()));
        grid.add(chartCard("Monthly Payroll Trend", buildMonthlyTrendLine()));

        add(grid, BorderLayout.CENTER);
    }

    private JComponent chartCard(String title, JFreeChart chart) {
        RoundedPanel card = new RoundedPanel(16);
        card.setCardBackground(Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel label = new JLabel(title);
        label.setFont(UITheme.H3);
        label.setBorder(new EmptyBorder(0, 4, 8, 0));
        card.add(label, BorderLayout.NORTH);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 260));
        chartPanel.setMouseWheelEnabled(true);
        card.add(chartPanel, BorderLayout.CENTER);
        return card;
    }

    private JFreeChart buildDepartmentBarChart() {
        Map<String, BigDecimal> data = analyticsService.departmentWisePayroll();
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        data.forEach((dept, total) -> dataset.addValue(total, "Payroll", dept));

        JFreeChart chart = ChartFactory.createBarChart(
                null, "Department", "Total Payroll", dataset, PlotOrientation.VERTICAL, false, true, false);
        stylePalette(chart, UITheme.PRIMARY);
        return chart;
    }

    private JFreeChart buildEmployeeDistributionPie() {
        Map<String, Integer> data = analyticsService.employeeDistributionByDepartment();
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        data.forEach(dataset::setValue);

        JFreeChart chart = ChartFactory.createPieChart(null, dataset, true, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        return chart;
    }

    private JFreeChart buildSalaryDistributionPie() {
        Map<String, Integer> buckets = analyticsService.salaryDistribution();
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        buckets.forEach(dataset::setValue);

        JFreeChart chart = ChartFactory.createPieChart(null, dataset, true, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        return chart;
    }

    private JFreeChart buildMonthlyTrendLine() {
        Map<String, BigDecimal> trend = analyticsService.monthlyPayrollTrend(12);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        trend.forEach((month, total) -> dataset.addValue(total, "Net Payroll", month));

        JFreeChart chart = ChartFactory.createLineChart(
                null, "Month", "Net Payroll", dataset, PlotOrientation.VERTICAL, false, true, false);
        stylePalette(chart, UITheme.ACCENT_PURPLE);
        return chart;
    }

    private void stylePalette(JFreeChart chart, Color accent) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getPlot().setBackgroundPaint(new Color(250, 250, 253));
        if (chart.getPlot() instanceof org.jfree.chart.plot.CategoryPlot plot) {
            plot.getRenderer().setSeriesPaint(0, accent);
            plot.setRangeGridlinePaint(UITheme.BORDER);
        }
    }
}
