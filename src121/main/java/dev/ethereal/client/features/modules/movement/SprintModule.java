package dev.ethereal.client.features.modules.movement;

import lombok.Getter;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.event.events.player.move.SprintEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.utils.player.DirectionalInput;
import dev.ethereal.client.features.modules.combat.Aura;

@ModuleRegister(name = "Sprint", category = Category.MOVEMENT)
public class SprintModule extends Module {
    @Getter private static final SprintModule instance = new SprintModule();

    public SprintModule() {
        setEnabled(true);
    }

    @EventHandler(priority = 1)
    public void onSprint(SprintEvent event) {
        if (shouldStopSprintForAuraCrit()) {
            event.setSprint(false);
            return;
        }

        event.setSprint(event.isSprint() || shouldAutoSprint(event.getDirectionalInput()));
    }

    @EventHandler
    public void onTick(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        Aura aura = Aura.getInstance();
        if (shouldStopSprintForAuraCrit()) {
            mc.options.sprintKey.setPressed(false);
            mc.player.setSprinting(false);
        } else if (aura.isEnabled()
                && aura.getTarget() != null
                && aura.sprint.getValue().equals("Легит")
                && shouldAutoSprint(new DirectionalInput(mc.player.input))) {
            mc.options.sprintKey.setPressed(true);
        }
    }

    public boolean isSprintAllowed(DirectionalInput directionalInput) {
        return !shouldStopSprintForAuraCrit()
                && (mc.options.sprintKey.isPressed() || shouldAutoSprint(directionalInput));
    }

    public boolean isSprintAllowed() {
        return isSprintAllowed(new DirectionalInput(mc.player.input.movementForward, mc.player.input.movementSideways));
    }

    private boolean shouldAutoSprint(DirectionalInput directionalInput) {
        return directionalInput.isForwards() && !directionalInput.isBackwards();
    }

    private boolean shouldStopSprintForAuraCrit() {
        if (mc.player == null || mc.world == null) return false;

        Aura aura = Aura.getInstance();
        return aura.isEnabled() && aura.shouldSuppressSprintForCriticalWindow();
    }
}
