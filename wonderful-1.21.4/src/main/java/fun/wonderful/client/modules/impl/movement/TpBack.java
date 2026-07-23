package fun.wonderful.client.modules.impl.movement;


import fun.wonderful.api.events.EventLink;
import fun.wonderful.api.events.implement.EventUpdate;
import fun.wonderful.client.modules.Module;
import fun.wonderful.client.modules.settings.implement.FloatSetting;

public class TpBack extends Module {

    public static TpBack INSTANCE = new TpBack();

    private boolean isDead = false;
    private boolean waitingForRespawn = false;
    private int tickCounter = 0;


    public FloatSetting delay = new FloatSetting("Задержка", 5, 1, 20, 1);

    public TpBack() {
        super("TpBack", "Возвращает на точки смерти", ModuleCategory.MOVEMENT);
        addSettings(delay);
    }

        
        @EventLink
        public void onEvent(final EventUpdate event) {
            if (mc.player == null || mc.world == null) return;

            boolean playerDead = mc.player.getHealth() <= 0 & mc.player.deathTime > 0;

            if (playerDead && !isDead) {
                isDead = true;
                mc.player.networkHandler.sendChatMessage("/sethome wonderful");
                mc.player.requestRespawn();
                waitingForRespawn = true;
                tickCounter = 0;
            }

            if (waitingForRespawn && !playerDead) {
                tickCounter++;
                if (tickCounter >= delay.get()) {
                    mc.player.networkHandler.sendChatMessage("/home wonderful");
                    waitingForRespawn = false;
                    tickCounter = 0;
                }
            }

            if (!playerDead && !waitingForRespawn) {
                isDead = false;}
    }
}
