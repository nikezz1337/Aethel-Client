package dev.ethereal.client.ui.theme.basic;

import java.awt.*;

public class CrimsonTheme extends ADefaultTheme {
    public CrimsonTheme() {
        super("Crimson");
    }

    @Override
    public Color setPrimary() {
        return new Color(255, 60, 60, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(255, 20, 100, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(20, 5, 5, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(60, 15, 15, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(45, 10, 10, 255);
    }

    @Override
    public Color setText() {
        return new Color(255, 220, 220, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(200, 120, 120, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(255, 80, 80, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(220, 180, 180, 255);
    }
}

