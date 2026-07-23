package dev.ethereal.api.utils.player;

import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.player.script.Script;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.List;

@UtilityClass
public class InventoryFlowManager implements QuickImports {
    public final List<KeyBinding> moveKeys = List.of(
            mc.options.forwardKey,
            mc.options.backKey,
            mc.options.leftKey,
            mc.options.rightKey,
            mc.options.jumpKey
    );
    public final Script script = new Script();
    public final Script postScript = new Script();
    public boolean canMove = true;

    public void tick() {
        script.update();
        postScript.update();
    }

    public void addTask(Runnable task) {
        if (mc.player == null) return;
        if (!script.isFinished()) return;

        boolean moving = mc.player.input.movementForward != 0.0f || mc.player.input.movementSideways != 0.0f;
        if (moving) {
            script.cleanup()
                    .addTickStep(0, InventoryFlowManager::disableMoveKeys)
                    .addTickStep(1, task::run)
                    .addTickStep(2, InventoryFlowManager::enableMoveKeys);
            return;
        }

        script.cleanup().addTickStep(0, task::run);
    }
    public void disableMoveKeys() {
        canMove = false;
        unPressMoveKeys();

        if (mc.player != null) {
            mc.player.input.movementForward  = 0;
            mc.player.input.movementSideways = 0;
            mc.player.setSprinting(false);
        }
    }
    public void enableMoveKeys() {
        InventoryTask.closeScreen(true);
        canMove = true;
        updateMoveKeys();
    }

    public void unPressMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(false));
    }

    public void updateMoveKeys() {
        moveKeys.forEach(keyBinding -> keyBinding.setPressed(
                InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyBinding.getDefaultKey().getCode())));
    }

    public boolean shouldSkipExecution() {
        return mc.currentScreen != null
                && !(mc.currentScreen instanceof ChatScreen)
                && !(mc.currentScreen instanceof SignEditScreen)
                && !(mc.currentScreen instanceof AnvilScreen)
                && !(mc.currentScreen instanceof AbstractCommandBlockScreen)
                && !(mc.currentScreen instanceof StructureBlockScreen);
    }
}
