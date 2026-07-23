package dev.aethel.module.list.combat.aura;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Optional;
import java.util.function.Predicate;

public class RayTraceUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Entity getTargetedEntity(Entity target, float targetYaw, float targetPitch, double distance) {
        Entity viewerEntity = mc.getCameraEntity();
        if (viewerEntity == null || mc.world == null) return null;

        Vec3d startVector = viewerEntity.getEyePos();
        Vec3d directionVector = getVectorForRotation(targetPitch, targetYaw);
        Vec3d endVector = startVector.add(directionVector.multiply(distance));

        Box targetBoundingBox = target.getBoundingBox().expand(target.getTargetingMargin());
        EntityHitResult entityRayTraceResult = traceEntities(viewerEntity, startVector, endVector, targetBoundingBox,
                (entity) -> !entity.isSpectator(), distance);

        return entityRayTraceResult != null ? entityRayTraceResult.getEntity() : null;
    }

    public static EntityHitResult traceEntities(Entity shooter, Vec3d startVector, Vec3d endVector, Box boundingBox, Predicate<Entity> filter, double distance) {
        var world = shooter.getWorld();
        double closestDistance = distance;
        Entity closestEntity = null;
        Vec3d closestHitVector = null;

        for (Entity entity : world.getOtherEntities(shooter, boundingBox, filter)) {
            Box entityBoundingBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
            Optional<Vec3d> optional = entityBoundingBox.raycast(startVector, endVector);

            if (entityBoundingBox.contains(startVector) || optional.isPresent()) {
                double distanceToHit = optional.map(startVector::distanceTo).orElse(0.0D);
                if (distanceToHit < closestDistance || closestDistance == 0.0D) {
                    if (entity.getRootVehicle() != shooter.getRootVehicle()) {
                        closestEntity = entity;
                        closestHitVector = optional.orElse(startVector);
                        closestDistance = distanceToHit;
                    }
                }
            }
        }

        return closestEntity == null ? null : new EntityHitResult(closestEntity, closestHitVector);
    }

    public static HitResult calculateRayTrace(double distance, float yaw, float pitch, Entity entity, boolean ignoreBlocks) {
        Vec3d startVector = mc.player.getEyePos();
        Vec3d directionVector = getVectorForRotation(pitch, yaw);
        Vec3d endVector = startVector.add(directionVector.multiply(distance));

        HitResult blockResult = traceBlock(startVector, endVector, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE);
        double entityDistance = blockResult.getPos().squaredDistanceTo(startVector);

        Box entityBoundingBox = entity.getBoundingBox().expand(directionVector.x * distance, directionVector.y * distance, directionVector.z * distance).expand(1.0D);
        EntityHitResult entityRayTraceResult = ProjectileUtil.raycast(entity, startVector, endVector, entityBoundingBox, (x) -> !x.isSpectator() && x.isAlive(), distance);

        if (entityRayTraceResult != null && (ignoreBlocks || entityRayTraceResult.getPos().squaredDistanceTo(startVector) < entityDistance)) {
            return entityRayTraceResult;
        }

        return blockResult;
    }

    public static boolean rayTraceEntity(float yaw, float pitch, double distance, Entity entity) {
        Vec3d eyeVec = mc.player.getEyePos();
        Vec3d lookVec = getVectorForRotation(pitch, yaw);
        Vec3d endVec = eyeVec.add(lookVec.multiply(distance));

        Box entityBox = entity.getBoundingBox();
        return entityBox.contains(eyeVec) || entityBox.raycast(eyeVec, endVec).isPresent();
    }

    public static Vec3d getVectorForRotation(float pitch, float yaw) {
        float yawRadians = -yaw * ((float) Math.PI / 180) - (float) Math.PI;
        float pitchRadians = -pitch * ((float) Math.PI / 180);

        float cosYaw = MathHelper.cos(yawRadians);
        float sinYaw = MathHelper.sin(yawRadians);
        float cosPitch = -MathHelper.cos(pitchRadians);
        float sinPitch = MathHelper.sin(pitchRadians);

        return new Vec3d(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }

    public static HitResult traceBlock(Vec3d startVec, Vec3d endVec, RaycastContext.ShapeType blockMode, RaycastContext.FluidHandling fluidMode) {
        return mc.world.raycast(new RaycastContext(startVec, endVec, blockMode, fluidMode, mc.player));
    }

    public static boolean isViewEntity(LivingEntity target, float yaw, float pitch, float distance, boolean ignoreWalls) {
        Entity entity = mc.getCameraEntity();
        if (entity == null || mc.world == null) return false;

        Vec3d startVec = entity.getEyePos();
        Vec3d endVec = startVec.add(getVectorForRotation(pitch, yaw).multiply(distance));
        Box aabb = target.getBoundingBox();

        EntityHitResult result = ProjectileUtil.raycast(entity, startVec, endVec, aabb,
                (entityIn) -> !entityIn.isSpectator() && entityIn.isAlive() && entityIn == target,
                distance * distance);

        return result != null;
    }

    public static boolean canSeen(Vec3d point) {
        if (mc.world == null || mc.player == null) return false;
        Vec3d eyePos = mc.player.getEyePos();
        HitResult result = mc.world.raycast(new RaycastContext(eyePos, point,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS;
    }

    public static Vec3d getPoint(LivingEntity entity) {
        return entity.getPos().add(0, entity.getHeight() / 2, 0);
    }
}
