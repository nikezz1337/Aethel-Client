package dev.ethereal.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.player.other.MovementInputEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;

@ModuleRegister(name = "Velocity", category = Category.COMBAT)
public class VelocityModule extends Module {
    @Getter private static final VelocityModule instance = new VelocityModule();

    private final ModeSetting knockback = new ModeSetting("Режим").value("Cancel").values("Cancel", "Legit");

    private boolean isFallDamage = false;

    public VelocityModule() {
        addSettings(knockback);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        handlePacketEvent(event, event);
    }

    @EventHandler
    public void onMovementInput(MovementInputEvent event) {
        handleMoveInputEvent(event);
    }

    private void handleMoveInputEvent(MovementInputEvent event) {
        switch (knockback.getValue()) {
            case "Legit" -> {
                if (mc.player.hurtTime != 9 || !mc.player.isOnGround() || !mc.player.isSprinting() || isFallDamage) {
                    return;
                }

                event.setJump(true);
            }
        }
    }

    private void handlePacketEvent(PacketEvent event, PacketEvent data) {
        if (data.packet() instanceof EntityVelocityUpdateS2CPacket velocityPacket && velocityPacket.getEntityId() == mc.player.getId()) {
            switch (knockback.getValue()) {
                case "Cancel" -> {
                    event.setCancel(true);
                }

                case "Legit" -> {
                    isFallDamage = velocityPacket.getVelocityX() == 0.0
                            && velocityPacket.getVelocityY() == 0.0
                            && velocityPacket.getVelocityZ() < 0;
                }
            }
        }
    }
}
