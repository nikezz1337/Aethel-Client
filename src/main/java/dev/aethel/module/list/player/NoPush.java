package dev.aethel.module.list.player;

import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.MultiBooleanSetting;
import dev.aethel.module.settings.BooleanSetting;

@ModuleInformation(
    moduleName = "NoPush",
    moduleCategory = ModuleCategory.PLAYER,
    moduleDesc = "Убирает отталкивание от блоков и игроков"
)
public class NoPush extends Module {

    public final MultiBooleanSetting collisionList = new MultiBooleanSetting("Коллизия", "",
            new BooleanSetting("Блоки", true),
            new BooleanSetting("Вода", false),
            new BooleanSetting("Удочик", true),
            new BooleanSetting("Игроки", true));
}
