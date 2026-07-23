package dev.ethereal.client.ui.theme.basic;

import java.awt.*;

public class CandyLoveTheme extends ADefaultTheme {
    public CandyLoveTheme() {
        super("Candy Love");
    }

    @Override
    public Color setPrimary() {
        return new Color(255, 100, 255, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(255, 50, 200, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(60, 30, 60, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(90, 45, 90, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(75, 35, 75, 255);
    }

    @Override
    public Color setText() {
        return new Color(255, 230, 255, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(200, 150, 200, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(255, 120, 255, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(220, 180, 220, 255);
    }
}
