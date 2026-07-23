package dev.aethel.module.settings.impl;

public class ThemeManager {
    private static ThemeManager instance;

    private final Theme defaultTheme = new Theme("Default", 0xFF4A90E2, 0xFF9013FE);

    private ThemeManager() {
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public Theme getCurrentTheme() {
        return defaultTheme;
    }
}
