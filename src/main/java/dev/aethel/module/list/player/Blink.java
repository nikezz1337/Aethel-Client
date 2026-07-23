package dev.aethel.module.list.player;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventPacket;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.list.combat.aura.util.time.TimerUtil;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.SliderSetting;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(
        moduleName = "Blink",
        moduleCategory = ModuleCategory.PLAYER,
        moduleDesc = "Задерживает пакеты движения"
)
public class Blink extends Module {

    public final BooleanSetting pulse =
            new BooleanSetting("Пульс", false);
    public final SliderSetting time =
            new SliderSetting("Время (сек)", 12.0, 1.0, 40.0, 1.0);
    public final BooleanSetting onlyGround =
            new BooleanSetting("Только с земли", true);

    private final List<Packet<?>> packets = new ArrayList<>();
    private final TimerUtil timer = new TimerUtil();
    private Vec3d lastPos;
    private boolean replaying;
    private boolean wasAirborne;

    public Blink() {
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (event.getType() != EventPacket.Type.SEND) return;
        if (replaying) return;

        Packet<?> packet = event.getPacket();
        if (packet instanceof PlayerMoveC2SPacket) {
            packets.add(packet);
            event.cancelEvent();

            boolean landed = onlyGround.getValue() && wasAirborne && mc.player.isOnGround();
            boolean pulseReady = pulse.getValue() && timer.hasReached((long) (time.getFloatValue() * 1000));

            if (!mc.player.isOnGround()) {
                wasAirborne = true;
            }

            if (landed || pulseReady) {
                releasePackets();
            }
        }
    }

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null) return;
        if (onlyGround.getValue() && !mc.player.isOnGround()) {
            wasAirborne = true;
        }
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        packets.clear();
        lastPos = mc.player.getPos();
        timer.reset();
        replaying = false;
        wasAirborne = !mc.player.isOnGround();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            if (canRelease()) {
                releasePackets();
            } else {
                packets.clear();
            }
            wasAirborne = false;
        }
        super.onDisable();
    }

    private boolean canRelease() {
        return !onlyGround.getValue() || mc.player == null || mc.player.isOnGround();
    }

    private void releasePackets() {
        if (mc.player == null || mc.player.networkHandler == null || packets.isEmpty()) return;

        replaying = true;
        for (Packet<?> packet : packets) {
            mc.player.networkHandler.sendPacket(packet);
        }
        replaying = false;
        packets.clear();

        if (mc.player != null) {
            lastPos = mc.player.getPos();
        }
        timer.reset();
        wasAirborne = false;
    }

    public int getPacketsCount() {
        return packets.size();
    }
}
