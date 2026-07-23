package dev.ethereal.client.features.modules.movement;

import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.player.move.MotionEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import lombok.Getter;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

@ModuleRegister(name = "AirStuck", category = Category.MOVEMENT)
public class AirStuck extends Module {
    @Getter private static final AirStuck instance = new AirStuck();

    private Vec3d fixVelocity = null;
    private Vec3d freezePos = null;
    private boolean wasInAir = false;

    @Override
    public void onEnable() {
        fixVelocity = null;
        freezePos = null;
        wasInAir = false;
    }

    @Override
    public void onDisable() {
        if (mc.player != null && wasInAir && fixVelocity != null) {
            mc.player.setVelocity(fixVelocity);
        }

        fixVelocity = null;
        freezePos = null;
        wasInAir = false;
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (mc.player == null) return;
        if (!mc.player.isOnGround() && event.isSend()) {
            if (event.packet() instanceof PlayerMoveC2SPacket) {
                event.setCancel(true);
            }
        }
    }

    @EventHandler
    public void onMotion(MotionEvent event) {
        if (mc.player == null) return;
        if (!mc.player.isOnGround()) {
            event.setCancel(true);
        }
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        if (!mc.player.isOnGround()) {
            if (!wasInAir) {
                freezePos = mc.player.getPos();
                fixVelocity = mc.player.getVelocity();
                wasInAir = true;
            }

            mc.player.setPosition(freezePos.x, freezePos.y, freezePos.z);
            mc.player.setVelocity(0, 0, 0);
            mc.player.input.movementForward = 0;
            mc.player.input.movementSideways = 0;
        } else {
            wasInAir = false;
            freezePos = null;
            fixVelocity = null;
        }
    }
}
