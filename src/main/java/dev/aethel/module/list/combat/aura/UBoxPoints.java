package dev.aethel.module.list.combat.aura;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class UBoxPoints {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

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
        return mc.world.raycast(new RaycastContext(
                startVec,
                endVec,
                blockMode,
                fluidMode,
                mc.player)
        );
    }

    private static double getDistanceXZ(double x, double z) {
        double d0 = mc.player.getX() - x, d1 = mc.player.getZ() - z;
        return MathHelper.sqrt((float) (d0 * d0 + d1 * d1));
    }

    private static boolean seenOnce(Vec3d pos) {
        return mc.world != null &&
                traceBlock(mc.player.getEyePos(), pos,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE).getType() != HitResult.Type.BLOCK;
    }

    private static boolean localSeen(final Vec3d xyz, final float scale) {
        return scale == 0 ? seenOnce(xyz) :
                seenOnce(xyz) &&
                        seenOnce(xyz.add(0, scale, 0)) &&
                        seenOnce(xyz.add(0, -scale, 0)) &&
                        seenOnce(xyz.add(scale, 0, 0)) &&
                        seenOnce(xyz.add(-scale, 0, 0)) &&
                        seenOnce(xyz.add(0, 0, scale)) &&
                        seenOnce(xyz.add(0, 0, -scale));
    }

    public static List<Vec3d> entityBoxVec3dsAlternates(Entity entity) {
        return entityBoxVec3dsAlternate(entity, entity.getBoundingBox());
    }

    public static List<Vec3d> entityBoxVec3dsAlternate(Entity entity, Box aabb) {
        if (entity == null) return null;
        final List<Vec3d> vecs = new ArrayList<>();
        double offsetXYZ = .02F;
        int maxPointsCountXZ = 14, minPointsCountXZ = 5;
        int maxPointsCountY = 27, minPointsCountY = 9;
        double[] whh = new double[]{entity.getWidth() - offsetXYZ * 2D, entity.getHeight() - offsetXYZ * 2D, (entity.getHeight() - offsetXYZ * 2D) / 1.05D};
        double[] xyz = new double[]{entity.getX(), entity.getY(), entity.getZ()};
        double[] xyz1, xyz2;

        if (aabb != null) {
            aabb = aabb.expand(-offsetXYZ);
            whh = new double[]{aabb.maxX - aabb.minX, aabb.maxY - aabb.minY, (aabb.maxY - aabb.minY) / 1.05D};
            xyz = new double[]{aabb.minX + whh[0] / 2.D, aabb.minY, aabb.minZ + whh[0] / 2.D};
            xyz1 = new double[]{aabb.minX, aabb.minY, aabb.minZ};
            xyz2 = new double[]{aabb.maxX, aabb.maxY, aabb.maxZ};
        } else {
            xyz2 = new double[]{xyz[0] + whh[0] / 2.D, xyz[1] + whh[1], xyz[2] + whh[0] / 2.D};
            xyz1 = new double[]{xyz[0] - whh[0] / 2.D, xyz[1], xyz[2] - whh[0] / 2.D};
        }

        final float factorCount = (1.F - Math.min((float) mc.player.getPos().distanceTo(new Vec3d(xyz[0], xyz[1], xyz[2])) / 5.F, 1.F))
                * Math.min((float) mc.player.getPos().distanceTo(new Vec3d(xyz[0], mc.player.getY(), xyz[2])) / .6F, 1.F);
        final int pointsCountXZ = lerp(minPointsCountXZ, maxPointsCountXZ, factorCount);
        final int pointsCountY = lerp(minPointsCountY, maxPointsCountY, factorCount);
        float scaleSeenCheck = .0F;

        for (int xsI = 0; xsI < pointsCountXZ; xsI++) {
            final boolean edgeX = xsI == 0 || xsI == pointsCountXZ - 1;
            final double xs = lerp(xyz1[0], xyz2[0], xsI / (float) (pointsCountXZ - 1));
            for (int zsI = 0; zsI < pointsCountXZ; zsI++) {
                final boolean edgeZ = zsI == 0 || zsI == pointsCountXZ - 1;
                final double zs = lerp(xyz1[2], xyz2[2], zsI / (float) (pointsCountXZ - 1));
                for (int ysI = 0; ysI < pointsCountY; ysI++) {
                    final boolean edgeY = ysI == 0 || ysI == pointsCountY - 1;
                    final double ys = lerp(xyz1[1], xyz2[1], ysI / (float) (pointsCountY - 1));
                    final Vec3d vec = new Vec3d(xs, ys, zs);
                    float sqrtWHH0CubeD2 = (float) Math.sqrt(whh[0] * whh[0] + whh[0] * whh[0] + whh[0] * whh[0]) / 2.F;
                    if (!edgeX && !edgeZ && !edgeY
                            || mc.player.getPos().distanceTo(vec.add(0, -mc.player.getEyeHeight(mc.player.getPose()), 0)) < sqrtWHH0CubeD2
                            || !localSeen(vec, scaleSeenCheck))
                        continue;
                    vecs.add(vec);
                }
            }
        }
        return vecs;
    }

    public static Vec3d getBestVector3dOnEntityBox(Entity entity, boolean alwaysMultipoints) {
        if (entity == null) return mc.player.getEyePos();
        Box aabb = entity.getBoundingBox();
        double[] whh = new double[]{entity.getWidth(), entity.getHeight(), entity.getHeight() / 1.05F};
        double[] xyz = new double[]{entity.getX(), entity.getY(), entity.getZ()};

        if (aabb != null) {
            whh = new double[]{aabb.maxX - aabb.minX, aabb.maxY - aabb.minY, (aabb.maxY - aabb.minY) / 1.1F};
            xyz = new double[]{aabb.minX + whh[0] / 2.D, aabb.minY, aabb.minZ + whh[0] / 2.D};
        }

        double[] diffs = new double[]{mc.player.getY() - xyz[1], getDistanceXZ(xyz[0], xyz[2])};
        double ddtn = clamp((diffs[1] - whh[0] / 2.F) / (5.D + whh[0] / 2.D), 0.1D, .95D);
        double pca = clamp(ddtn * ddtn, 0.D, 1.D);
        final double pitchPointHeight = clamp((whh[2] / 2.D * pca + (whh[2] / 2.D) * (clamp(diffs[0] + pca, 0.D, 1.D))), 0, whh[2]);
        Vec3d defaultVec = new Vec3d(xyz[0], xyz[1] + pitchPointHeight, xyz[2]);

        return defaultVec;
    }

    public static Vec3d getBestVector3dOnEntityBox(Entity entity) {
        return getBestVector3dOnEntityBox(entity, mc.player.getPos().distanceTo(new Vec3d(entity.getX(), mc.player.getY(), entity.getZ())) > entity.getWidth() * 1.37F);
    }

    public static Vec2f getBestRotateVector2fOnEntityBox(Entity entity, boolean alwaysMultipoints) {
        Vec3d vec = UBoxPoints.getBestVector3dOnEntityBox(entity, alwaysMultipoints);
        return UBoxPoints.getVanillaRotate(vec);
    }

    public static Vec2f getBestRotateVector2fOnEntityBox(Entity entity) {
        return getBestRotateVector2fOnEntityBox(entity, true);
    }
}
