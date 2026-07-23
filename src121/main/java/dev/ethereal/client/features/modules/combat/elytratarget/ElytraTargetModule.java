package dev.ethereal.client.features.modules.combat.elytratarget;

import lombok.Getter;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.other.RotationUpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;

@ModuleRegister(name = "Elytra Target", category = Category.COMBAT)
public class ElytraTargetModule extends Module {
    @Getter private static final ElytraTargetModule instance = new ElytraTargetModule();
    public final ElytraRotationProcessor elytraRotationProcessor = new ElytraRotationProcessor(this);

    public ElytraTargetModule() {
        addSettings(elytraRotationProcessor.getSettings());
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent event) {
        elytraRotationProcessor.processRotation();
    }
}
