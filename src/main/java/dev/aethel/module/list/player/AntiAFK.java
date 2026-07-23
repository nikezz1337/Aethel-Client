package dev.aethel.module.list.player;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.ModeSetting;
import dev.aethel.module.settings.SliderSetting;

@ModuleInformation(
    moduleName = "AntiAFK",
    moduleCategory = ModuleCategory.PLAYER,
    moduleDesc = "Анти-кик за бездействие"
)
public class AntiAFK extends Module {

    private final ModeSetting mode = new ModeSetting("Режим", "Движение", "Движение", "Автобай");
    private final SliderSetting moveDelay = new SliderSetting("Задержка перед движением", 59.0, 1.0, 60.0, 0.5);

    private long lastSpecialMoveTime = 0;
    private boolean performingSpecialMove = false;
    private long specialMoveStartTime = 0;
    private int specialMovePhase = 0;

    private long lastRctTime = 0;
    private long ahScheduledTime = 0;

    @Override
    public void onEnable() {
        super.onEnable();
        lastSpecialMoveTime = System.currentTimeMillis();
        performingSpecialMove = false;
        specialMovePhase = 0;
        lastRctTime = System.currentTimeMillis();
        ahScheduledTime = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetAllKeys();
        ahScheduledTime = 0;
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        long currentTime = System.currentTimeMillis();

        if (mode.is("Движение")) {
            handleMovementMode(currentTime);
        } else if (mode.is("Автобай")) {
            handleAutobuyMode(currentTime);
        }
    }

    private void handleMovementMode(long currentTime) {
        if (performingSpecialMove) {
            long elapsed = currentTime - specialMoveStartTime;

            switch (specialMovePhase) {
                case 0 -> {
                    resetAllKeys();
                    mc.options.forwardKey.setPressed(true);

                    if (elapsed >= 280) {
                        specialMovePhase = 1;
                        specialMoveStartTime = currentTime;
                        resetAllKeys();
                    }
                }
                case 1 -> {
                    resetAllKeys();
                    mc.options.backKey.setPressed(true);

                    if (elapsed >= 350) {
                        specialMovePhase = 2;
                        performingSpecialMove = false;
                        resetAllKeys();
                    }
                }
            }
            return;
        }

        long delayMs = (long) (moveDelay.getFloatValue() * 1000);

        if (currentTime - lastSpecialMoveTime >= delayMs) {
            performingSpecialMove = true;
            specialMoveStartTime = currentTime;
            specialMovePhase = 0;
            lastSpecialMoveTime = currentTime;
            resetAllKeys();
        }
    }

    private void handleAutobuyMode(long currentTime) {
        if (ahScheduledTime > 0 && currentTime >= ahScheduledTime) {
            mc.player.networkHandler.sendChatMessage("/ah");
            ahScheduledTime = 0;
        }

        if (currentTime - lastRctTime >= 59000) {
            mc.player.networkHandler.sendChatMessage(".rct");
            lastRctTime = currentTime;
            ahScheduledTime = currentTime + 5000;
        }
    }

    private void resetAllKeys() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
    }
}
