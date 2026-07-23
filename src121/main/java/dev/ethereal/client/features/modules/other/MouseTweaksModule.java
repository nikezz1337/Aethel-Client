package dev.ethereal.client.features.modules.other;

import lombok.Getter;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.SliderSetting;

@ModuleRegister(name = "ItemScroller", category = Category.OTHER)
public class MouseTweaksModule extends Module {
    @Getter private static final MouseTweaksModule instance = new MouseTweaksModule();

    public final SliderSetting delay = new SliderSetting("Задержка").value(50f).range(0f, 500f).step(10f);

    public MouseTweaksModule() {
        addSettings(delay);
    }

}
