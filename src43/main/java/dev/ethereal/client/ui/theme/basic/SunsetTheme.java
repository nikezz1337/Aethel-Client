package dev.ethereal.client.ui.theme.basic;

import java.awt.*;

public class SunsetTheme extends ADefaultTheme {
    public SunsetTheme() {
        super("Sunset");
    }

    @Override
    public Color setPrimary() {
        return new Color(255, 120, 50, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(255, 60, 120, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(30, 15, 20, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(60, 30, 40, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(45, 20, 30, 255);
    }

    @Override
    public Color setText() {
        return new Color(255, 240, 220, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(220, 160, 140, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(255, 140, 80, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(230, 190, 170, 255);
    }
}
