package dev.ethereal.client.features.modules.movement.noslow;

import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import lombok.Getter;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.system.backend.Choice;
import dev.ethereal.client.features.modules.movement.noslow.modes.*;

@ModuleRegister(name = "No Slow", category = Category.MOVEMENT)
public class NoSlowModule extends Module {
    @Getter private static final NoSlowModule instance = new NoSlowModule();

    private final NoSlowCancel noSlowCancel = new NoSlowCancel();
    private final NoSlowSlotUpdate noSlowSlotUpdate = new NoSlowSlotUpdate();
    private final NoSlowGrim noSlowGrim = new NoSlowGrim();
    private final NoSlowFunTime noSlowFunTime = new NoSlowFunTime();
    private final NoSlowMatrix noSlowMatrix = new NoSlowMatrix();

    private final NoSlowMode[] modes = new NoSlowMode[]{
            noSlowCancel, noSlowSlotUpdate, noSlowGrim, noSlowFunTime, noSlowMatrix
    };

    private NoSlowMode currentMode;

    @Getter private final ModeSetting mode = new ModeSetting("Режим").value("Cancel").values(
            Choice.getValues(modes)
    ).onAction(() -> {
        currentMode = (NoSlowMode) Choice.getChoiceByName(getMode().getValue(), modes);
    });

    @Getter private final BooleanSetting funtimeIce = new BooleanSetting("Работать на льду").value(true).setVisible(() -> mode.is("FunTime"));
    @Getter private final BooleanSetting funtimeSnow = new BooleanSetting("Работать на снегу").value(true).setVisible(() -> mode.is("FunTime"));
    @Getter private final BooleanSetting funtimeCrossBow = new BooleanSetting("Работать с арбалетом").value(true).setVisible(() -> mode.is("FunTime"));
    @Getter private final ModeSetting grimMode = new ModeSetting("Режим Grim").value("Tick").values("Tick", "Old").setVisible(() -> mode.is("Grim")).onAction(() -> {
        noSlowGrim.bypassType = switch (getGrimMode().getValue()) {
            case "Tick" -> NoSlowGrim.BypassType.TICK;

            default -> NoSlowGrim.BypassType.OLD;
        };
    });

    public NoSlowModule() {
        addSettings(mode, funtimeIce, funtimeSnow, funtimeCrossBow, grimMode);

        currentMode = (NoSlowMode) Choice.getChoiceByName(mode.getValue(), modes);
        if (currentMode == null) {
            currentMode = noSlowCancel;
        }
    }

    public boolean doUseNoSlow() {
        return isEnabled() && mc.player.isUsingItem() && currentMode != null && currentMode.slowingCancel();
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (currentMode != null) {
            currentMode.onUpdate();
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (currentMode != null) {
            currentMode.onTick();
        }
    }
}
