package dev.ethereal.client.features.modules.movement;

import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.KeyEvent;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.player.move.TravelEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BindSetting;
import dev.ethereal.api.utils.other.StopWatch;
import dev.ethereal.api.utils.player.MoveUtil;
import dev.ethereal.client.features.modules.combat.AuraModule;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@ModuleRegister(
        name = "TPBack",
        category = Category.MOVEMENT,
        bind = -1
)
@Environment(EnvType.CLIENT)
public class TPBack extends Module {

    private final BindSetting bind = new BindSetting("Бинд");

    @Getter
    private static final TPBack instance = new TPBack();

    public TPBack() {
        addSettings(bind);
    }

    boolean tp;
    private boolean isBoosting = false;
    private final StopWatch boostTimer = new StopWatch();

    @Override
    public void onEvent() {
        EventListener travelEvent = TravelEvent.getInstance().subscribe(new Listener<>(event -> {
            move();
        }));

        EventListener keyEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            input(event);
        }));

        addEvents(travelEvent, keyEvent);
    }

    private void input(KeyEvent.KeyEventData event) {
        if (mc.currentScreen == null && event.action() == 1) { // 1 = нажатие, 0 = отжатие
            if (event.key() == bind.getValue()) {
                if (!isBoosting) {
                    tp = !tp;
                    isBoosting = true;
                    boostTimer.reset();

                    if (!tp) {
                        mc.player.setVelocity(mc.player.getVelocity().x, 0.42, mc.player.getVelocity().z);
                    }
                }
            }
        }
    }

    public void move() {
        if (isBoosting) {
            if (boostTimer.isReached(tp ? 1500 : 750)) {
                isBoosting = false;
                return;
            }

            double speed = tp ? 1.1 : 1.2;
            mc.player.setVelocity(mc.player.getVelocity().x * speed, mc.player.getVelocity().y, mc.player.getVelocity().z * speed);
        }
    }
}
