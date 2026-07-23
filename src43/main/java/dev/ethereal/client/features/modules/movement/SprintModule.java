package dev.ethereal.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.events.player.move.SprintEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.utils.player.DirectionalInput;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationManager;
import dev.ethereal.api.utils.rotation.manager.RotationPlan;
import dev.ethereal.client.features.modules.combat.AuraModule;

@ModuleRegister(name = "Sprint", category = Category.MOVEMENT)
public class SprintModule extends Module {
    @Getter private static final SprintModule instance = new SprintModule();

    public SprintModule() {
        setEnabled(true);
    }

    @Override
    public void onEvent() {
        EventListener sprintEvent = SprintEvent.getInstance().subscribe(new Listener<>(1, event -> {
            if (!isSprintAllowed(event.getDirectionalInput())) {
                event.setSprint(true);
            }
        }));

        addEvents(sprintEvent);
    }

    public boolean isSprintAllowed(DirectionalInput directionalInput) {
        AuraModule auraModule = AuraModule.getInstance();
        boolean auraCheck = auraModule.target != null && auraModule.isEnabled() && auraModule.combatExecutor.combatManager().clickScheduler().isOneTickBeforeAttack();

        if (auraModule.isEnabled() && auraModule.moveCorrection.getValue() && auraModule.target != null) {
            return !directionalInput.isMoving() && !auraCheck;
        }

        return (!mc.options.sprintKey.isPressed() || mc.player.input.movementForward != 0) && (hasForwardMovement() || auraCheck);
    }

    public boolean isSprintAllowed() {
        return isSprintAllowed(new DirectionalInput(mc.player.input.movementForward, mc.player.input.movementSideways));
    }

    @SuppressWarnings("MagicNumber")
    public boolean hasForwardMovement() {
        RotationManager rotationManager = RotationManager.getInstance();
        RotationPlan plan = rotationManager.getCurrentRotationPlan();
        if (plan == null || plan.provider() instanceof StrafeModule || plan.moveCorrection()) {
            return false;
        }

        Rotation currentRotation = rotationManager.getCurrentRotation() != null ? rotationManager.getCurrentRotation() : new Rotation(mc.player.getYaw(), mc.player.getPitch());

        float deltaYaw = mc.player.getYaw() - currentRotation.getYaw();
        float forward = mc.player.input.movementForward;
        float sideways = mc.player.input.movementSideways;

        boolean hasForwardMovement = forward * MathHelper.cos(deltaYaw * 0.017453292f) + sideways * MathHelper.sin(deltaYaw * 0.017453292f) > 1.0E-5f;

        return !hasForwardMovement;
    }

}
