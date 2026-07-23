package dev.ethereal.api.module.setting;

import lombok.Getter;
import lombok.Setter;
import dev.ethereal.client.ui.theme.Theme;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ThemeSetting extends Setting<ThemeSetting.ThemePreset> {
    private final List<ThemePreset> presets = new ArrayList<>();
    @Setter private ThemePreset selectedPreset;
    @Setter private Color customPrimary = new Color(108, 101, 204);
    @Setter private Color customSecondary = new Color(177, 149, 214);
    
    public ThemeSetting(String name) {
        super(name);
        initPresets();
    }
    
    @Override
    public Setting<ThemePreset> value(ThemePreset value) {
        this.selectedPreset = value;
        this.value = value;
        return this;
    }
    
    private void initPresets() {
        // Готовые темы с градиентами
        presets.add(new ThemePreset("Purple", new Color(108, 101, 204), new Color(177, 149, 214)));
        presets.add(new ThemePreset("Coral", new Color(255, 107, 107), new Color(255, 179, 71)));
        presets.add(new ThemePreset("Mint", new Color(78, 205, 196), new Color(149, 225, 211)));
        presets.add(new ThemePreset("Pink", new Color(255, 0, 85), new Color(153, 0, 255)));
        presets.add(new ThemePreset("Neon", new Color(0, 217, 255), new Color(123, 47, 255)));
        presets.add(new ThemePreset("Fire", new Color(255, 56, 56), new Color(255, 140, 56)));
        presets.add(new ThemePreset("Ocean", new Color(79, 172, 254), new Color(0, 242, 254)));
        presets.add(new ThemePreset("Custom", customPrimary, customSecondary));
        
        selectedPreset = presets.get(0); // По умолчанию Purple
        value = selectedPreset;
    }
    
    public void applyToTheme(Theme theme) {
        if (selectedPreset.getName().equals("Custom")) {
            theme.getElementColors().get(0).setColor(customPrimary);
            theme.getElementColors().get(1).setColor(customSecondary);
        } else {
            theme.getElementColors().get(0).setColor(selectedPreset.getPrimary());
            theme.getElementColors().get(1).setColor(selectedPreset.getSecondary());
        }
    }
    
    @Getter
    public static class ThemePreset {
        private final String name;
        private final Color primary;
        private final Color secondary;
        
        public ThemePreset(String name, Color primary, Color secondary) {
            this.name = name;
            this.primary = primary;
            this.secondary = secondary;
        }
    }
}
