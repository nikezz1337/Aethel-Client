package fun.wonderful.client.modules.impl.player;


import fun.wonderful.api.events.EventLink;
import fun.wonderful.api.events.implement.EventUpdate;
import fun.wonderful.client.modules.Module;

public class NoClip extends Module {

    public static NoClip INSTANCE = new NoClip();
    public NoClip() {
        super("NoClip", "Позволяте проходить через блоки", ModuleCategory.PLAYER);
    }

    
    @EventLink
    public void onUpdate(final EventUpdate ignored) {
        if (mc.player == null) return;

        if (mc.player.age % 35 == 0) {
            mc.player.networkHandler.sendChatMessage("/gmsp");
        } else if (mc.player.age % 35 == 2){
            mc.player.networkHandler.sendChatMessage("/gms");
        }
    }
}

