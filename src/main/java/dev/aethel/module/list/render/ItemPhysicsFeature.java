package dev.aethel.module.list.render;

import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;

@ModuleInformation(
        moduleName = "ItemPhysics",
        moduleCategory = ModuleCategory.RENDER,
        moduleDesc = "Дропнутые предметы лежат на земле"
)
public class ItemPhysicsFeature extends Module {

    private static ItemPhysicsFeature instance;

    public ItemPhysicsFeature() {
        instance = this;
    }

    public static boolean isPhysicsEnabled() {
        return instance != null && instance.isEnabled();
    }
}
