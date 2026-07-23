package dev.aethel.module.list.movement;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventPacket;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

@ModuleInformation(
        moduleName = "AirStuck",
        moduleCategory = ModuleCategory.MOVEMENT,
        moduleDesc = "Замирает в воздухе на пике высоты"
)
public class AirStuck extends Module {

    public final BooleanSetting catchMoment =
            new BooleanSetting("Ловить момент", true);

    private double peakY = Double.NaN;
    private boolean stuck;
    private Vec3d stuckPos = Vec3d.ZERO;

    public AirStuck() {
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null) {
            reset();
            return;
        }

        if (catchMoment.getValue()) {
            double y = mc.player.getY();
            if (mc.player.isOnGround()) {
                peakY = Double.NaN;
                stuck = false;
            } else if (!stuck) {
                if (Double.isNaN(peakY) || y > peakY) {
                    peakY = y;
                } else if (y < peakY) {
                    freezePlayer();
                }
            }
        }

        if (stuck) {
            mc.player.setVelocity(Vec3d.ZERO);
            mc.player.setPosition(stuckPos);
        }
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (stuck && event.getType() == EventPacket.Type.SEND
                && event.getPacket() instanceof PlayerMoveC2SPacket) {
            event.cancelEvent();
        }
    }

    @Override
    public void onEnable() {
        stuck = false;
        stuckPos = Vec3d.ZERO;
        if (mc.player == null) {
            peakY = Double.NaN;
        } else {
            if (catchMoment.getValue()) {
                peakY = mc.player.isOnGround() ? Double.NaN : mc.player.getY();
            } else {
                peakY = mc.player.getY();
                freezePlayer();
            }
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }

    private void freezePlayer() {
        if (mc.player != null) {
            stuck = true;
            stuckPos = mc.player.getPos();
            peakY = mc.player.getY();
        }
    }

    private void reset() {
        stuck = false;
        peakY = Double.NaN;
        stuckPos = Vec3d.ZERO;
    }
}
