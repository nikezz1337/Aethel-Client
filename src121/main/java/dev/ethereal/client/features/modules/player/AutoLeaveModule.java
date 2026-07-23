package dev.ethereal.client.features.modules.player;

import lombok.Getter;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.system.configs.FriendManager;

@ModuleRegister(name = "Auto Leave", category = Category.PLAYER)
public class AutoLeaveModule extends Module {
    @Getter private static final AutoLeaveModule instance = new AutoLeaveModule();

    private final SliderSetting distance = new SliderSetting("Дистанция").value(50f).range(1f, 100f).step(1f);
    private final ModeSetting action = new ModeSetting("Действие").value("Spawn").values("Hub", "Spawn", "Home");

    public AutoLeaveModule() {
        addSettings(distance, action);
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        handleUpdateEvent();
    }

    private void handleUpdateEvent() {
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (mc.player == player) continue;
            if (FriendManager.getInstance().contains(player.getName().getString())) continue;

            if (player.getPos().distanceTo(mc.player.getPos()) <= distance.getValue()) {
                handleLeave();
                toggle();
                break;
            }
        }
    }

    private void handleLeave() {
        switch (action.getValue()) {
            case "Hub" -> mc.player.networkHandler.sendChatCommand("hub");
            case "Spawn" -> mc.player.networkHandler.sendChatCommand("spawn");
            case "Home" -> mc.player.networkHandler.sendChatCommand("home home");
        }
    }
}
