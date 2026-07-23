
package dev.ethereal.api.utils.rotation.misc;

import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.rotation.RotationUtil;

import dev.ethereal.client.features.modules.combat.AuraModule;

public final class AuraUtil implements QuickImports {
    private static Vec3d dvdPoint;
    private static Vec3d dvdMotion;
    private static float hitCount;
    private static TimerUtil attackTimer;

    private static int lgPointIndex =0;
    private static int lgSameStreak = 0;
    private static int lgLastPoint = -1;
    private static long lgNextSwitchTime = 0L;
    private static final int LG_POINTS = 18;
    private static final java.util.Random lgRnd = new java.util.Random();

    private static boolean lgDiverting = false;
    private static long lgDivertEndTime = 0L;
    private static long lgNextDivertTime = 0L;
    private static Vec3d lgDivertTarget = null;

    private static void lgScheduleNextDivert() {
        lgNextDivertTime = System.currentTimeMillis() + 30L + (long)(lgRnd.nextFloat() * 120f);
    }

    private static void lgScheduleNext() {
        lgNextSwitchTime = System.currentTimeMillis() + 10L + (long)(lgRnd.nextFloat() * 190f);
    }

    private static void lgSwitchPoint() {
        lgSwitchPoint(false);
    }

    private static void lgSwitchPoint(boolean forceDifferentFromCurrent) {
        int next = lgPickNext(lgPointIndex);
        if (forceDifferentFromCurrent && next == lgPointIndex) {
            do {
                next = lgPickNext(lgPointIndex);
            } while (next == lgPointIndex);
        }
        lgSameStreak = (next == lgLastPoint) ? lgSameStreak + 1 : 0;
        lgLastPoint = next;
        lgPointIndex = next;
        lgScheduleNext();
    }

    private static Vec3d lgMakeDivertPoint(Entity entity) {
        Box box = entity.getBoundingBox();
        double cx = (box.minX + box.maxX) / 2.0;
        double cy = (box.minY + box.maxY) / 2.0;
        double cz = (box.minZ + box.maxZ) / 2.0;
        double angle = lgRnd.nextDouble() * Math.PI * 2;
        double dist  = 1.8 + lgRnd.nextDouble() * 3.5;
        double offY  = (lgRnd.nextDouble() - 0.5) * 5.0;
        return new Vec3d(cx + Math.cos(angle) * dist, cy + offY, cz + Math.sin(angle) * dist);
    }

    public static void onAttack(String mode) {
        switch (mode) {
            case "Spooky Time":
            case "Spooky Time 2":
                float hits = 0.3F;
                hitCount += hits;
                if (hitCount >= hits * 2.0F) {
                    hitCount = -hits;
                }
                break;
            case "Lony Grief":
                attackTimer.reset();
                lgSwitchPoint(true);
                break;
            case "FunTime":
                attackTimer.reset();
                lgSwitchPoint(true);
                break;
            default:
                ++hitCount;
                if (hitCount >= 3.0F) {
                    hitCount = 0.0F;
                }
        }

    }

    public static Vec3d getAimpoint(LivingEntity entity, String mode) {
        switch (mode) {
            case "Smooth" -> {
                return getDvDPoint(entity);
            }
            case "Spooky Time" -> {
                return getSpookyTimePoint(entity);
            }
            case "Spooky Time 2" -> {
                return getSpookyTimePoint2(entity);
            }
            case "Lony Grief" -> {
                return getLonyGriefPoint(entity);
            }
            case "FunTime" -> {
                return getFunTimePoint(entity);
            }
            default -> {
                return RotationUtil.getSpot(entity);
            }
        }
    }

    public static Vec3d getFunTimePoint(Entity entity) {
        long now = System.currentTimeMillis();
        if (now >= lgNextSwitchTime) {
            lgSwitchPoint();
        }
        return lgBuildPoint(entity, lgPointIndex);
    }

    public static Vec3d getLonyGriefPoint(Entity entity) {
        long now = System.currentTimeMillis();

        if (!lgDiverting && now >= lgNextDivertTime) {
            lgDiverting     = true;
            lgDivertEndTime = now + 10L + (long)(lgRnd.nextFloat() * 190f);
            lgDivertTarget  = lgMakeDivertPoint(entity);
        }
        if (lgDiverting && now >= lgDivertEndTime) {
            lgDiverting = false;
            lgScheduleNextDivert();
        }

        if (lgDiverting && lgDivertTarget != null) {
            return lgDivertTarget;
        }

        if (now >= lgNextSwitchTime) {
            lgSwitchPoint();
        }
        return lgBuildPoint(entity, lgPointIndex);
    }

    private static int lgPickNext(int current) {
        if (lgSameStreak >= 7) {
            int n;
            do { n = lgRnd.nextInt(LG_POINTS); } while (n == current);
            lgSameStreak = 0;
            return n;
        }

        if (lgRnd.nextFloat() < 0.40f) {
            int zone = current / 3;
            int nextZone = (zone + 1 + lgRnd.nextInt(5)) % (LG_POINTS / 3);
            return nextZone * 3 + lgRnd.nextInt(3);
        }
        return lgRnd.nextInt(LG_POINTS);
    }

    private static Vec3d lgBuildPoint(Entity entity, int index) {
        Box box = entity.getBoundingBox();
        double cx = (box.minX + box.maxX) / 2.0;
        double cz = (box.minZ + box.maxZ) / 2.0;
        double h  = box.maxY - box.minY;
        double hw = (box.maxX - box.minX) / 2.0;
        double nx = (lgRnd.nextDouble() - 0.5) * 0.07;
        double nz = (lgRnd.nextDouble() - 0.5) * 0.07;
        return switch (index) {
            case 0  -> new Vec3d(cx + nx,        box.minY + h * 0.96, cz + nz);
            case 1  -> new Vec3d(cx + hw * 0.5,  box.minY + h * 0.93, cz - hw * 0.3);
            case 2  -> new Vec3d(cx - hw * 0.5,  box.minY + h * 0.93, cz + hw * 0.3);
            case 3  -> new Vec3d(cx + nx,         box.minY + h * 0.85, cz + nz);
            case 4  -> new Vec3d(cx + hw * 0.4,   box.minY + h * 0.82, cz + nz);
            case 5  -> new Vec3d(cx - hw * 0.4,   box.minY + h * 0.82, cz + nz);
            case 6  -> new Vec3d(cx + nx,          box.minY + h * 0.72, cz + nz);
            case 7  -> new Vec3d(cx + hw * 0.55,   box.minY + h * 0.70, cz + nz);
            case 8  -> new Vec3d(cx - hw * 0.55,   box.minY + h * 0.70, cz + nz);
            case 9  -> new Vec3d(cx + nx,           box.minY + h * 0.58, cz + nz);
            case 10 -> new Vec3d(cx + hw * 0.4,     box.minY + h * 0.55, cz + nz);
            case 11 -> new Vec3d(cx - hw * 0.4,     box.minY + h * 0.55, cz + nz);
            case 12 -> new Vec3d(cx + nx,            box.minY + h * 0.42, cz + nz);
            case 13 -> new Vec3d(cx + hw * 0.3,      box.minY + h * 0.38, cz + nz);
            case 14 -> new Vec3d(cx - hw * 0.3,      box.minY + h * 0.38, cz + nz);
            case 15 -> new Vec3d(cx + nx,             box.minY + h * 0.22, cz + nz);
            case 16 -> new Vec3d(cx + hw * 0.35,      box.minY + h * 0.18, cz + nz);
            default -> new Vec3d(cx - hw * 0.35,      box.minY + h * 0.08, cz + nz);
        };
    }

    public static Vec3d getBestVector(Entity entity, float jitterOnBoxValue) {
        double yExpand = (double)(MathHelper.clamp(mc.player.getEyeHeight(mc.player.getPose()) - entity.getEyeHeight(entity.getPose()), entity.getHeight() / 2.0F, entity.getHeight()) / (mc.player.isGliding() ? 10.0F : (!mc.options.jumpKey.isPressed() && mc.player.isOnGround() ? (entity.isSneaking() ? 0.8F : 0.6F) : 1.0F)));
        Vec3d finalVector = entity.getPos().add((double)0.0F, yExpand, (double)0.0F);
        return finalVector.add((double)jitterOnBoxValue, (double)(jitterOnBoxValue / 2.0F), (double)jitterOnBoxValue);
    }

    public static Vec3d getSpookyTimePoint(Entity entity) {
        Box box = entity.getBoundingBox();
        return new Vec3d(
                (box.minX + box.maxX) / 2.0,
                box.maxY - 0.05,
                (box.minZ + box.maxZ) / 2.0
        );
    }

    public static Vec3d getSpookyTimePoint2(Entity entity) {
        Box box = entity.getBoundingBox();
        return new Vec3d(
                (box.minX + box.maxX) / 2.0 + MathUtil.random(-0.06, 0.06),
                box.maxY - 0.05 + MathUtil.random(-0.1, 0.1),
                (box.minZ + box.maxZ) / 2.0 + MathUtil.random(-0.06, 0.06)
        );
    }

    public static Vec3d getDistanceBasedPoint(Entity entity) {
        Vec3d eye = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        float attackDistance = AuraModule.getInstance().getAttackDistance() + AuraModule.getInstance().getPreDistance();
        float distanceFactor = (float)(mc.player.getPos().distanceTo(entity.getPos()) / (double)attackDistance);
        float minY = (float)(box.maxY - box.minY);
        float clampedY = (float)Math.max(box.minY + (double)(minY * distanceFactor), box.minY + (double)(minY * 0.3F));
        float safePoint = entity.getWidth() * 0.4F;
        Vec3d basePoint = new Vec3d(MathHelper.clamp(eye.x, box.minX + (double)safePoint, box.maxX - (double)safePoint), MathHelper.clamp(eye.y, box.minY, (double)clampedY), MathHelper.clamp(eye.z, box.minZ + (double)safePoint, box.maxZ - (double)safePoint));
        return basePoint;
    }

    public static Vec3d getFTVec(Entity entity) {
        Vec3d eye = mc.player.getEyePos();
        Box box = entity.getBoundingBox();
        float safePoint = entity.getWidth() * 0.4F;
        double x = MathHelper.clamp(eye.x, box.minX + safePoint, box.maxX - safePoint);
        double z = MathHelper.clamp(eye.z, box.minZ + safePoint, box.maxZ - safePoint);
        double y = box.minY + MathUtil.random(0.0f, (float)(box.maxY - box.minY));
        return new Vec3d(x, y, z);
    }

    public static Vec3d getDvDPoint(Entity entity) {
        float minMotionXZ = 0.003F;
        float maxMotionXZ = 0.04F;
        float minMotionY = 0.001F;
        float maxMotionY = 0.03F;
        double lengthX = entity.getBoundingBox().getLengthX();
        double lengthY = entity.getBoundingBox().getLengthY() * (double)1.0F;
        double lengthZ = entity.getBoundingBox().getLengthZ();
        if (dvdMotion.equals(Vec3d.ZERO)) {
            dvdMotion = new Vec3d((double)MathUtil.random(-0.05F, 0.05F), (double)MathUtil.random(-0.05F, 0.05F), (double)MathUtil.random(-0.05F, 0.05F));
        }

        dvdPoint = dvdPoint.add(dvdMotion);
        if (dvdPoint.x >= (lengthX - 0.05) / (double)2.0F) {
            dvdMotion = new Vec3d((double)(-MathUtil.random(minMotionXZ, maxMotionXZ)), dvdMotion.getY(), dvdMotion.getZ());
        }

        if (dvdPoint.y >= lengthY) {
            dvdMotion = new Vec3d(dvdMotion.getX(), (double)(-MathUtil.random(minMotionY, maxMotionY)), dvdMotion.getZ());
        }

        if (dvdPoint.z >= (lengthZ - 0.05) / (double)2.0F) {
            dvdMotion = new Vec3d(dvdMotion.getX(), dvdMotion.getY(), (double)(-MathUtil.random(minMotionXZ, maxMotionXZ)));
        }

        if (dvdPoint.x <= -(lengthX - 0.05) / (double)2.0F) {
            dvdMotion = new Vec3d((double)MathUtil.random(minMotionXZ, 0.03F), dvdMotion.getY(), dvdMotion.getZ());
        }

        if (dvdPoint.y <= lengthY * 0.4) {
            dvdMotion = new Vec3d(dvdMotion.getX(), (double)MathUtil.random(minMotionY, maxMotionY), dvdMotion.getZ());
        }

        if (dvdPoint.z <= -(lengthZ - 0.05) / (double)2.0F) {
            dvdMotion = new Vec3d(dvdMotion.getX(), dvdMotion.getY(), (double)MathUtil.random(minMotionXZ, maxMotionXZ));
        }

        dvdPoint.add((double)MathUtil.random(-0.03F, 0.03F), (double)0.0F, (double)MathUtil.random(-0.03F, 0.03F));
        Vec3d dvdPointed = entity.getPos().add(dvdPoint);
        Box box = entity.getBoundingBox();
        return new Vec3d(MathHelper.clamp(dvdPointed.x, box.minX, box.maxX), MathHelper.clamp(dvdPointed.y, box.minY, box.maxY), MathHelper.clamp(dvdPointed.z, box.minZ, box.maxZ));
    }

    @Generated
    private AuraUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    static {
        dvdPoint = Vec3d.ZERO;
        dvdMotion = Vec3d.ZERO;
        hitCount = 0.0F;
        attackTimer = new TimerUtil();
        lgScheduleNextDivert();
    }
}
