package dev.ethereal.api.utils.rotation.manager;

import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.player.move.JumpEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.event.events.player.move.MotionEvent;
import dev.ethereal.api.event.events.player.move.TravelEvent;
import dev.ethereal.api.module.ModuleManager;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.client.features.modules.combat.Aura;
import net.minecraft.util.math.MathHelper;

public final class RotationComponent implements QuickImports {
    private static final RotationComponent INSTANCE = new RotationComponent();

    public static RotationComponent getInstance() {
        return INSTANCE;
    }

    private static final long AURA_ACQUIRE_DURATION = 180L;
    private static final long AURA_RETURN_DURATION = 250L;

    private boolean auraActive;
    private boolean auraAcquiring;
    private boolean auraReturning;
    private long auraTransitionStartTime;
    private float auraStartYaw;
    private float auraStartPitch;
    private float auraTargetYaw;
    private float auraTargetPitch;
    private float auraYaw;
    private float auraPitch;
    private float auraVisualYaw;
    private float auraVisualPitch;
    private boolean auraVisualInitialized;

    private RotationComponent() {
        Events.subscribe(this);
    }

    @EventHandler
    public void onPlayerTick(UpdateEvent event) {
        Aura aura = ModuleManager.getInstance().get(Aura.class);
        if (aura == null) {
            clearAuraState();
            return;
        }

        tickAuraState(aura);
    }

    @EventHandler
    public void onMotion(MotionEvent event) {
        if (!shouldApplyDisabledAuraReturn()) return;

        event.setYaw(auraYaw);
        event.setPitch(auraPitch);
        mc.player.setHeadYaw(auraVisualYaw);
        mc.player.setBodyYaw(calculateCorrectYawOffset(auraVisualYaw));
    }

    @EventHandler
    public void onTravel(TravelEvent event) {
        if (!shouldApplyDisabledAuraReturn()) return;

        event.setYaw(auraYaw);
        event.setPitch(auraPitch);
    }

    @EventHandler
    public void onJump(JumpEvent event) {
        if (!shouldApplyDisabledAuraReturn()) return;

        event.setYaw(auraYaw);
    }

    public void onAuraEnable() {
        if (mc.player == null || mc.world == null) {
            clearAuraState();
            return;
        }

        auraYaw = mc.player.getYaw();
        auraPitch = mc.player.getPitch();
        auraVisualYaw = auraYaw;
        auraVisualPitch = auraPitch;
        auraVisualInitialized = true;
        auraActive = false;
        auraAcquiring = false;
        auraReturning = false;
    }

    public void onAuraTargetAcquired(Aura aura) {
        if (mc.player == null || mc.world == null) {
            clearAuraState();
            return;
        }

        float[] desired = aura.getCurrentRotations();
        auraStartYaw = hasAuraRotationTask() ? auraYaw : mc.player.getYaw();
        auraStartPitch = hasAuraRotationTask() ? auraPitch : mc.player.getPitch();
        auraTargetYaw = desired[0];
        auraTargetPitch = desired[1];
        auraTransitionStartTime = System.currentTimeMillis();
        auraActive = true;
        auraAcquiring = true;
        auraReturning = false;

        if (!auraVisualInitialized) {
            auraVisualYaw = auraStartYaw;
            auraVisualPitch = auraStartPitch;
            auraVisualInitialized = true;
        }
    }

    public void onAuraTargetLost() {
        if (mc.player == null || mc.world == null) {
            clearAuraState();
            return;
        }

        if (!hasAuraRotationTask()) {
            return;
        }

        startAuraReturn(mc.player.getYaw(), mc.player.getPitch());
    }

    public void onAuraDisable() {
        if (mc.player == null || mc.world == null) {
            clearAuraState();
            return;
        }

        if (!hasAuraRotationTask()) {
            clearAuraState();
            return;
        }

        float yawDiff = Math.abs(MathHelper.wrapDegrees(auraYaw - mc.player.getYaw()));
        float pitchDiff = Math.abs(auraPitch - mc.player.getPitch());
        if (yawDiff <= 5.0f && pitchDiff <= 5.0f) {
            clearAuraState();
            return;
        }

        startAuraReturn(mc.player.getYaw(), mc.player.getPitch());
    }

    public boolean hasAuraRotationTask() {
        return auraActive || auraReturning;
    }

    public boolean isAuraReady() {
        return auraActive && !auraAcquiring && !auraReturning;
    }

    public boolean hasAuraVisualRotation(Aura aura) {
        return aura != null && auraVisualInitialized && hasAuraRotationTask();
    }

    public float getAuraYaw() {
        return auraYaw;
    }

    public float getAuraPitch() {
        return auraPitch;
    }

    public float getAuraVisualYaw() {
        return auraVisualYaw;
    }

    public float getAuraVisualPitch() {
        return auraVisualPitch;
    }

    public boolean isRotating() {
        return hasAuraRotationTask();
    }

    public Rotation currentRotation() {
        if (!hasAuraRotationTask()) return null;
        return new Rotation(auraYaw, auraPitch);
    }

    public void stopRotation() {
        clearAuraState();
    }

    public static void update(Rotation rotation, float yawSpeed, float pitchSpeed, float yawSpeedMax, float pitchSpeedMax, int ticks, int priority, boolean clientLook) {
        int[] remainingTicks = {Math.max(ticks, 1)};
        RotationManager.getInstance().addRotation(new dev.ethereal.api.utils.rotation.RotationChanger(
                priority,
                () -> new Float[]{rotation.getYaw(), rotation.getPitch()},
                () -> --remainingTicks[0] <= 0
        ));

        if (clientLook && mc.player != null) {
            mc.player.setYaw(rotation.getYaw());
            mc.player.setPitch(rotation.getPitch());
        }
    }

    private void tickAuraState(Aura aura) {
        if (mc.player == null || mc.world == null) {
            clearAuraState();
            return;
        }

        if (auraReturning) {
            updateAuraReturn();
            updateAuraVisualRotation();
            return;
        }

        if (!aura.isEnabled() || aura.getTarget() == null) {
            if (!auraActive) {
                clearAuraState();
            }
            return;
        }

        float[] desired = aura.getCurrentRotations();
        if (!auraActive) {
            auraYaw = desired[0];
            auraPitch = desired[1];
            auraActive = true;
        } else {
            long elapsed = System.currentTimeMillis() - auraTransitionStartTime;
            float progress = Math.min(1.0f, (float) elapsed / AURA_ACQUIRE_DURATION);
            if (progress >= 1.0f) {
                auraYaw = desired[0];
                auraPitch = desired[1];
                auraAcquiring = false;
            } else {
                float eased = easeOutCubic(progress);
                auraYaw = interpolateAngle(auraStartYaw, desired[0], eased);
                auraPitch = auraStartPitch + (desired[1] - auraStartPitch) * eased;
            }
        }

        auraYaw = snapYaw(auraYaw);
        auraPitch = snapPitch(auraPitch);
        updateAuraVisualRotation();
    }

    private void startAuraReturn(float targetYaw, float targetPitch) {
        auraStartYaw = auraYaw;
        auraStartPitch = auraPitch;
        auraTargetYaw = closestYawTo(auraStartYaw, targetYaw);
        auraTargetPitch = targetPitch;
        auraTransitionStartTime = System.currentTimeMillis();
        auraActive = false;
        auraAcquiring = false;
        auraReturning = true;
    }

    private void updateAuraReturn() {
        auraTargetYaw = closestYawTo(auraYaw, mc.player.getYaw());
        auraTargetPitch = mc.player.getPitch();

        long elapsed = System.currentTimeMillis() - auraTransitionStartTime;
        float progress = Math.min(1.0f, (float) elapsed / AURA_RETURN_DURATION);
        float eased = easeInOutCubic(progress);

        auraYaw = interpolateAngle(auraStartYaw, auraTargetYaw, eased);
        auraPitch = auraStartPitch + (auraTargetPitch - auraStartPitch) * eased;
        auraYaw = snapYaw(auraYaw);
        auraPitch = snapPitch(auraPitch);

        if (progress >= 1.0f) {
            finishAuraReturn();
            clearAuraState();
        }
    }

    private void finishAuraReturn() {
        if (mc.player == null) return;

        float playerYaw = mc.player.getYaw();
        if (Math.abs(auraTargetYaw - playerYaw) > 180.0f
                && Math.abs(MathHelper.wrapDegrees(auraTargetYaw - playerYaw)) <= 1.0f) {
            mc.player.setYaw(auraTargetYaw);
        }
    }

    private void updateAuraVisualRotation() {
        if (!auraVisualInitialized) {
            auraVisualYaw = auraYaw;
            auraVisualPitch = auraPitch;
            auraVisualInitialized = true;
            return;
        }

        float deltaYaw = MathHelper.wrapDegrees(auraYaw - auraVisualYaw);
        float deltaPitch = auraPitch - auraVisualPitch;
        auraVisualYaw += deltaYaw * 0.55f;
        auraVisualPitch += deltaPitch * 0.55f;
    }

    private void clearAuraState() {
        auraActive = false;
        auraAcquiring = false;
        auraReturning = false;
        auraVisualInitialized = false;
        auraTransitionStartTime = 0L;
    }

    private boolean shouldApplyDisabledAuraReturn() {
        Aura aura = ModuleManager.getInstance().get(Aura.class);
        return aura != null
                && !aura.isEnabled()
                && auraReturning
                && mc.player != null
                && mc.world != null
                && RotationManager.getInstance().isEmpty();
    }

    private float snapYaw(float yaw) {
        if (mc.player == null) return yaw;
        float gcd = MouseUtil.getGCD();
        return mc.player.getYaw() + Math.round((yaw - mc.player.getYaw()) / gcd) * gcd;
    }

    private float snapPitch(float pitch) {
        if (mc.player == null) return pitch;
        float gcd = MouseUtil.getGCD();
        float snapped = mc.player.getPitch() + Math.round((pitch - mc.player.getPitch()) / gcd) * gcd;
        return MathHelper.clamp(snapped, -90.0f, 90.0f);
    }

    private float interpolateAngle(float start, float end, float progress) {
        return start + MathHelper.wrapDegrees(end - start) * progress;
    }

    private float closestYawTo(float referenceYaw, float yaw) {
        return referenceYaw + MathHelper.wrapDegrees(yaw - referenceYaw);
    }

    private float easeInOutCubic(float t) {
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0) / 2.0f;
    }

    private float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3.0);
    }

    static float calculateCorrectYawOffset(float yaw) {
        if (mc.player == null) return yaw;

        double xDiff = mc.player.getX() - mc.player.prevX;
        double zDiff = mc.player.getZ() - mc.player.prevZ;
        float distSquared = (float) (xDiff * xDiff + zDiff * zDiff);
        float bodyYaw = mc.player.prevBodyYaw;
        float offset = bodyYaw;

        if (distSquared > 0.0025000002f) {
            offset = (float) MathHelper.atan2(zDiff, xDiff) * 180.0f / (float) Math.PI - 90.0f;
        }

        if (mc.player.handSwinging) {
            offset = yaw;
        }

        float yawOffsetDiff = MathHelper.wrapDegrees(yaw - (bodyYaw + MathHelper.wrapDegrees(offset - bodyYaw) * 0.3f));
        yawOffsetDiff = MathHelper.clamp(yawOffsetDiff, -75.0f, 75.0f);
        bodyYaw = yaw - yawOffsetDiff;
        if (yawOffsetDiff * yawOffsetDiff > 2500.0f) {
            bodyYaw += yawOffsetDiff * 0.2f;
        }

        return bodyYaw;
    }
}
