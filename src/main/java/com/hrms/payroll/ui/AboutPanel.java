package com.hrms.payroll.ui;

import com.hrms.payroll.ui.components.RoundedPanel;
import com.hrms.payroll.utils.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Static "About" screen: app identity, version, and tech stack summary. */
public class AboutPanel extends JPanel {

    public AboutPanel() {
        setLayout(new BorderLayout());
        setBackground(UITheme.PAGE_BG);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        JLabel title = new JLabel("About");
        title.setFont(UITheme.H1);
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel(16);
        card.setCardBackground(Color.WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(26, 30, 26, 30));
        card.setMaximumSize(new Dimension(560, 320));

        JLabel appName = new JLabel("Payroll & Tax Calculator with Persistence & Analytics");
        appName.setFont(UITheme.H2);
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel version = new JLabel("Version 1.0.0");
        version.setFont(UITheme.BODY);
        version.setForeground(UITheme.TEXT_SECONDARY);
        version.setAlignmentX(Component.LEFT_ALIGNMENT);
        version.setBorder(new EmptyBorder(4, 0, 18, 0));

        JTextArea description = new JTextArea(
                "A professional HR payroll management system built to demonstrate a full " +
                "Java MVC architecture: Swing + FlatLaf for the UI, MySQL via JDBC for " +
                "persistence, a DAO layer with prepared statements, a service layer that owns " +
                "all payroll and tax business rules, and JFreeChart / OpenPDF / Apache POI for " +
                "analytics and reporting.\n\n" +
                "Tech stack: Java 17, Maven, Swing, FlatLaf, MySQL, JDBC, HikariCP, JFreeChart, " +
                "OpenPDF, Apache POI, jBCrypt.");
        description.setFont(UITheme.BODY);
        description.setForeground(UITheme.TEXT_PRIMARY);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setEditable(false);
        description.setOpaque(false);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(appName);
        card.add(version);
        card.add(description);

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrapper.setOpaque(false);
        wrapper.add(card);
        add(wrapper, BorderLayout.CENTER);
    }
}
