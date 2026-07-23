package dev.ethereal.client.features.modules.other;

import lombok.Getter;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.utils.other.SoundUtil;

@ModuleRegister(name = "Toggle Sounds", category = Category.OTHER)
public class ToggleSoundsModule extends Module {
    @Getter private static final ToggleSoundsModule instance = new ToggleSoundsModule();

    public final SliderSetting volume = new SliderSetting("Volume").value(60f).range(1f, 100f).step(1f);

    public ToggleSoundsModule() {
        addSettings(volume);
    }

    public static void playToggle(boolean state) {
        if (state && !instance.isEnabled()) return;
        SoundUtil.playWav(state ? "sounds/enabled.wav" : "sounds/disabled.wav");
    }

    @Override
    public void onEvent() {}
}
