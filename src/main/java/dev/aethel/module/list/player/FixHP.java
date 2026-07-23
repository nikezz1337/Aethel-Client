package dev.aethel.module.list.player;


import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.ModeSetting;

@ModuleInformation(moduleName = "FixHP", moduleCategory = ModuleCategory.PLAYER, moduleDesc = "Фикс хп для серверов с рандомными хп")
public class FixHP extends Module {
    public static ModeSetting mode = new ModeSetting("Режим", "FunTime", "FunTime", "ReallyWorld");
}
