package dev.aethel.module.list.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.MultiBooleanSetting;
import dev.aethel.module.settings.SliderSetting;

@ModuleInformation(
        moduleName = "CustomWorld",
        moduleCategory = ModuleCategory.RENDER,
        moduleDesc = "Настройки мира: время, туман"
)
public class WorldTweaks extends Module {

    public final MultiBooleanSetting worldSettings = new MultiBooleanSetting("World Settings", "",
            new BooleanSetting("Время", false),
            new BooleanSetting("Туман", false));

    public final SliderSetting timeSetting = new SliderSetting("Время", 12.0D, 0.0D, 24.0D, 1.0D);
    public final SliderSetting fogDistance = new SliderSetting("Дистанция тумана", 100.0D, 20.0D, 1200.0D, 10.0D);

    public boolean isTimeEnabled() {
        return isEnabled() && worldSettings.getValue("Время");
    }

    public boolean isFogEnabled() {
        return isEnabled() && worldSettings.getValue("Туман");
    }

    public long getForcedTime() {
        return (long) timeSetting.getValue() * 1000L;
    }

    public float getFogDistance() {
        return (float) fogDistance.getValue();
    }
}
