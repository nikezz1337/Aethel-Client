package dev.ethereal.client.ui.theme;

import lombok.Getter;
import lombok.Setter;
import dev.ethereal.client.ui.clickgui.module.settings.ColorComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Theme {
    private final String name;
    private final List<ElementColor> elementColors = new ArrayList<>();

    public Theme(String name) {
        this.name = name;

        elementColors.add(new ElementColor("Primary", new Color(108, 101, 204)));
        elementColors.add(new ElementColor("Secondary", new Color(177, 149, 214)));
    }

    public Color getPrimaryColor() { return getElementColor("Primary"); }
    public Color getSecondaryColor() { return getElementColor("Secondary"); }

    public Color getBlurColor() {
        Color p = getPrimaryColor();
        return new Color(
            Math.max(0, p.getRed() / 8),
            Math.max(0, p.getGreen() / 8),
            Math.max(0, p.getBlue() / 8),
            255
        );
    }
    
    public Color getWidgetBlurColor() {
        Color p = getPrimaryColor();
        return new Color(
            Math.max(0, p.getRed() / 6),
            Math.max(0, p.getGreen() / 6),
            Math.max(0, p.getBlue() / 6),
            255
        );
    }
    
    public Color getBackgroundBlurColor() {
        Color p = getPrimaryColor();
        return new Color(
            Math.max(0, p.getRed() / 4),
            Math.max(0, p.getGreen() / 4),
            Math.max(0, p.getBlue() / 4),
            255
        );
    }
    
    public Color getTextColor() { return new Color(255, 255, 255, 255); }
    
    public Color getInactiveTextColor() {
        Color s = getSecondaryColor();
        int avg = (s.getRed() + s.getGreen() + s.getBlue()) / 3;
        return new Color(
            Math.min(255, avg + 50),
            Math.min(255, avg + 50),
            Math.min(255, avg + 50)
        );
    }

    public Color getElementColor(String elementName) {
        for (ElementColor element : elementColors) {
            if (element.getName().equalsIgnoreCase(elementName)) {
                return element.getColor();
            }
        }
        return new Color(-1);
    }

    @Getter
    public static class ElementColor {
        private final String name;
        @Setter private Color color;
        private final ColorComponent colorComponent;

        public ElementColor(String name, Color color) {
            this.name = name;
            this.color = color;
            this.colorComponent = new ColorComponent(this);
        }
    }
}
