package dev.ethereal.api.utils.rotation.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public class UBoxPoints {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static double yOffset = 0.5;
    private static long lastUpdate = 0;
    private static Vec3d lastTargetPos = Vec3d.ZERO;


    private static List<Vec3d> pointQueue = new ArrayList<>();
    private static int currentPointIndex = 0;
    private static Entity lastTargetEntity = null;

    public static int clamp(int value, int min, int max) {
        return Math.min(max, Math.max(value, min));
    }

    public static float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }

    public static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(value, min));
    }

    public static int lerp(int a, int b, float f) {
        return a + (int) (f * (b - a));
    }

    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public static double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }

    public static Vec2f getVanillaRotate(Vec3d vec) {
        final Vec3d eyesPos = mc.player.getEyePos();
        final Vec3d rot = vec.add(-eyesPos.x, -eyesPos.y, -eyesPos.z);
        final double xzD = MathHelper.sqrt((float) (rot.x * rot.x + rot.z * rot.z));
        float yaw = (float) (Math.atan2(rot.z, rot.x) * 180.F / Math.PI - 90.F);
        float pitch = (float) Math.toDegrees(-Math.atan2(rot.y, xzD));
        return new Vec2f(yaw, pitch);
    }

    public static HitResult traceBlock(Vec3d startVec, Vec3d endVec, RaycastContext.ShapeType blockMode, RaycastContext.FluidHandling fluidMode) {
        return mc.world.raycast(new RaycastContext(startVec, endVec, blockMode, fluidMode, mc.player));
    }

    private static double getDistanceXZ(ClientPlayerEntity self, double x, double z) {
        double d0 = self.getX() - x, d1 = self.getZ() - z;
        return MathHelper.sqrt((float) (d0 * d0 + d1 * d1));
    }

    private static boolean seenOnce3(ClientPlayerEntity self, double x, double y, double z) {
        Vec3d vector3d1 = new Vec3d(x, y, z);
        return mc.world != null && traceBlock(self.getEyePos(), vector3d1, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE).getType() != HitResult.Type.BLOCK;
    }

    private static boolean seenOnceVec3d(ClientPlayerEntity self, Vec3d vec) {
        Vec3d vector3d = new Vec3d(self.getX(), self.getEyeY(), self.getZ());
        return mc.world != null && traceBlock(vector3d, vec, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE).getType() != HitResult.Type.BLOCK;
    }

    private static boolean localSeen(ClientPlayerEntity selfEntity, final Vec3d xyz, final float scale) {
        return scale == 0 ? seenOnce3(selfEntity, xyz.x, xyz.y, xyz.z) :
                seenOnce3(selfEntity, xyz.x, xyz.y, xyz.z) &&
                        seenOnce3(selfEntity, xyz.x, xyz.y + scale, xyz.z) &&
                        seenOnce3(selfEntity, xyz.x, xyz.y - scale, xyz.z) &&
                        seenOnce3(selfEntity, xyz.x + scale, xyz.y, xyz.z) &&
                        seenOnce3(selfEntity, xyz.x - scale, xyz.y, xyz.z) &&
                        seenOnce3(selfEntity, xyz.x, xyz.y, xyz.z + scale) &&
                        seenOnce3(selfEntity, xyz.x, xyz.y, xyz.z - scale);
    }

    private static double getDistanceToCoord(Entity entity, double x, double y, double z) {
        return Math.sqrt(entity.squaredDistanceTo(x, y, z));
    }


    public static List<Vec3d> entityBoxVec3dsAlternates(Entity entity) {
        return entityBoxVec3dsAlternate(entity, entity.getBoundingBox());
    }

    public static List<Vec3d> entityBoxVec3dsAlternate(Entity entity, Box aabb) {
        if (entity == null) return null;
        final List<Vec3d> vecs = new ArrayList<>();
        double offsetXYZ = .02F;
        int maxPointsCountXZ = 16, minPointsCountXZ = 10;
        int maxPointsCountY = 30, minPointsCountY = 15;
        double[] whh = new double[]{entity.getWidth() - offsetXYZ * 2D, entity.getHeight() - offsetXYZ * 2D, (entity.getHeight() - offsetXYZ * 2D) / 1.05D};
        double[] xyz = new double[]{entity.getX(), entity.getY(), entity.getZ()};
        double[] xyz1 = new double[]{xyz[0] + whh[0] / 2.D, xyz[1] + whh[1], xyz[2] + whh[0] / 2.D};
        double[] xyz2 = new double[]{xyz[0] - whh[0] / 2.D, xyz[1], xyz[2] - whh[0] / 2.D};
        if (aabb != null) {
            aabb = aabb.contract(offsetXYZ, offsetXYZ, offsetXYZ);
            whh = new double[]{aabb.maxX - aabb.minX, aabb.maxY - aabb.minY, (aabb.maxY - aabb.minY) / 1.05D};
            xyz = new double[]{aabb.minX + whh[0] / 2.D, aabb.minY, aabb.minZ + whh[0] / 2.D};
            xyz1 = new double[]{aabb.minX, aabb.minY, aabb.minZ};
            xyz2 = new double[]{aabb.maxX, aabb.maxY, aabb.maxZ};
        } else {
            xyz2 = new double[]{xyz[0] + whh[0] / 2.D, xyz[1] + whh[1], xyz[2] + whh[0] / 2.D};
            xyz1 = new double[]{xyz[0] - whh[0] / 2.D, xyz[1], xyz[2] - whh[0] / 2.D};
        }
        float sqrtWHH0CubeD2 = (float) Math.sqrt(whh[0] * whh[0] + whh[0] * whh[0] + whh[0] * whh[0]) / 2.F;
        final ClientPlayerEntity me = mc.player;
        if (me == null) return null;
        final float factorCount = (1.F - Math.min((float) getDistanceToCoord(me, xyz[0], xyz[1], xyz[2]) / 5.F, 1.F)) * Math.min((float) getDistanceToCoord(me, xyz[0], me.getY(), xyz[2]) / .6F, 1.F);
        final int pointsCountXZ = lerp(minPointsCountXZ, maxPointsCountXZ, factorCount);
        final int pointsCountY = lerp(minPointsCountY, maxPointsCountY, factorCount);
        float scaleSeenCheck = .0F;
        for (final Integer xsI : IntStream.range(0, pointsCountXZ).toArray()) {
            final boolean edgeX = xsI == 0 || xsI == pointsCountXZ - 1;
            final double xs = lerp(xyz1[0], xyz2[0], xsI / (float) (pointsCountXZ - 1));
            for (final Integer zsI : IntStream.range(0, pointsCountXZ).toArray()) {
                final boolean edgeZ = zsI == 0 || zsI == pointsCountXZ - 1;
                final double zs = lerp(xyz1[2], xyz2[2], zsI / (float) (pointsCountXZ - 1));
                for (final Integer ysI : IntStream.range(0, pointsCountY).toArray()) {
                    final boolean edgeY = ysI == 0 || ysI == pointsCountY - 1;
                    final double ys = lerp(xyz1[1], xyz2[1], ysI / (float) (pointsCountY - 1));
                    final Vec3d vec = new Vec3d(xs, ys, zs);
                    if (!edgeX && !edgeZ && !edgeY || me.getPos().distanceTo(vec.add(0.D, -me.getEyeHeight(me.getPose()), 0.D)) < sqrtWHH0CubeD2 || !localSeen(me, vec, scaleSeenCheck))
                        continue;
                    if (!vecs.add(vec)) break;
                }
            }
        }
        return vecs;
    }

    private static double getDistanceAtVec3dToVec3d(Vec3d first, Vec3d second) {
        final double xDiff, yDiff, zDiff;
        return Math.sqrt((xDiff = first.x - second.x) * xDiff + (yDiff = first.y - second.y) * yDiff + (zDiff = first.z - second.z) * zDiff);
    }

    public static Vec3d getBestVector3dOnEntityBox(Entity entity, boolean alwaysMultipoints) {
        if (entity == null) return mc.player.getEyePos();

        boolean targetMoved = entity.getPos().distanceTo(lastTargetPos) > 0.05;

        Box aabb = entity.getBoundingBox();
        double[] whh = new double[]{entity.getWidth(), entity.getHeight(), entity.getHeight() / 1.05F};
        double[] xyz = new double[]{entity.getX(), entity.getY(), entity.getZ()};
        if (aabb != null) {
            whh = new double[]{aabb.maxX - aabb.minX, aabb.maxY - aabb.minY, (aabb.maxY - aabb.minY) / 1.1F};
            xyz = new double[]{aabb.minX + whh[0] / 2.D, aabb.minY, aabb.minZ + whh[0] / 2.D};
        }
        double[] diffs = new double[]{mc.player.getY() - xyz[1], getDistanceXZ(mc.player, xyz[0], xyz[2])};

        if (lastTargetEntity != entity) {
            yOffset = Math.random() * 0.7;
            lastTargetPos = Vec3d.ZERO; // форс-ресет
        }


        final double pitchPointHeight = clamp(whh[2] * yOffset, 0, whh[2]);
        Vec3d defaultVec = new Vec3d(xyz[0], xyz[1] + pitchPointHeight, xyz[2]);
        if (!alwaysMultipoints && !seenOnceVec3d(mc.player, defaultVec))
            defaultVec = defaultVec.add(0.D, -pitchPointHeight / 2.D, 0.D);
        if (whh[1] <= 1D || !alwaysMultipoints && seenOnceVec3d(mc.player, defaultVec)) {
            return defaultVec;
        } else {
            long currentTime = System.currentTimeMillis();
            boolean shouldUpdateQueue = lastTargetEntity != entity
                    || pointQueue.isEmpty()
                    || currentPointIndex >= pointQueue.size()
                    || targetMoved;  // ← ключевой фикс: сбрасываем если цель двинулась

            if (shouldUpdateQueue) {
                final List<Vec3d> normalVecs = entityBoxVec3dsAlternate(entity, aabb);
                if (normalVecs != null && normalVecs.size() > 0) {
                    double maxY = xyz[1] + whh[1] * 0.8;
                    pointQueue = new ArrayList<>();
                    for (Vec3d vec : normalVecs) {
                        if (vec.y <= maxY) {
                            pointQueue.add(vec);
                        }
                    }

                    if (pointQueue.isEmpty()) {
                        pointQueue = new ArrayList<>(normalVecs);
                    }

                    java.util.Collections.shuffle(pointQueue);
                    currentPointIndex = 0;
                    lastTargetEntity = entity;
                } else {
                    return defaultVec;
                }
            }

            Vec3d result = pointQueue.get(currentPointIndex);
            currentPointIndex++;

            if (currentPointIndex >= pointQueue.size()) {
                currentPointIndex = 0;
                java.util.Collections.shuffle(pointQueue);
                lastTargetPos = entity.getPos();
            }

            return result;
        }
    }

    public static Vec3d getBestVector3dOnEntityBox(Entity entity) {
        return getBestVector3dOnEntityBox(entity, getDistanceToCoord(mc.player, entity.getX(), mc.player.getY(), entity.getZ()) > entity.getWidth() * 1.37F);
    }

    public static Vec3d getRandomVector3dOnEntityBox(Entity entity) {
        if (entity == null) return mc.player.getEyePos();

        Box aabb = entity.getBoundingBox();
        final List<Vec3d> points = entityBoxVec3dsAlternate(entity, aabb);

        if (points == null || points.isEmpty()) {
            return getBestVector3dOnEntityBox(entity, true);
        }

        Vec3d eyePos = mc.player.getEyePos();
        points.sort(Comparator.comparingDouble(v -> v.squaredDistanceTo(eyePos)));

        int pickRange = Math.max(1, (int)(points.size() * 0.4));
        int idx = (int)(Math.random() * pickRange);
        return points.get(idx);
    }

    public static Vec2f getBestRotateVector2fOnEntityBox(Entity entity, boolean alwaysMultipoints) {
        Vec3d vec = UBoxPoints.getBestVector3dOnEntityBox(entity, alwaysMultipoints);
        return UBoxPoints.getVanillaRotate(vec);
    }

    public static Vec2f getBestRotateVector2fOnEntityBox(Entity entity) {
        return getBestRotateVector2fOnEntityBox(entity, true);
    }
}
