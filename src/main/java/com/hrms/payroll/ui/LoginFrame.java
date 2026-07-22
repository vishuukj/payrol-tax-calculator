package com.hrms.payroll.ui;

import com.hrms.payroll.controller.LoginController;
import com.hrms.payroll.model.User;
import com.hrms.payroll.service.AppContext;
import com.hrms.payroll.service.AuthenticationException;
import com.hrms.payroll.ui.components.GradientPanel;
import com.hrms.payroll.ui.components.PrimaryButton;
import com.hrms.payroll.ui.components.RoundedPanel;
import com.hrms.payroll.utils.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;

/**
 * Application entry screen. Left panel is a branded gradient hero, right
 * panel holds the login form (username, password with show/hide, remember
 * me, validation messages).
 */
public class LoginFrame extends JFrame {

    private final LoginController loginController;
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JLabel errorLabel = new JLabel(" ");
    private final JCheckBox rememberMe = new JCheckBox("Remember me");
    private final JCheckBox showPassword = new JCheckBox("Show password");
    private static final Preferences PREFS = Preferences.userNodeForPackage(LoginFrame.class);

    public LoginFrame() {
        this.loginController = new LoginController(AppContext.getInstance().authService);

        setTitle("Payroll & Tax Calculator — Sign In");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 600);
        setMinimumSize(new Dimension(820, 540));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildHeroPanel(), BorderLayout.WEST);
        add(buildFormPanel(), BorderLayout.CENTER);

        getRootPane().setDefaultButton(null);
        prefillRememberedUsername();
    }

    private JComponent buildHeroPanel() {
        GradientPanel hero = new GradientPanel(UITheme.PRIMARY_DARK, UITheme.ACCENT_PURPLE);
        hero.setPreferredSize(new Dimension(380, 0));
        hero.setLayout(new GridBagLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("\uD83D\uDCBC");
        logo.setFont(UITheme.fontPlain(56));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Payroll & Tax");
        title.setFont(UITheme.fontBold(28));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title2 = new JLabel("Calculator");
        title2.setFont(UITheme.fontBold(28));
        title2.setForeground(Color.WHITE);
        title2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("<html><center>Employee management, payroll<br>and analytics for modern HR teams</center></html>");
        subtitle.setFont(UITheme.fontPlain(13));
        subtitle.setForeground(new Color(230, 230, 245));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(16, 0, 0, 0));

        content.add(logo);
        content.add(Box.createVerticalStrut(8));
        content.add(title);
        content.add(title2);
        content.add(subtitle);

        hero.add(content);
        return hero;
    }

    private JComponent buildFormPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);

        RoundedPanel formCard = new RoundedPanel(0);
        formCard.setCardBackground(Color.WHITE);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setPreferredSize(new Dimension(360, 440));

        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(UITheme.H1);
        welcome.setForeground(UITheme.TEXT_PRIMARY);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Sign in to the HR administration console");
        sub.setFont(UITheme.BODY);
        sub.setForeground(UITheme.TEXT_SECONDARY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setBorder(new EmptyBorder(4, 0, 24, 0));

        JLabel userLbl = fieldLabel("Username");
        styleField(usernameField);

        JLabel passLbl = fieldLabel("Password");
        styleField(passwordField);
        passwordField.setEchoChar('\u2022');

        showPassword.setFont(UITheme.SMALL);
        showPassword.setForeground(UITheme.TEXT_SECONDARY);
        showPassword.setOpaque(false);
        showPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        showPassword.addActionListener(e ->
                passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : '\u2022'));

        rememberMe.setFont(UITheme.SMALL);
        rememberMe.setForeground(UITheme.TEXT_SECONDARY);
        rememberMe.setOpaque(false);
        rememberMe.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel optionsRow = new JPanel(new BorderLayout());
        optionsRow.setOpaque(false);
        optionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsRow.setMaximumSize(new Dimension(320, 30));
        optionsRow.add(rememberMe, BorderLayout.WEST);
        optionsRow.add(showPassword, BorderLayout.EAST);

        errorLabel.setFont(UITheme.SMALL);
        errorLabel.setForeground(UITheme.DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setBorder(new EmptyBorder(10, 0, 6, 0));

        PrimaryButton loginBtn = new PrimaryButton("Sign In", UITheme.PRIMARY);
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(320, 42));
        loginBtn.addActionListener(this::handleLogin);

        JLabel hint = new JLabel("Default admin login — username: admin / password: Admin@123");
        hint.setFont(UITheme.SMALL);
        hint.setForeground(UITheme.TEXT_SECONDARY);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setBorder(new EmptyBorder(14, 0, 0, 0));

        formCard.add(welcome);
        formCard.add(sub);
        formCard.add(userLbl);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(usernameField);
        formCard.add(Box.createVerticalStrut(14));
        formCard.add(passLbl);
        formCard.add(Box.createVerticalStrut(4));
        formCard.add(passwordField);
        formCard.add(Box.createVerticalStrut(8));
        formCard.add(optionsRow);
        formCard.add(errorLabel);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(loginBtn);
        formCard.add(hint);

        usernameField.addActionListener(this::handleLogin);
        passwordField.addActionListener(this::handleLogin);

        wrapper.add(formCard);
        return wrapper;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.BODY_BOLD);
        l.setForeground(UITheme.TEXT_PRIMARY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void styleField(JTextField field) {
        field.setFont(UITheme.BODY);
        field.setMaximumSize(new Dimension(320, 38));
        field.setPreferredSize(new Dimension(320, 38));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
    }

    private void handleLogin(ActionEvent e) {
        errorLabel.setText(" ");
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        try {
            User user = loginController.attemptLogin(username, password);
            if (rememberMe.isSelected()) {
                PREFS.put("rememberedUsername", username);
            } else {
                PREFS.remove("rememberedUsername");
            }
            openDashboard(user);
        } catch (AuthenticationException ex) {
            errorLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            errorLabel.setText("Unable to reach the database. " + rootMessage(ex));
        }
    }

    private String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return cur.getMessage() == null ? "" : cur.getMessage();
    }

    private void prefillRememberedUsername() {
        String remembered = PREFS.get("rememberedUsername", "");
        if (!remembered.isBlank()) {
            usernameField.setText(remembered);
            rememberMe.setSelected(true);
        }
    }

    private void openDashboard(User user) {
        MainDashboardFrame dashboard = new MainDashboardFrame(user);
        dashboard.setVisible(true);
        dispose();
    }
}
