package dev.aethel.module.list.render;

import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.ModeSetting;
import dev.aethel.module.settings.SliderSetting;

@ModuleInformation(
    moduleName = "SwingAnimations",
    moduleCategory = ModuleCategory.RENDER,
    moduleDesc = "Кастомные анимации замаха"
)
public class SwingAnimations extends Module {

    public final BooleanSetting hmiEnable = new BooleanSetting("Красивые руки", false);

    public final ModeSetting hmiAnimationType = new ModeSetting("Вид анимации", "Классик", "Классик", "Шарп");

    public final SliderSetting hmiSmoothness = new SliderSetting("Плавность анимации", 1.0, 0.35, 2.5, 0.05);

    public final BooleanSetting swingEnabled = new BooleanSetting("Анимация свинга", true);

    public final ModeSetting swingType = new ModeSetting(
            "Тип свинга",
            "Smooth",
            "Smooth", "Static", "Down", "DropDown", "Poke", "SelfBack",
            "Feast", "ToBack", "Block", "Akrien", "Break", "Pander", "Slant"
    );

    public final SliderSetting swingStrength = new SliderSetting("Сила анимации", 1.0, 0.1, 3.0, 0.01);

    public final BooleanSetting smoothEnabled = new BooleanSetting("Плавная анимация", false);

    public final SliderSetting slowAnimationSpeed = new SliderSetting("Скорость анимации", 12.0, 1.0, 50.0, 1.0);
}
