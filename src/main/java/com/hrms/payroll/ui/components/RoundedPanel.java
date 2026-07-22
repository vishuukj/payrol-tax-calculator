package com.hrms.payroll.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * A JPanel that paints itself as a rounded rectangle. Used as the base for
 * dashboard KPI cards, form containers, and table wrappers to give the app
 * a soft, modern "material" look instead of flat Swing rectangles.
 */
public class RoundedPanel extends JPanel {

    private final int cornerRadius;
    private Color backgroundColorOverride;

    public RoundedPanel(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        setOpaque(false);
    }

    public void setCardBackground(Color color) {
        this.backgroundColorOverride = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(backgroundColorOverride != null ? backgroundColorOverride : getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);
        g2.dispose();
        super.paintComponent(g);
    }
}
