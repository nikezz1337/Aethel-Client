package dev.aethel.module.list.combat;

import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.MultiBooleanSetting;
import dev.aethel.module.settings.SliderSetting;

@ModuleInformation(
        moduleName = "Hitbox",
        moduleCategory = ModuleCategory.COMBAT,
        moduleDesc = "Увеличивает хитбоксы целей"
)
public class Hitbox extends Module {

    public final SliderSetting scale =
            new SliderSetting("Размер", 0.3, 0.0, 1.0, 0.05);

    public final MultiBooleanSetting targets =
            new MultiBooleanSetting("Цели", "",
                    new dev.aethel.module.settings.BooleanSetting("Игроков", true),
                    new dev.aethel.module.settings.BooleanSetting("Мобов", false),
                    new dev.aethel.module.settings.BooleanSetting("Животных", false),
                    new dev.aethel.module.settings.BooleanSetting("Невидимых", true),
                    new dev.aethel.module.settings.BooleanSetting("Голых", true)
            );

    public Hitbox() {
    }
}
