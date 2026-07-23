package dev.ethereal.client.ui.theme.basic;

import java.awt.*;

public class CyanTheme extends ADefaultTheme {
    public CyanTheme() {
        super("Cyan Wave");
    }

    @Override
    public Color setPrimary() {
        return new Color(0, 255, 255, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(0, 180, 255, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(10, 25, 35, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(20, 50, 70, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(15, 40, 55, 255);
    }

    @Override
    public Color setText() {
        return new Color(220, 255, 255, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(140, 200, 220, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(50, 255, 255, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(180, 220, 230, 255);
    }
}
