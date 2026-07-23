package dev.ethereal.api.utils.render.taksa;

import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.client.features.modules.combat.Aura;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TaksaBrain implements QuickImports {

    private static final double GRAVITY    = 0.08;
    private static final double DRAG_XZ    = 0.72;
    private static final double DRAG_Y     = 0.98;
    private static final double WATER_DRAG = 0.85;
    private static final double JUMP_FORCE = 0.48;
    private static final double HEIGHT     = 0.5;

    private Vec3d pos;
    private Vec3d prevPos;  // for partialTicks interpolation
    private Vec3d vel = Vec3d.ZERO;
    private boolean onGround = false;
    private boolean inWater  = false;

    // Smooth rotation (lerp, no allocations)
    private float smoothBody  = 0;
    private float smoothYaw   = 0;
    private float smoothPitch = 0;
    private float yaw  = 0;
    private float body = 0;

    @Getter private boolean lay;
    private long stayingTime = System.currentTimeMillis();

    public float prevLimbSwingAmount;
    public float limbSwingAmount;
    public float limbSwing;

    @Getter public float tailWag = 0;

    @Setter private PlayerEntity entity;

    public void update() {
        if (entity == null || mc.player == null || mc.world == null) return;

        Vec3d playerPos = entity.getPos();

        // Init / teleport if too far
        if (pos == null || pos.distanceTo(playerPos) > 14) {
            pos = playerPos.add(MathUtil.random(-1, 1), 0, MathUtil.random(-1, 1));
            prevPos = pos;
            vel = Vec3d.ZERO;
            return;
        }

        prevPos = pos;
        inWater = isInWater(pos);

        // Gravity
        if (inWater) {
            vel = new Vec3d(vel.x, vel.y * 0.5 + 0.03, vel.z);
        } else {
            vel = vel.add(0, -GRAVITY, 0);
        }

        // Goal: just run toward target and jump near it
        LivingEntity target = Aura.getInstance().getTarget();
        Vec3d goal;

        if (target != null && entity.equals(mc.player)) {
            goal = target.getPos();
            tailWag = MathHelper.lerp(0.12f, tailWag, 1f);
        } else {
            goal = playerPos;
            tailWag = MathHelper.lerp(0.08f, tailWag, 0f);
        }

        // Steer toward goal
        Vec3d toGoal = goal.subtract(pos);
        double hDist = toGoal.horizontalLength();
        if (hDist > 0.8) {
            double accel = inWater ? 0.05 : 0.10;
            Vec3d dir = toGoal.normalize();
            vel = vel.add(dir.x * accel, 0, dir.z * accel);
        }

        // Jump over blocks ahead
        if (onGround && !inWater && hDist > 0.3 && vel.horizontalLength() > 0.04) {
            Vec3d ahead = pos.add(new Vec3d(vel.x, 0, vel.z).normalize().multiply(0.4));
            if (isBlockSolid(ahead.x, pos.y + 0.1, ahead.z)) {
                vel = new Vec3d(vel.x, JUMP_FORCE, vel.z);
                onGround = false;
            }
        }

        // Periodic jump when chasing target
        if (target != null && onGround && !inWater && hDist > 0.8 && Math.random() < 0.05) {
            vel = new Vec3d(vel.x, JUMP_FORCE * MathUtil.random(0.9, 1.1), vel.z);
            onGround = false;
        }

        // Drag
        double drag = inWater ? WATER_DRAG : DRAG_XZ;
        vel = new Vec3d(vel.x * drag, vel.y * DRAG_Y, vel.z * drag);

        // Collide and move
        Vec3d newPos = collideAndMove(pos, vel);
        boolean nowOnGround = isOnGround(newPos);
        if (nowOnGround && !onGround) vel = new Vec3d(vel.x, 0, vel.z);
        onGround = nowOnGround;
        pos = newPos;

        handleRotation(target);
        limbTick();

        // Lay detection
        double moved = Math.hypot(pos.x - prevPos.x, pos.z - prevPos.z);
        if (moved > 0.01) stayingTime = System.currentTimeMillis();
        lay = System.currentTimeMillis() - stayingTime > 1500;
    }

    private Vec3d collideAndMove(Vec3d from, Vec3d v) {
        double x = from.x + v.x;
        double y = from.y + v.y;
        double z = from.z + v.z;

        if (v.y < 0 && isBlockSolid(from.x, from.y + v.y, from.z)) {
            y = Math.floor(from.y) + 0.001;
            vel = new Vec3d(vel.x, 0, vel.z);
        } else if (v.y > 0 && isBlockSolid(from.x, from.y + HEIGHT + v.y, from.z)) {
            y = from.y;
            vel = new Vec3d(vel.x, 0, vel.z);
        }

        if (isBlockSolid(x, from.y + 0.1, from.z) || isBlockSolid(x, from.y + HEIGHT - 0.1, from.z)) {
            x = from.x;
            vel = new Vec3d(0, vel.y, vel.z);
        }

        if (isBlockSolid(from.x, from.y + 0.1, z) || isBlockSolid(from.x, from.y + HEIGHT - 0.1, z)) {
            z = from.z;
            vel = new Vec3d(vel.x, vel.y, 0);
        }

        return new Vec3d(x, y, z);
    }

    private boolean isOnGround(Vec3d p) {
        return isBlockSolid(p.x, p.y - 0.05, p.z);
    }

    private void handleRotation(LivingEntity target) {
        if (Math.abs(vel.x) > 0.01 || Math.abs(vel.z) > 0.01) {
            yaw = (float) Math.toDegrees(Math.atan2(vel.z, vel.x)) - 90;
        }

        Vec3d lookAt = (target != null && entity.equals(mc.player)) ? target.getEyePos() : entity.getEyePos();
        float[] rot = getRotation(pos, lookAt);

        float limit = lay ? 200 : 150;
        float headLimit = lay ? 100 : 50;
        if (rot[0] - yaw < -limit || rot[0] - yaw > limit) yaw = rot[0];

        float shortestYaw = (float) (((((yaw - body) % 360) + 540) % 360) - 180);
        if (!lay) body += shortestYaw;

        smoothBody  = lerpAngle(smoothBody,  body, 0.25f);
        smoothYaw   = lerpAngle(smoothYaw,   MathHelper.clamp(rot[0] - yaw, -headLimit, headLimit), 0.25f);
        smoothPitch = lerpAngle(smoothPitch, rot[1], 0.25f);
    }

    private float lerpAngle(float cur, float target, float t) {
        return cur + MathHelper.wrapDegrees(target - cur) * t;
    }

    private float[] getRotation(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        return new float[]{
            (float) Math.toDegrees(Math.atan2(dz, dx)) - 90,
            (float) -Math.toDegrees(Math.atan2(dy, dist))
        };
    }

    public void limbTick() {
        prevLimbSwingAmount = limbSwingAmount;
        double moved = prevPos != null ? Math.hypot(pos.x - prevPos.x, pos.z - prevPos.z) : 0;
        float f = Math.min((float) moved * 4f, 1f);
        limbSwingAmount += (f - limbSwingAmount) * 0.4f;
        limbSwing += limbSwingAmount;
    }

    private boolean isBlockSolid(double x, double y, double z) {
        if (mc.world == null) return false;
        BlockPos bp = BlockPos.ofFloored(x, y, z);
        BlockState bs = mc.world.getBlockState(bp);
        return !bs.getCollisionShape(mc.world, bp).isEmpty();
    }

    private boolean isInWater(Vec3d p) {
        if (mc.world == null) return false;
        BlockPos bp = BlockPos.ofFloored(p.x, p.y + 0.1, p.z);
        return !mc.world.getBlockState(bp).getFluidState().isEmpty();
    }

    public float getBody()  { return smoothBody; }
    public float getYaw()   { return smoothYaw; }
    public float getPitch() { return smoothPitch; }

    /** Returns interpolated position for smooth rendering between ticks */
    public Vec3d getPos(float partialTicks) {
        if (prevPos == null || pos == null) return pos != null ? pos : Vec3d.ZERO;
        return prevPos.lerp(pos, partialTicks);
    }

    public Vec3d getPos() {
        return pos != null ? pos : Vec3d.ZERO;
    }
}
