package dev.ethereal.client.features.modules.movement.nitrofirework;

import lombok.Getter;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.system.backend.Choice;
import dev.ethereal.client.features.modules.movement.nitrofirework.modes.*;

@ModuleRegister(name = "ElytraBooster", category = Category.MOVEMENT)
public class NitroFireworkModule extends Module {
    @Getter private static final NitroFireworkModule instance = new NitroFireworkModule();

    private final NitroFireworkCustom nitroFireworkCustom = new NitroFireworkCustom(() -> getMode().is("Custom"));
    private final NitroFireworkRW NitroFireworkRW = new NitroFireworkRW(() -> getMode().is("Grim"));

    private final NitroFireworkMode[] modes = new NitroFireworkMode[]{
            nitroFireworkCustom, NitroFireworkRW
    };

    public NitroFireworkMode currentMode = nitroFireworkCustom;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value("Custom").values(
            Choice.getValues(modes)
    ).onAction(() -> {
        currentMode = (NitroFireworkMode) Choice.getChoiceByName(getMode().getValue(), modes);
    });

    public NitroFireworkModule() {
        addSettings(mode);
        getSettings().addAll(nitroFireworkCustom.getSettings());
        getSettings().addAll(NitroFireworkRW.getSettings());
    }

    @Override
    public void onEvent() {

    }
}
