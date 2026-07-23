package dev.aethel.util.entity;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.task.TaskPriority;
import dev.aethel.util.task.scripts.Script;
import dev.aethel.util.world.ServerUtil;
import dev.aethel.module.list.combat.aura.rotation.Rotation;
import dev.aethel.module.list.combat.aura.rotation.URotations;
import dev.aethel.ui.ClickGuiFrame;

import java.util.List;

@UtilityClass
public class PlayerInventoryComponent implements IMinecraft {
    public final List<KeyBinding> moveKeys = List.of(mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey);
    public static final Script script = new Script(), postScript = new Script();
    public boolean canMove = true;

    public void tick() {
        script.update();
    }

    public void postMotion() {
        postScript.update();
    }

    public void input(dev.aethel.event.list.MoveInputEvent e) {
        if (!canMove) {
            e.forward = 0;
            e.strafe = 0;
            e.jump = false;
        }
    }

    public void addTask(Runnable task) {
        if (script.isFinished() && MovingUtil.hasPlayerMovement()) {
            switch (ServerUtil.server) {
                case "Vanilla" -> {
                    script.cleanup().addTickStep(0, () -> {
                        PlayerInventoryComponent.disableMoveKeys();
                        PlayerInventoryComponent.rotateToCamera();
                    }).addTickStep(1, () -> {
                        task.run();
                        enableMoveKeys();
                    });
                    return;
                }
                case "ReallyWorld" -> {
                    if (mc.player.isOnGround()) {
                        script.cleanup().addTickStep(0, PlayerInventoryComponent::disableMoveKeys).addTickStep(2, PlayerInventoryComponent::rotateToCamera).addTickStep(3, task::run)
                                .addTickStep(4, PlayerInventoryComponent::enableMoveKeys);
                        return;
                    }
                }
                case "SpookyTime", "mioclient" -> {
                    script.cleanup().addTickStep(0, () -> {
                        PlayerInventoryComponent.disableMoveKeys();
                        PlayerInventoryComponent.rotateToCamera();
                    }).addTickStep(1, task::run)
                            .addTickStep(2, PlayerInventoryComponent::enableMoveKeys);
                    return;
                }

            }
        }
        script.addTickStep(0, PlayerInventoryComponent::rotateToCamera);
        postScript.cleanup().addTickStep(0, () -> {
            task.run();
            PlayerInventoryUtil.closeScreen(true);
        });
    }

    private void rotateToCamera() {
        Rotation rotation = new Rotation(Rotation.cameraYaw(), Rotation.cameraPitch());
        URotations.update(rotation, 180f, 180f, 100, TaskPriority.HIGH_IMPORTANCE_3.getPriority());
    }

    public void disableMoveKeys() {
        canMove = false;
        unPressMoveKeys();
    }

    public void enableMoveKeys() {
        PlayerInventoryUtil.closeScreen(true);
        canMove = true;
        updateMoveKeys();
    }

    public void unPressMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(false));
    }

    public void updateMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyBinding.getDefaultKey().getCode())));
    }

    public boolean shouldSkipExecution() {
        return mc.currentScreen != null && !PlayerIntersectionUtil.isChat(mc.currentScreen) && !(mc.currentScreen instanceof SignEditScreen) && !(mc.currentScreen instanceof AnvilScreen)
                && !(mc.currentScreen instanceof AbstractCommandBlockScreen) && !(mc.currentScreen instanceof StructureBlockScreen) && !(mc.currentScreen instanceof ClickGuiFrame);
    }
}
