package dev.aethel.module.list.render;

import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.MultiBooleanSetting;

@ModuleInformation(
        moduleName = "Removals",
        moduleCategory = ModuleCategory.RENDER,
        moduleDesc = "Убирает выбранные элементы рендера"
)
public class Removals extends Module {

    public final MultiBooleanSetting elements = new MultiBooleanSetting("Элементы", "",
            new BooleanSetting("Огонь", false),
            new BooleanSetting("Плохие эффекты", false),
            new BooleanSetting("Оверлей в блоке", false),
            new BooleanSetting("Частицы", false),
            new BooleanSetting("Погода", false),
            new BooleanSetting("Облака", false),
            new BooleanSetting("Блок-сущности", false),
            new BooleanSetting("Тени", false),
            new BooleanSetting("Анимацию тотема", false)
    );

    public boolean isEnabled(String element) {
        return isEnabled() && elements.getValue(element);
    }

    public boolean isTotemAnimationDisabled() {
        return isEnabled("Анимацию тотема");
    }
}
