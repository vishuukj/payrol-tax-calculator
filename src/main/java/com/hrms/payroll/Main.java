package com.hrms.payroll;

import com.formdev.flatlaf.FlatLightLaf;
import com.hrms.payroll.service.AppBootstrap;
import com.hrms.payroll.ui.LoginFrame;

import javax.swing.*;

/**
 * Application entry point.
 *
 * Boots the FlatLaf look-and-feel for a modern, flat UI, ensures the default
 * admin account exists, then launches the login screen on the Swing Event
 * Dispatch Thread (as all Swing UI must be built/mutated from the EDT).
 */
public class Main {

    public static void main(String[] args) {
        // A modern flat Look & Feel instead of the default Metal/Aqua theme.
        FlatLightLaf.setup();

        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 12);

        try {
            AppBootstrap.ensureDefaultAdminExists();
        } catch (Exception e) {
            System.err.println("Warning: could not verify/create default admin account at startup: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            try {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Failed to start the application.\n\n" + e.getMessage() +
                                "\n\nMake sure MySQL is running and src/main/resources/db.properties is configured correctly.",
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}
