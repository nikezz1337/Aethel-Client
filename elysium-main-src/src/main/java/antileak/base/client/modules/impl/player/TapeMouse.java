package antileak.base.client.modules.impl.player;

import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventTickPre;
import antileak.base.api.utils.bot.BotSessionManager;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.settings.implement.BooleanSetting;
import antileak.base.client.modules.settings.implement.FloatSetting;

import java.util.List;

public class TapeMouse extends Module {

    public static TapeMouse INSTANCE = new TapeMouse();

    private static final float MIN_CPS = 0.05f;

    private final FloatSetting cps = new FloatSetting("CPS", 1, MIN_CPS, 2, 0.05f);
    private final BooleanSetting rightClick = new BooleanSetting("Правая кнопка", false);
    private final BooleanSetting onlyBots = new BooleanSetting("Только боты", true);

    private long lastClick;

    public TapeMouse() {
        super("TapeMouse", "Автоматически бьет по таймингу", ModuleCategory.PLAYER);
        addSettings(cps, rightClick, onlyBots);
    }

    @Override
    public void onEnable() {
        lastClick = 0L;
        super.onEnable();
    }

    @EventLink
    public void onTick(final EventTickPre ignored) {
        long delay = getClickDelayMs();
        long now = System.currentTimeMillis();

        if (now - lastClick < delay) {
            return;
        }

        List<BotSessionManager.BotConnection> bots = BotSessionManager.getConnections();
        boolean clicked = false;

        if (!onlyBots.isState()) {
            clicked = clickForLocalPlayer();
        }

        if (!bots.isEmpty()) {
            clicked |= BotSessionManager.pulseBots(rightClick.isState());
        }

        if (clicked) {
            lastClick = now;
        }
    }

    private long getClickDelayMs() {
        float clicksPerSecond = Math.max(MIN_CPS, cps.get());
        return Math.max(1L, Math.round(1000.0f / clicksPerSecond));
    }

    private boolean clickForLocalPlayer() {
        if (mc.player == null || mc.currentScreen != null) return false;

        if (rightClick.isState()) {
            mc.doItemUse();
        } else {
            mc.doAttack();
        }
        return true;
    }
}
