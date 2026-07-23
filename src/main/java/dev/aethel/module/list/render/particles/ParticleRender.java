package dev.aethel.module.list.render.particles;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.render.math.MathUtil;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;

public class ParticleRender implements IMinecraft {
    private float prevX, prevY, prevZ;
    private float x, y, z;
    private float motionX, motionY, motionZ;
    private int lifeTime;
    private int maxLife;
    private float prevSize = 0f;
    private float rotation, prevRotation = 0f;
    private float rotateSpeed = 20f, size;
    private int index;
    private Identifier identifier;
    private boolean dropPhysics, rotating;

    private float spawnDuration, dyingDuration;

    private final TimerUtil timerUtil = new TimerUtil();
    private double alphaValue = 0.0;
    private double alphaTarget = 0.0;
    private long alphaStartTime = 0;
    private long alphaDuration = 0;
    private boolean gravityFalls = false;

    private boolean trail;
    private float trailLength = 5f;
    private boolean dyingEffect;
    private final Deque<Vec3d> trailPoints = new ArrayDeque<>();

    public ParticleRender(float x, float y, float z, int lifeTime) {
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.x = x;
        this.y = y;
        this.z = z;
        this.maxLife = (int) MathUtil.random(Math.max(lifeTime / 2, 0), lifeTime);
        this.rotation = MathUtil.random(-180f, 180f);
    }

    public static String[] textures = new String[]{
            "Spark", "Star", "Heart", "Dollar", "Snowflake", "Glow", "Ball",
    };

    public static Identifier getTexture(String mode) {
        String path;
        if (mode.equals("Spark")) {
            int r = (int) MathUtil.random(1, 4.99);
            path = "textures/particles/spark_" + r + ".png";
        } else {
            path = "textures/particles/" + mode.toLowerCase() + ".png";
        }
        return Identifier.of("aethel", path);
    }

    public boolean update() {
        float gravity = gravityFalls ? (float) alphaValue * 0.3f : 1f;

        prevX = x;
        prevY = y;
        prevZ = z;

        x += motionX;
        y += motionY * gravity;
        z += motionZ;

        double speed = Math.sqrt((motionX * motionX + motionZ * motionZ));
        float halfSize = prevSize;

        if (posBlock(x, y - halfSize - 0.05f, z)) {
            motionY = -motionY / 1.1f;
            motionX /= 1.1f;
            motionZ /= 1.1f;
        } else {
            if (posBlock(x - (float) speed - halfSize, y, z - (float) speed - halfSize) ||
                    posBlock(x + (float) speed + halfSize, y, z + (float) speed + halfSize) ||
                    posBlock(x + (float) speed + halfSize, y, z - (float) speed - halfSize) ||
                    posBlock(x - (float) speed - halfSize, y, z + (float) speed + halfSize) ||
                    posBlock(x + (float) speed + halfSize, y, z) ||
                    posBlock(x - (float) speed - halfSize, y, z) ||
                    posBlock(x, y, z + (float) speed + halfSize) ||
                    posBlock(x, y, z - (float) speed - halfSize)) {
                motionX = -motionX;
                motionZ = -motionZ;
                maxLife--;
            } else if (dropPhysics) {
                motionY -= 0.02f;
            }
        }

        prevRotation = rotation;
        rotation -= (prevRotation > 0) ? -rotateSpeed : rotateSpeed;

        if (!gravityFalls) {
            float scale = 1.1f;
            motionX /= scale;
            motionY /= scale;
            motionZ /= scale;
        }

        if (trail) {
            trailPoints.addFirst(new Vec3d(x, y, z));
            while (trailPoints.size() > trailLength) trailPoints.removeLast();
        }

        return mc.player.getPos().distanceTo(new Vec3d(x, y, z)) >= 80 ||
                alphaValue <= 0.0 && timerUtil.hasReached((long) ((spawnDuration + dyingDuration + maxLife) * 50));
    }

    private float alphaPC() {
        return (float) alphaValue;
    }

    private int alpha() {
        return (int) (255 * alphaPC());
    }

    public void updateAlpha() {
        updateAnim();

        float alphaAnim = alphaPC();

        if (alphaAnim <= 0.0 && !timerUtil.hasReached((long) (spawnDuration * 50)))
            runAlpha(1.0, (long) (spawnDuration * 50));

        if (alphaAnim >= 1.0 && timerUtil.hasReached((long) ((spawnDuration + maxLife) * 50)))
            runAlpha(0.0, (long) (dyingDuration * 50));
    }

    private void runAlpha(double target, long duration) {
        alphaStartTime = System.currentTimeMillis();
        alphaTarget = target;
        alphaDuration = duration;
    }

    private void updateAnim() {
        if (alphaDuration <= 0) return;
        long elapsed = System.currentTimeMillis() - alphaStartTime;
        if (elapsed >= alphaDuration) {
            alphaValue = alphaTarget;
            alphaDuration = 0;
            return;
        }
        double progress = (double) elapsed / alphaDuration;
        double diff = alphaTarget - alphaValue;
        alphaValue += diff * Math.min(progress * 3, 1.0);
    }

    public void render(MatrixStack matrixStack) {
        if (!canSee(new Vec3d(x, y, z))) return;

        Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
        Camera camera = mc.gameRenderer.getCamera();
        Color primaryColor = setAlpha(gradientColor(index * 90), alpha());
        Vec3d interpolatedPos = interpolatePosition(prevX, prevY, prevZ, x, y, z);

        float halfSize = MathHelper.lerp((float) MathUtil.random(0, 1), prevSize, (size * alphaPC()));
        prevSize = halfSize;

        RenderSystem.setShaderTexture(0, identifier);
        matrixStack.translate(interpolatedPos.x, interpolatedPos.y, interpolatedPos.z);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        if (rotating) matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), prevRotation, rotation)));

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(matrix4f, halfSize, -halfSize, 0f).texture(0f, 1f).color(primaryColor.getRGB());
        bufferBuilder.vertex(matrix4f, -halfSize, -halfSize, 0f).texture(1f, 1f).color(primaryColor.getRGB());
        bufferBuilder.vertex(matrix4f, -halfSize, halfSize, 0f).texture(1f, 0f).color(primaryColor.getRGB());
        bufferBuilder.vertex(matrix4f, halfSize, halfSize, 0f).texture(0f, 0f).color(primaryColor.getRGB());
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public void renderTrail(MatrixStack matrixStack) {
        if (!trail || trailPoints.size() <= 1) return;
        if (!canSee(new Vec3d(x, y, z))) return;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(1.5f);
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        Matrix4f mat = matrixStack.peek().getPositionMatrix();
        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
        Color col = setAlpha(gradientColor(index * 90), alpha());

        double interpX = MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), prevX, x);
        double interpY = MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), prevY, y);
        double interpZ = MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), prevZ, z);

        Vec3d last = null;
        for (Vec3d p : trailPoints) {
            double smoothX = p.x + (interpX - p.x) * 0.05;
            double smoothY = p.y + (interpY - p.y) * 0.05;
            double smoothZ = p.z + (interpZ - p.z) * 0.05;
            Vec3d smooth = new Vec3d(smoothX, smoothY, smoothZ);

            if (last != null) {
                buf.vertex(mat, (float)(last.x - cam.x), (float)(last.y - cam.y), (float)(last.z - cam.z))
                        .color(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
                buf.vertex(mat, (float)(smooth.x - cam.x), (float)(smooth.y - cam.y), (float)(smooth.z - cam.z))
                        .color(col.getRed(), col.getGreen(), col.getBlue(), col.getAlpha());
            }
            last = smooth;
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private boolean posBlock(float x, float y, float z) {
        Block block = mc.world != null ? mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).getBlock() : null;
        return block != null &&
                !(block instanceof AirBlock) &&
                block != Blocks.WATER &&
                block != Blocks.LAVA &&
                block != Blocks.SEAGRASS &&
                block != Blocks.TALL_SEAGRASS &&
                block != Blocks.SHORT_GRASS &&
                block != Blocks.TALL_GRASS &&
                block != Blocks.FERN &&
                block != Blocks.DEAD_BUSH &&
                block != Blocks.VINE &&
                block != Blocks.SNOW &&
                block != Blocks.POPPY &&
                block != Blocks.DANDELION &&
                block != Blocks.BROWN_MUSHROOM &&
                block != Blocks.RED_MUSHROOM;
    }

    private Vec3d interpolatePosition(float prevX, float prevY, float prevZ, float currentX, float currentY, float currentZ) {
        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;

        double interpolatedX = MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), prevX, currentX) - cameraX;
        double interpolatedY = MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), prevY, currentY) - cameraY;
        double interpolatedZ = MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), prevZ, currentZ) - cameraZ;

        return new Vec3d(interpolatedX, interpolatedY, interpolatedZ);
    }

    private Color setAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), MathHelper.clamp(alpha, 0, 255));
    }

    private Color gradientColor(int index) {
        float hue = (index * 0.05f) % 1.0f;
        return Color.getHSBColor(hue, 0.8f, 1.0f);
    }

    private boolean canSee(Vec3d pos) {
        if (mc.player == null) return false;
        Vec3d eyePos = mc.player.getEyePos();
        return mc.world != null && mc.world.raycast(new net.minecraft.world.RaycastContext(
                eyePos, pos,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        )).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }

    // --- Inner TimerUtil ---

    private static class TimerUtil {
        private long lastMs = System.currentTimeMillis();

        public boolean hasReached(long delay) {
            return System.currentTimeMillis() - lastMs >= delay;
        }

        public void reset() {
            lastMs = System.currentTimeMillis();
        }
    }
}
