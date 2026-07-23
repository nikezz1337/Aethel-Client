package dev.ethereal.api.utils.rotation.manager;

import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.player.move.MotionEvent;
import dev.ethereal.api.event.events.other.TraceEvent;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.combat.ServerManager;
import dev.ethereal.api.utils.other.NetworkUtil;
import dev.ethereal.api.utils.rotation.RotationChanger;
import dev.ethereal.api.utils.rotation.RotationData;
import lombok.Getter;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RotationManager implements QuickImports {
    @Getter private static final RotationManager instance = new RotationManager();

    private final List<RotationChanger> changers = new ArrayList<>();
    @Getter private final RotationData rotationData = new RotationData();

    private RotationManager() {
        Events.subscribe(this);
    }

    public void load() {
    }

    public void addRotation(RotationChanger changer) {
        if (mc.player == null || mc.world == null) return;

        if (!changers.contains(changer)) {
            changers.add(changer);
            sortRotations();
        }

        rotationData.setRotation(changers.getFirst().rotations().get()[0], changers.getFirst().rotations().get()[1]);
    }

    public void removeRotation(RotationChanger changer) {
        if (mc.player == null || mc.world == null) return;

        changers.remove(changer);
        sortRotations();
    }

    public void addPacketRotation(float[] rotations) {
        if (mc.player == null || mc.world == null
                || Math.abs(rotations[0] - ServerManager.getInstance().getServerYaw()) < 0.001f
                || Math.abs(rotations[1] - ServerManager.getInstance().getServerPitch()) < 0.001f
        ) return;

        NetworkUtil.sendPacket(new PlayerMoveC2SPacket.Full(
                ServerManager.getInstance().getServerX(),
                ServerManager.getInstance().getServerY(),
                ServerManager.getInstance().getServerZ(),
                rotations[0],
                rotations[1],
                ServerManager.getInstance().isServerOnGround(),
                ServerManager.getInstance().isServerHorizontalCollision()
        ));
    }

    public boolean isEmpty() {
        return changers.isEmpty();
    }

    public Rotation getServerRotation() {
        return new Rotation(ServerManager.getInstance().getServerYaw(), ServerManager.getInstance().getServerPitch());
    }

    public Rotation getCurrentRotation() {
        if (changers.isEmpty()) return null;
        return new Rotation(rotationData.getYaw(), rotationData.getPitch());
    }

    public Rotation getRotation() {
        Rotation currentRotation = getCurrentRotation();
        return currentRotation != null ? currentRotation : getServerRotation();
    }

    public Rotation getPreviousRotation() {
        return getServerRotation();
    }

    public void updateServerRotation(Rotation rotation) {
        ServerManager.getInstance().setServerYaw(rotation.getYaw());
        ServerManager.getInstance().setServerPitch(rotation.getPitch());
    }

    public RotationPlan getCurrentRotationPlan() {
        if (changers.isEmpty()) return null;
        return new RotationPlan();
    }

    private void sortRotations() {
        changers.sort(Comparator.comparing(RotationChanger::priority));
        Collections.reverse(changers);
    }

    @EventHandler
    public void onTrace(TraceEvent e) {
        if (mc.player == null || mc.world == null || changers.isEmpty() || !false) return;

        if (changers.getFirst().remove().get()) removeRotation(changers.getFirst());

        e.setYaw(rotationData.getYaw());
        e.setPitch(rotationData.getPitch());
        e.cancel();
    }

    @EventHandler
    public void onMotion(MotionEvent e) {
        if (mc.player == null || mc.world == null || changers.isEmpty()) return;

        if (changers.getFirst().remove().get()) removeRotation(changers.getFirst());

        e.setYaw(rotationData.getYaw());
        e.setPitch(rotationData.getPitch());
        mc.player.setHeadYaw(rotationData.getYaw());
        mc.player.setBodyYaw(rotationData.getYaw());
    }

}
