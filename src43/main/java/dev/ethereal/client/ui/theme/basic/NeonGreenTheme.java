package dev.ethereal.client.ui.theme.basic;

import java.awt.*;

public class NeonGreenTheme extends ADefaultTheme {
    public NeonGreenTheme() {
        super("Neon Green");
    }

    @Override
    public Color setPrimary() {
        return new Color(50, 255, 100, 255);
    }

    @Override
    public Color setSecondary() {
        return new Color(100, 255, 50, 255);
    }

    @Override
    public Color setBlur() {
        return new Color(10, 25, 15, 255);
    }

    @Override
    public Color setWidgetBlur() {
        return new Color(20, 50, 30, 255);
    }

    @Override
    public Color setBackgroundBlur() {
        return new Color(15, 40, 20, 255);
    }

    @Override
    public Color setText() {
        return new Color(230, 255, 230, 255);
    }

    @Override
    public Color setInactiveText() {
        return new Color(150, 200, 160, 255);
    }

    @Override
    public Color setKnob() {
        return new Color(80, 255, 120, 255);
    }

    @Override
    public Color setInactiveKnob() {
        return new Color(180, 230, 190, 255);
    }
}
