package com.hrms.payroll.ui.components;

import com.hrms.payroll.utils.UITheme;

import javax.swing.*;
import java.awt.*;

/**
 * A single dashboard KPI "card": icon glyph, big value, and a caption,
 * on a colored rounded background. Used for the six headline dashboard
 * metrics (Total Employees, Monthly Payroll, etc).
 */
public class StatCard extends RoundedPanel {

    private final JLabel valueLabel;

    public StatCard(String iconGlyph, String title, String value, Color accentColor) {
        super(18);
        setLayout(new BorderLayout());
        setCardBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        setPreferredSize(new Dimension(220, 110));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel icon = new JLabel(iconGlyph);
        icon.setFont(UITheme.fontPlain(26));
        icon.setForeground(accentColor);

        RoundedPanel iconBadge = new RoundedPanel(14);
        iconBadge.setCardBackground(mix(accentColor, Color.WHITE, 0.85f));
        iconBadge.setPreferredSize(new Dimension(46, 46));
        iconBadge.setLayout(new GridBagLayout());
        iconBadge.add(icon);

        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeWrap.setOpaque(false);
        badgeWrap.add(iconBadge);

        top.add(badgeWrap, BorderLayout.WEST);
        add(top, BorderLayout.NORTH);

        valueLabel = new JLabel(value);
        valueLabel.setFont(UITheme.fontBold(24));
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.SMALL);
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(valueLabel);
        textPanel.add(titleLabel);

        add(textPanel, BorderLayout.CENTER);
    }

    public void updateValue(String value) {
        valueLabel.setText(value);
    }

    private static Color mix(Color c1, Color c2, float ratio) {
        int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
        return new Color(r, g, b);
    }
}
