package com.hrms.payroll.ui;

import com.hrms.payroll.model.User;
import com.hrms.payroll.ui.components.RoundedPanel;
import com.hrms.payroll.utils.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/** Read-only profile screen showing the current user's account details. */
public class ProfilePanel extends JPanel {

    public ProfilePanel(User user) {
        setLayout(new BorderLayout());
        setBackground(UITheme.PAGE_BG);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        JLabel title = new JLabel("My Profile");
        title.setFont(UITheme.H1);
        title.setForeground(UITheme.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel(16);
        card.setCardBackground(Color.WHITE);
        card.setLayout(new GridLayout(0, 2, 12, 14));
        card.setBorder(new EmptyBorder(24, 28, 24, 28));
        card.setMaximumSize(new Dimension(480, 240));

        card.add(bold("Full Name:"));
        card.add(plain(user.getFullName()));
        card.add(bold("Username:"));
        card.add(plain(user.getUsername()));
        card.add(bold("Role:"));
        card.add(plain(user.getRole().name().replace('_', ' ')));
        card.add(bold("Email:"));
        card.add(plain(user.getEmail() == null ? "—" : user.getEmail()));
        card.add(bold("Last Login:"));
        card.add(plain(user.getLastLogin() == null ? "First login"
                : user.getLastLogin().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))));

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        wrapper.setOpaque(false);
        wrapper.add(card);
        add(wrapper, BorderLayout.CENTER);
    }

    private JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.BODY_BOLD);
        l.setForeground(UITheme.TEXT_SECONDARY);
        return l;
    }

    private JLabel plain(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.BODY);
        l.setForeground(UITheme.TEXT_PRIMARY);
        return l;
    }
}
