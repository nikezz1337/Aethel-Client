package dev.ethereal.client.features.modules.combat;

import lombok.Getter;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;

@ModuleRegister(name = "No Push", category = Category.PLAYER)
public class NoPushModule extends Module {
    @Getter private static final NoPushModule instance = new NoPushModule();

    private final MultiBooleanSetting pushing = new MultiBooleanSetting("Pushing").value(
            new BooleanSetting("Block").value(true),
            new BooleanSetting("Liquids").value(true),
            new BooleanSetting("Entity").value(true),
            new BooleanSetting("Fishing rod").value(true)
    );

    public NoPushModule() {
        addSettings(pushing);
    }


    public boolean cancelPush(PushingSource data) {
        return isEnabled() && switch (data) {
            case BLOCK -> pushing.isEnabled("Block");
            case LIQUIDS -> pushing.isEnabled("Liquids");
            case ENTITY -> pushing.isEnabled("Entity");
            case FISHING_ROD -> pushing.isEnabled("Fishing rod");
        };
    }

    public enum PushingSource {
        BLOCK, LIQUIDS, ENTITY, FISHING_ROD
    }
}
