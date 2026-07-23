package dev.ethereal.client.ui.theme.basic;

import java.awt.*;

public class BlueTheme extends ADefaultTheme {
    public BlueTheme() {
        super("Lavender");
    }

    @Override
    public Color setPrimary() {
        return new Color(120, 130, 255, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(180, 100, 255, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(25, 25, 45, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(45, 45, 85, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(35, 35, 70, 255);
    }

    @Override
    public Color setText() {
        return new Color(255, 255, 255, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(160, 160, 200, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(150, 160, 255, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(200, 200, 220, 255);
    }
}
