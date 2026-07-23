package antileak.base.client.modules.impl.player;

import antileak.base.client.modules.settings.implement.FloatSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventMoveInput;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.api.utils.math.TimerUtils;
import antileak.base.api.utils.player.InventoryUtils;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.settings.implement.BooleanSetting;
import antileak.base.elysium;

import java.util.List;

public class TargetPearl extends Module {

    public static final TargetPearl INSTANCE = new TargetPearl();

    private final BooleanSetting onlyTarget      = new BooleanSetting("Только за таргетом", false);
    private final FloatSetting   minDist         = new FloatSetting("Мин дистанция", 10, 8, 20, 1);
    private final BooleanSetting ignoreFriends   = new BooleanSetting("Игнорировать друзей", true);
    private final BooleanSetting noThrowOnEntity = new BooleanSetting("Не кидать в игрока", true);

    private final TimerUtils timer = new TimerUtils();

    private EnderPearlEntity targetPearl       = null;
    private Vec3d            cachedLanding     = null;
    private long             lastPearlScan     = 0L;
    private long             lastThrowTime     = 0L;
    private boolean          isThrowing        = false;
    public  Vec2f            server            = null;

    private Entity rememberedTarget     = null;
    private long   rememberedTargetTime = 0L;

    public TargetPearl() {
        super("TargetPearl", "Автоматически бросает жемчуг в цель", ModuleCategory.PLAYER);
        addSettings(onlyTarget, minDist, ignoreFriends, noThrowOnEntity);
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.player.isGliding()) return;
        tryThrow();
    }

    @EventLink
    public void onMoveInput(EventMoveInput event) {
        if (!isEnable() || !isThrowing || server == null) return;

        float forward = event.getForward();
        float strafe  = event.getStrafe();
        if (forward == 0f && strafe == 0f) return;

        double targetAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(server.x, forward, strafe)));
        float  bestFwd = 0f, bestStr = 0f;
        float  smallest = Float.MAX_VALUE;

        for (float tf = -1f; tf <= 1f; tf++) {
            for (float ts = -1f; ts <= 1f; ts++) {
                if (tf == 0f && ts == 0f) continue;
                double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(server.x, tf, ts)));
                float  diff  = Math.abs(MathHelper.wrapDegrees((float)(targetAngle - angle)));
                if (diff < smallest) { smallest = diff; bestFwd = tf; bestStr = ts; }
            }
        }

        event.setForward(bestFwd);
        event.setStrafe(bestStr);
    }

    @Override
    public void onDisable() {
        isThrowing        = false;
        targetPearl       = null;
        cachedLanding     = null;
        server            = null;
        lastThrowTime     = 0;
        rememberedTarget     = null;
        rememberedTargetTime = 0;
        timer.reset();
        super.onDisable();
    }

    private void tryThrow() {
        if (System.currentTimeMillis() - lastThrowTime < 2500) return;
        if (!canThrow()) return;

        updateTargetPearl();
        if (cachedLanding == null) {
            isThrowing = false;
            server = null;
            return;
        }

        float[] rot = calculateYawPitch(cachedLanding);
        if (rot == null) rot = findAnyWorkingRotation(cachedLanding);
        if (rot == null) {
            isThrowing = false;
            server = null;
            return;
        }

        Vec3d predicted = simulateTrajectory(rot[0], rot[1]);
        if (predicted != null) {
            Vec3d  eye        = mc.player.getEyePos();
            double distToLand = eye.distanceTo(cachedLanding);
            double hDiff      = cachedLanding.y - eye.y;
            boolean isHigh    = hDiff > 5.0;
            double maxErr     = distToLand > 60 ? 3.0 : 2.25;
            if (isHigh) maxErr = 4.5;
            if (hasObstacleBetween(eye, cachedLanding)) maxErr = isHigh ? 6.0 : 3.75;

            if (cachedLanding.distanceTo(predicted) > maxErr) {
                rot = findAnyWorkingRotation(cachedLanding);
                if (rot == null) return;
                predicted = simulateTrajectory(rot[0], rot[1]);
                if (predicted == null || cachedLanding.distanceTo(predicted) > maxErr * 1.5) return;
            }
        }

        if (noThrowOnEntity.isState() && hasPlayerOnPath(rot[0], rot[1])) return;

        if (!hasPearl()) return;

        float prevYaw   = mc.player.getYaw();
        float prevPitch = mc.player.getPitch();

        mc.player.setYaw(rot[0]);
        mc.player.setPitch(rot[1]);

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                rot[0], rot[1], mc.player.isOnGround(), mc.player.horizontalCollision
        ));

        isThrowing = true;
        server = new Vec2f(rot[0], rot[1]);

        InventoryUtils.swapDef(Items.ENDER_PEARL);

        mc.player.setYaw(prevYaw);
        mc.player.setPitch(prevPitch);

        isThrowing = false;
        server = null;
        lastThrowTime = System.currentTimeMillis();
        timer.reset();
    }

    private boolean canThrow() {
        return !mc.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL.getDefaultStack())
                && timer.finished(1000L);
    }

    private boolean hasPlayerOnPath(float yaw, float pitch) {
        Vec3d pos    = throwPos(yaw);
        Vec3d motion = throwMotion(yaw, pitch);

        for (int i = 0; i < 200; i++) {
            Vec3d prev = pos;
            pos    = pos.add(motion);
            motion = motion.multiply(0.99).add(0, -0.03, 0);

            if (pos.y <= mc.world.getBottomY()) break;

            BlockPos bp = BlockPos.ofFloored(pos);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) break;

            Box segment = new Box(prev, pos).expand(0.35);

            for (Entity entity : mc.world.getOtherEntities(mc.player, segment, e ->
                    e instanceof PlayerEntity
                            && e.isAlive()
                            && !e.isSpectator()
                            && e != mc.player)) {
                if (entity.getBoundingBox().expand(0.25).intersects(segment)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateTargetPearl() {
        long now = System.currentTimeMillis();
        if (now - lastPearlScan < 50) return;
        lastPearlScan = now;
        cachedLanding = null;
        targetPearl   = findBestPearl();

        if (targetPearl != null && targetPearl.isAlive()) {
            cachedLanding = predictLanding(targetPearl);
            if (cachedLanding != null) {
                BlockPos pos = BlockPos.ofFloored(cachedLanding);
                if (mc.world.getBlockState(pos.down()).getCollisionShape(mc.world, pos.down()).isEmpty()
                        && mc.world.getBlockState(pos.down(2)).getCollisionShape(mc.world, pos.down(2)).isEmpty()) {
                    cachedLanding = null;
                }
            }
        }
    }

    private EnderPearlEntity findBestPearl() {
        List<Entity> entities = mc.world.getOtherEntities(
                mc.player, mc.player.getBoundingBox().expand(130.0));

        EnderPearlEntity best     = null;
        double           bestDist = Double.MAX_VALUE;
        double           minD     = minDist.getValue().doubleValue();

        Entity targetToUse = null;
        if (onlyTarget.isState()) {
            LivingEntity auraTarget = ModuleClass.INSTANCE != null
                    ? ModuleClass.INSTANCE.aura.getTarget() : null;
            long now = System.currentTimeMillis();

            if (auraTarget != null && auraTarget.isAlive()) {
                rememberedTarget     = auraTarget;
                rememberedTargetTime = now;
                targetToUse          = auraTarget;
            } else if (rememberedTarget != null) {
                if (!rememberedTarget.isAlive()) {
                    rememberedTarget = null;
                    rememberedTargetTime = 0;
                } else if (now - rememberedTargetTime < 10_000L) {
                    targetToUse = rememberedTarget;
                } else {
                    rememberedTarget = null;
                    rememberedTargetTime = 0;
                }
            }
        }

        for (Entity e : entities) {
            if (!(e instanceof EnderPearlEntity pearl) || !pearl.isAlive()) continue;
            if (pearl.getOwner() == mc.player) continue;
            if (isIgnoredFriend(pearl.getOwner())) continue;
            if (onlyTarget.isState()) {
                if (targetToUse == null) continue;
                if (!targetToUse.equals(pearl.getOwner())) continue;
            }

            Vec3d landing = predictLanding(pearl);
            if (landing == null) continue;

            double dist = mc.player.getPos().distanceTo(landing);
            if (dist >= minD && dist <= 120.0 && dist < bestDist) {
                best     = pearl;
                bestDist = dist;
            }
        }
        return best;
    }

    private Vec3d predictLanding(EnderPearlEntity pearl) {
        Vec3d pos = pearl.getPos();
        Vec3d vel = pearl.getVelocity();

        for (int i = 0; i < 200; i++) {
            Vec3d next = pos.add(vel);
            vel = vel.multiply(0.99).add(0, -0.03, 0);

            if (next.y <= mc.world.getBottomY()) return snapToBlockCenter(next);

            BlockPos bp = BlockPos.ofFloored(next);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty())
                return snapToBlockCenter(next);

            pos = next;
        }
        return null;
    }

    private Vec3d snapToBlockCenter(Vec3d v) {
        return new Vec3d(MathHelper.floor(v.x) + 0.5, MathHelper.floor(v.y), MathHelper.floor(v.z) + 0.5);
    }

    private boolean hasObstacleBetween(Vec3d start, Vec3d end) {
        Vec3d dir  = end.subtract(start);
        double len = dir.length();
        Vec3d norm = dir.normalize();
        int steps  = (int)(len / 0.5) + 1;
        for (int i = 1; i < steps; i++) {
            Vec3d check = start.add(norm.multiply(i * 0.5));
            BlockPos bp = BlockPos.ofFloored(check);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) return true;
        }
        return false;
    }

    private float[] calculateYawPitch(Vec3d target) {
        Vec3d  eye   = mc.player.getEyePos();
        double dx    = target.x - eye.x;
        double dz    = target.z - eye.z;
        float  yaw   = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        double dist  = eye.distanceTo(target);
        double hDiff = target.y - eye.y;
        boolean isHigh = hDiff > 5.0;

        float  maxPitch = isHigh ? 89f : 85f;
        float  minPitch = isHigh ? -80f : (dist > 60 ? -50f : -30f);
        float  step     = isHigh ? 0.2f : (dist > 60 ? 0.3f : 0.25f);
        double maxErr   = isHigh ? 4.0 : 2.0;

        float  bestPitch = Float.MAX_VALUE;
        int    bestTicks = Integer.MAX_VALUE;
        double bestError = Double.MAX_VALUE;

        for (float pitch = maxPitch; pitch >= minPitch; pitch -= step) {
            SimResult res = simulateWithTicks(yaw, pitch, target);
            if (res != null && res.error <= maxErr) {
                if (res.ticks < bestTicks || (res.ticks == bestTicks && res.error < bestError)) {
                    bestTicks = res.ticks;
                    bestPitch = pitch;
                    bestError = res.error;
                }
            }
        }

        if (bestTicks != Integer.MAX_VALUE) {
            return new float[]{ yaw, MathHelper.clamp(bestPitch, -90f, 90f) };
        }
        return null;
    }

    private float[] findAnyWorkingRotation(Vec3d target) {
        Vec3d  eye     = mc.player.getEyePos();
        double dx      = target.x - eye.x;
        double dz      = target.z - eye.z;
        float  baseYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        double dist    = eye.distanceTo(target);
        double hDiff   = target.y - eye.y;
        boolean isHigh = hDiff > 5.0;

        float  maxPitch = isHigh ? 89f : 85f;
        float  minPitch = isHigh ? -85f : (dist > 60 ? -70f : -50f);
        float  step     = isHigh ? 0.8f : (dist > 60 ? 1.0f : 1.2f);
        float  yawRange = isHigh ? 40f : (dist > 60 ? 30f : 20f);
        float  yawStep  = isHigh ? 2f  : (dist > 60 ? 3f : 2.5f);
        double baseErr  = isHigh ? 4.5 : (dist > 60 ? 3.0 : 2.25);

        for (float pitch = maxPitch; pitch >= minPitch; pitch -= step) {
            Vec3d landing = simulateTrajectory(baseYaw, pitch);
            if (landing != null && target.distanceTo(landing) <= baseErr)
                return new float[]{ baseYaw, MathHelper.clamp(pitch, -90f, 90f) };
        }

        for (float yawOff = -yawRange; yawOff <= yawRange; yawOff += yawStep) {
            if (yawOff == 0) continue;
            float yaw = baseYaw + yawOff;
            for (float pitch = maxPitch; pitch >= minPitch; pitch -= step) {
                Vec3d landing = simulateTrajectory(yaw, pitch);
                if (landing != null && target.distanceTo(landing) <= baseErr)
                    return new float[]{ yaw, MathHelper.clamp(pitch, -90f, 90f) };
            }
        }

        if (isHigh) {
            for (float pitch = 89f; pitch >= 75f; pitch -= 1.0f) {
                for (float yawOff = -yawRange; yawOff <= yawRange; yawOff += yawStep) {
                    float yaw = baseYaw + yawOff;
                    Vec3d landing = simulateTrajectory(yaw, pitch);
                    if (landing != null && target.distanceTo(landing) <= baseErr * 1.5)
                        return new float[]{ yaw, MathHelper.clamp(pitch, -90f, 90f) };
                }
            }
        }

        for (float pitch = maxPitch; pitch >= 70f; pitch -= 2f) {
            for (float yawOff = -yawRange; yawOff <= yawRange; yawOff += yawStep * 2) {
                float yaw = baseYaw + yawOff;
                Vec3d landing = simulateTrajectory(yaw, pitch);
                if (landing != null && target.distanceTo(landing) <= baseErr * 2.0)
                    return new float[]{ yaw, MathHelper.clamp(pitch, -90f, 90f) };
            }
        }

        return null;
    }

    private static class SimResult {
        final double error;
        final int    ticks;
        SimResult(double error, int ticks) { this.error = error; this.ticks = ticks; }
    }

    private SimResult simulateWithTicks(float yaw, float pitch, Vec3d target) {
        Vec3d pos    = throwPos(yaw);
        Vec3d motion = throwMotion(yaw, pitch);

        for (int t = 0; t < 200; t++) {
            pos    = pos.add(motion);
            motion = motion.multiply(0.99).add(0, -0.03, 0);

            if (pos.y <= mc.world.getBottomY())
                return new SimResult(snapToBlockCenter(pos).distanceTo(target), t + 1);

            BlockPos bp = BlockPos.ofFloored(pos);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty())
                return new SimResult(snapToBlockCenter(pos).distanceTo(target), t + 1);
        }
        return null;
    }

    private Vec3d simulateTrajectory(float yaw, float pitch) {
        Vec3d pos    = throwPos(yaw);
        Vec3d motion = throwMotion(yaw, pitch);

        for (int i = 0; i < 200; i++) {
            pos    = pos.add(motion);
            motion = motion.multiply(0.99).add(0, -0.03, 0);

            if (pos.y <= mc.world.getBottomY()) return snapToBlockCenter(pos);

            BlockPos bp = BlockPos.ofFloored(pos);
            if (!mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty())
                return snapToBlockCenter(pos);
        }
        return null;
    }

    private Vec3d throwPos(float yaw) {
        float yr = (float) Math.toRadians(yaw);
        return new Vec3d(
                mc.player.getX() - MathHelper.cos(yr) * 0.16,
                mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()) - 0.1,
                mc.player.getZ() - MathHelper.sin(yr) * 0.16
        );
    }

    private Vec3d throwMotion(float yaw, float pitch) {
        double v  = 1.5;
        float  yr = (float) Math.toRadians(yaw);
        float  pr = (float) Math.toRadians(pitch);
        double vx = -MathHelper.sin(yr) * MathHelper.cos(pr) * v;
        double vy = -MathHelper.sin(pr) * v + mc.player.getVelocity().y;
        double vz =  MathHelper.cos(yr) * MathHelper.cos(pr) * v;
        return new Vec3d(vx, vy, vz);
    }

    private boolean hasPearl() {
        return mc.player.getMainHandStack().isOf(Items.ENDER_PEARL)
                || mc.player.getOffHandStack().isOf(Items.ENDER_PEARL)
                || InventoryUtils.find(Items.ENDER_PEARL, 0, 8)  != -1
                || InventoryUtils.find(Items.ENDER_PEARL, 9, 45) != -1;
    }

    private boolean isIgnoredFriend(Entity owner) {
        if (!ignoreFriends.isState() || !(owner instanceof PlayerEntity player)) return false;
        return elysium.INSTANCE != null
                && elysium.INSTANCE.friendStorage != null
                && elysium.INSTANCE.friendStorage.isFriend(player.getName().getString());
    }

    private static double direction(float yaw, float fwd, float strafe) {
        if (fwd < 0) yaw += 180f;
        float f = 1f;
        if (fwd < 0) f = -0.5f; else if (fwd > 0) f = 0.5f;
        if (strafe >  0) yaw -= 90f * f;
        if (strafe <  0) yaw += 90f * f;
        return Math.toRadians(yaw);
    }
}