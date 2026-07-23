package dev.ethereal.api.utils.rotation.manager;

import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.task.TaskPriority;
import dev.ethereal.api.module.Module;
import lombok.Getter;

public class RotationManager implements QuickImports {
    @Getter private static final RotationManager instance = new RotationManager();

    private Rotation serverRotation = Rotation.DEFAULT;
    private final RotationPlan currentRotationPlan = new RotationPlan();

    public void load() {
        // Dummy load method — kept for compatibility
    }

    public void addRotation(Rotation rotation, RotationStrategy strategy, TaskPriority priority, Module provider) {
        RotationComponent.update(rotation, 180f, 180f, 180f, 180f, 3, 5, true);
    }

    public void addRotation(Object... args) {
        if (args.length > 0 && args[0] instanceof Rotation) {
            RotationComponent.update((Rotation) args[0], 180f, 180f, 180f, 180f, 3, 5, false);
        } else if (args.length > 0 && args[0] instanceof Rotation.VecRotation) {
            RotationComponent.update(((Rotation.VecRotation) args[0]).rotation(), 180f, 180f, 180f, 180f, 3, 5, false);
        }
    }

    public RotationPlan getCurrentRotationPlan() {
        return RotationComponent.getInstance().isRotating() ? currentRotationPlan : null;
    }

    public Rotation getRotation() {
        Rotation active = RotationComponent.getInstance().currentRotation();
        if (active != null) return active;
        if (mc.player == null) return Rotation.DEFAULT;
        return new Rotation(mc.player.getYaw(), mc.player.getPitch());
    }

    public Rotation getPreviousRotation() {
        return getRotation();
    }

    public Rotation getCurrentRotation() {
        return RotationComponent.getInstance().currentRotation();
    }

    public Rotation getServerRotation() {
        return serverRotation;
    }

    public boolean isRotating() {
        return RotationComponent.getInstance().isRotating();
    }

    public void stopRotation() {
        RotationComponent.getInstance().stopRotation();
    }

    public void resetRotation() {
        RotationComponent.getInstance().stopRotation();
    }
}
