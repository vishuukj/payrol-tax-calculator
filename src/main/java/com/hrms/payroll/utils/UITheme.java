package com.hrms.payroll.utils;

import java.awt.*;

/**
 * Single source of truth for the app's visual identity (blue / white / purple
 * palette). Every custom-painted UI component pulls its colors and fonts
 * from here so re-theming the app means editing one file.
 */
public final class UITheme {

    private UITheme() {}

    // ---- Palette ----
    public static final Color PRIMARY        = new Color(63, 81, 181);   // indigo blue
    public static final Color PRIMARY_DARK   = new Color(45, 55, 145);
    public static final Color PRIMARY_LIGHT  = new Color(121, 134, 203);
    public static final Color ACCENT_PURPLE  = new Color(126, 87, 194);
    public static final Color ACCENT_TEAL    = new Color(0, 150, 136);
    public static final Color SUCCESS        = new Color(46, 160, 90);
    public static final Color WARNING        = new Color(245, 158, 11);
    public static final Color DANGER         = new Color(220, 53, 69);

    public static final Color SIDEBAR_BG     = new Color(30, 34, 62);
    public static final Color SIDEBAR_HOVER  = new Color(48, 53, 92);
    public static final Color SIDEBAR_TEXT   = new Color(220, 222, 235);

    public static final Color CARD_BG        = Color.WHITE;
    public static final Color PAGE_BG        = new Color(244, 246, 251);
    public static final Color BORDER         = new Color(230, 232, 240);
    public static final Color TEXT_PRIMARY   = new Color(33, 37, 55);
    public static final Color TEXT_SECONDARY = new Color(110, 116, 140);
    public static final Color ROW_ALT        = new Color(247, 248, 252);

    // ---- Fonts ----
    public static final String FONT_FAMILY = "Segoe UI";

    public static Font fontBold(int size) { return new Font(FONT_FAMILY, Font.BOLD, size); }
    public static Font fontPlain(int size) { return new Font(FONT_FAMILY, Font.PLAIN, size); }

    public static final Font H1 = fontBold(26);
    public static final Font H2 = fontBold(20);
    public static final Font H3 = fontBold(16);
    public static final Font BODY = fontPlain(13);
    public static final Font BODY_BOLD = fontBold(13);
    public static final Font SMALL = fontPlain(11);
}
