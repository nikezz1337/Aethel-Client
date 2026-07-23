package dev.aethel.module.list.combat.aura.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

public class Rotation {
    public float yaw, pitch;

    public Rotation() {}

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Rotation(Entity entity) {
        this.yaw = entity.getYaw();
        this.pitch = entity.getPitch();
    }

    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public void setPitch(float pitch) { this.pitch = pitch; }

    public float getDelta(Rotation target) {
        float yawDelta = MathHelper.wrapDegrees(target.getYaw() - this.yaw);
        float pitchDelta = target.getPitch() - this.pitch;
        return (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
    }

    public double getDeltaDouble(Rotation target) {
        double yawDelta = MathHelper.wrapDegrees(target.getYaw() - yaw);
        double pitchDelta = MathHelper.wrapDegrees(target.getPitch() - pitch);
        return Math.hypot(yawDelta, pitchDelta);
    }

    public static Vec2f camera() {
        return new Vec2f(cameraYaw(), cameraPitch());
    }

    public static float cameraYaw() {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean frontView = mc.options.getPerspective().isFrontView();
        return MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() + (frontView ? 180 : 0));
    }

    public static float cameraPitch() {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean frontView = mc.options.getPerspective().isFrontView();
        return (frontView ? -1 : 1) * mc.gameRenderer.getCamera().getPitch();
    }
}
