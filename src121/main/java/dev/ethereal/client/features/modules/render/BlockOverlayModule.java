package dev.ethereal.client.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.system.files.FileUtil;
import dev.ethereal.api.utils.color.UIColors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "BlockOverlay", category = Category.RENDER)
public class BlockOverlayModule extends Module {
    @Getter private static final BlockOverlayModule instance = new BlockOverlayModule();

    @AllArgsConstructor
    private enum ShaderMode implements ModeSetting.NamedChoice {
        CLASSIC("Classic"),
        NEBULA("Nebula"),
        COSMIC("Cosmic");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }

    private static final class BlockOverlayShaderState {
        private final ShaderProgramKey key;
        private ShaderProgram lastShader;
        private GlUniform time;
        private GlUniform mouse;
        private GlUniform resolution;
        private GlUniform alpha;
        private GlUniform mode;

        private BlockOverlayShaderState(ShaderProgramKey key) {
            this.key = key;
        }

        private void use() {
            ShaderProgram shader = RenderSystem.setShader(key);
            if (shader != lastShader) {
                time = shader.getUniform("Time");
                mouse = shader.getUniform("Mouse");
                resolution = shader.getUniform("Resolution");
                alpha = shader.getUniform("OverlayAlpha");
                mode = shader.getUniform("Mode");
                lastShader = shader;
            }
        }
    }

    private static final long MAX_SMOOTH_STEP_MS = 75L;
    private static final float POSITION_LERP_PER_SEC = 22.0f;
    private static final float FADE_IN_PER_SEC = 10.5f;
    private static final float FADE_OUT_PER_SEC = 8.5f;
    private static final float MIN_VISIBLE_ALPHA = 0.015f;
    private static final double OVERLAY_EXPAND = 0.0012;
    private static final float OUTLINE_BASE_WIDTH = 1.9f;
    private static final ShaderProgramKey BLOCK_OVERLAY_SHADER_KEY =
            new ShaderProgramKey(FileUtil.getShader("block_overlay"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
    private static final BlockOverlayShaderState BLOCK_SHADER_STATE = new BlockOverlayShaderState(BLOCK_OVERLAY_SHADER_KEY);
    private static boolean lineWidthRangeKnown = false;
    private static float minLineWidth = 1.0f;
    private static float maxLineWidth = 1.0f;

    private final BooleanSetting shader = new BooleanSetting("Shader").value(true);
    private final ModeSetting shaderMode = new ModeSetting("Shader Mode")
            .value(ShaderMode.CLASSIC)
            .values(ShaderMode.values())
            .setVisible(shader::getValue);
    private final SliderSetting fillAlpha = new SliderSetting("Fill Alpha").value(150f).range(0f, 255f).step(5f);
    private final SliderSetting lineAlpha = new SliderSetting("Line Alpha").value(0f).range(0f, 255f).step(5f);

    private Vec3d smoothedOrigin;
    private long lastSmoothUpdateMs = System.currentTimeMillis();
    private long lastFadeUpdateMs = System.currentTimeMillis();
    private float overlayVisibility = 0.0f;
    private final List<Box> cachedLocalBoxes = new ArrayList<>();

    public BlockOverlayModule() {
        addSettings(shader, shaderMode, fillAlpha, lineAlpha);
    }

    @Override
    public void onDisable() {
        smoothedOrigin = null;
        overlayVisibility = 0.0f;
        cachedLocalBoxes.clear();
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        boolean hasTarget = false;
        HitResult hitResult = mc.crosshairTarget;

        if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            BlockPos blockPos = blockHitResult.getBlockPos();

            Vec3d playerPos = mc.player.getLerpedPos(event.partialTicks());
            Vec3d blockCenter = Vec3d.ofCenter(blockPos);
            double distance = playerPos.distanceTo(blockCenter);

            if (distance <= 100.0f) {
                hasTarget = true;

                Vec3d targetOrigin = Vec3d.of(blockPos);
                updateSmoothedOrigin(targetOrigin);

                VoxelShape shape = mc.world.getBlockState(blockPos).getOutlineShape(mc.world, blockPos);
                cachedLocalBoxes.clear();
                if (shape.isEmpty()) {
                    cachedLocalBoxes.add(new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).expand(OVERLAY_EXPAND));
                } else {
                    shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) ->
                            cachedLocalBoxes.add(new Box(minX, minY, minZ, maxX, maxY, maxZ).expand(OVERLAY_EXPAND)));
                }
            }
        }

        updateVisibility(hasTarget);
        if (overlayVisibility <= MIN_VISIBLE_ALPHA || smoothedOrigin == null || cachedLocalBoxes.isEmpty()) {
            if (!hasTarget && overlayVisibility <= MIN_VISIBLE_ALPHA) {
                smoothedOrigin = null;
                cachedLocalBoxes.clear();
            }
            return;
        }

        Color overlayColor = UIColors.primary();
        Color overlayColorSecondary = UIColors.secondary();
        for (Box localBox : cachedLocalBoxes) {
            Box smoothedBox = localBox.offset(smoothedOrigin.x, smoothedOrigin.y, smoothedOrigin.z);
            renderBlockOverlayDirect(event.matrixStack(), smoothedBox, overlayColor, overlayColorSecondary, overlayVisibility);
        }
    }

    private void updateSmoothedOrigin(Vec3d targetOrigin) {
        long now = System.currentTimeMillis();
        long elapsedMs = now - lastSmoothUpdateMs;
        if (elapsedMs <= 0L) {
            elapsedMs = 16L;
        }
        elapsedMs = Math.min(elapsedMs, MAX_SMOOTH_STEP_MS);
        lastSmoothUpdateMs = now;

        if (smoothedOrigin == null) {
            smoothedOrigin = targetOrigin;
            return;
        }

        float deltaSeconds = elapsedMs / 1000f;
        double distance = smoothedOrigin.distanceTo(targetOrigin);
        float speed = POSITION_LERP_PER_SEC;
        if (distance > 0.75) {
            speed *= 1.35f;
        }
        if (distance > 1.35) {
            speed *= 1.6f;
        }
        float factor = Math.min(1.0f, speed * deltaSeconds);
        smoothedOrigin = smoothedOrigin.lerp(targetOrigin, factor);
    }

    private void updateVisibility(boolean shouldShow) {
        long now = System.currentTimeMillis();
        long elapsedMs = now - lastFadeUpdateMs;
        if (elapsedMs <= 0L) {
            elapsedMs = 16L;
        }
        elapsedMs = Math.min(elapsedMs, MAX_SMOOTH_STEP_MS);
        lastFadeUpdateMs = now;

        float step = elapsedMs / 1000f * (shouldShow ? FADE_IN_PER_SEC : FADE_OUT_PER_SEC);
        float target = shouldShow ? 1.0f : 0.0f;
        overlayVisibility = approach(overlayVisibility, target, step);
    }

    private float approach(float current, float target, float step) {
        if (current < target) {
            return Math.min(target, current + step);
        }
        return Math.max(target, current - step);
    }

    private void renderBlockOverlayDirect(MatrixStack matrices, Box box, Color overlayColor, Color overlayColorSecondary, float alphaScale) {
        int scaledFillAlpha = scaleAlpha(fillAlpha.getValue().intValue(), alphaScale);
        int scaledOutlineAlpha = scaleAlpha(lineAlpha.getValue().intValue(), alphaScale);
        if (scaledFillAlpha <= 0 && scaledOutlineAlpha <= 0) {
            return;
        }

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Color fillColor = new Color(overlayColor.getRed(), overlayColor.getGreen(), overlayColor.getBlue(), scaledFillAlpha);
        Color fillColorSecondary = new Color(
                overlayColorSecondary.getRed(),
                overlayColorSecondary.getGreen(),
                overlayColorSecondary.getBlue(),
                scaledFillAlpha
        );
        renderBoxFillGradient(matrices, box, fillColor, fillColorSecondary);

        Color outlineColor = new Color(overlayColor.getRed(), overlayColor.getGreen(), overlayColor.getBlue(), scaledOutlineAlpha);
        Color outlineColorSecondary = new Color(
                overlayColorSecondary.getRed(),
                overlayColorSecondary.getGreen(),
                overlayColorSecondary.getBlue(),
                scaledOutlineAlpha
        );
        Color glowOutline = new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), Math.max(8, outlineColor.getAlpha() / 3));
        Color glowOutlineSecondary = new Color(
                outlineColorSecondary.getRed(),
                outlineColorSecondary.getGreen(),
                outlineColorSecondary.getBlue(),
                Math.max(8, outlineColorSecondary.getAlpha() / 3)
        );

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        renderBoxOutlineGradient(matrices, box, glowOutline, glowOutlineSecondary, 1.45f);
        renderBoxOutlineGradient(matrices, box, outlineColor, outlineColorSecondary, 1.0f);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void updateBlockOverlayUniforms(float overlayAlpha) {
        BlockOverlayShaderState shaderState = BLOCK_SHADER_STATE;
        shaderState.use();

        if (shaderState.time != null) {
            shaderState.time.set((System.currentTimeMillis() % 1_000_000L) / 1000.0f);
        }

        float resolutionX = Math.max(1.0f, (float) mc.getWindow().getFramebufferWidth());
        float resolutionY = Math.max(1.0f, (float) mc.getWindow().getFramebufferHeight());
        if (shaderState.resolution != null) {
            shaderState.resolution.set(resolutionX, resolutionY);
        }

        double width = Math.max(1.0, mc.getWindow().getWidth());
        double height = Math.max(1.0, mc.getWindow().getHeight());
        float mouseX = (float) MathHelper.clamp(mc.mouse.getX() / width, 0.0, 1.0);
        float mouseY = (float) MathHelper.clamp(1.0 - (mc.mouse.getY() / height), 0.0, 1.0);
        if (shaderState.mouse != null) {
            shaderState.mouse.set(mouseX, mouseY);
        }

        if (shaderState.alpha != null) {
            shaderState.alpha.set(MathHelper.clamp(overlayAlpha, 0.0f, 1.0f));
        }
        if (shaderState.mode != null) {
            shaderState.mode.set((float) getModeOrdinal());
        }
    }

    private int scaleAlpha(int alphaValue, float scale) {
        if (alphaValue <= 0 || scale <= 0.0f) {
            return 0;
        }
        return MathHelper.clamp(Math.round(alphaValue * scale), 0, 255);
    }

    private void renderBoxFillGradient(MatrixStack matrices, Box box, Color startColor, Color endColor) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();

        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);

        float r1 = startColor.getRed() / 255.0f;
        float g1 = startColor.getGreen() / 255.0f;
        float b1 = startColor.getBlue() / 255.0f;
        float a1 = startColor.getAlpha() / 255.0f;

        float r2 = endColor.getRed() / 255.0f;
        float g2 = endColor.getGreen() / 255.0f;
        float b2 = endColor.getBlue() / 255.0f;
        float a2 = endColor.getAlpha() / 255.0f;

        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);

        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);

        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);

        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);

        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);

        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r1, g1, b1, a1);

        if (shader.getValue()) {
            updateBlockOverlayUniforms((a1 + a2) * 0.5f);
        } else {
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void renderBoxOutlineGradient(MatrixStack matrices, Box box, Color startColor, Color endColor, float widthMultiplier) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();

        Vec3d boxCenter = box.getCenter();
        double distance = camera.distanceTo(boxCenter);
        float adjustedLineWidth = getOutlineLineWidth(distance) * widthMultiplier;

        RenderSystem.lineWidth(clampLineWidth(adjustedLineWidth));

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float minX = (float) (box.minX - camera.x);
        float minY = (float) (box.minY - camera.y);
        float minZ = (float) (box.minZ - camera.z);
        float maxX = (float) (box.maxX - camera.x);
        float maxY = (float) (box.maxY - camera.y);
        float maxZ = (float) (box.maxZ - camera.z);

        float r1 = startColor.getRed() / 255.0f;
        float g1 = startColor.getGreen() / 255.0f;
        float b1 = startColor.getBlue() / 255.0f;
        float a1 = startColor.getAlpha() / 255.0f;

        float r2 = endColor.getRed() / 255.0f;
        float g2 = endColor.getGreen() / 255.0f;
        float b2 = endColor.getBlue() / 255.0f;
        float a2 = endColor.getAlpha() / 255.0f;

        buffer.vertex(matrix, minX, minY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, minY, minZ).color(r1, g1, b1, a1);

        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);

        buffer.vertex(matrix, minX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, minZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, minX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, minX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, maxZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, maxZ).color(r1, g1, b1, a1);
        buffer.vertex(matrix, maxX, minY, minZ).color(r2, g2, b2, a2);
        buffer.vertex(matrix, maxX, maxY, minZ).color(r1, g1, b1, a1);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.lineWidth(1.0f);
    }

    private int getModeOrdinal() {
        if (shaderMode.is(ShaderMode.NEBULA)) {
            return 1;
        }
        if (shaderMode.is(ShaderMode.COSMIC)) {
            return 2;
        }
        return 0;
    }

    private float getOutlineLineWidth(double distance) {
        float nearBonus = (float) (2.8 / Math.max(2.0, distance + 2.0));
        return OUTLINE_BASE_WIDTH + nearBonus;
    }

    private static float clampLineWidth(float width) {
        if (!lineWidthRangeKnown) {
            FloatBuffer buffer = BufferUtils.createFloatBuffer(2);
            GL11.glGetFloatv(GL11.GL_LINE_WIDTH_RANGE, buffer);
            minLineWidth = buffer.get(0);
            maxLineWidth = buffer.get(1);
            lineWidthRangeKnown = true;
        }
        if (!Float.isFinite(width)) {
            return minLineWidth;
        }
        if (width < minLineWidth) {
            return minLineWidth;
        }
        if (width > maxLineWidth) {
            return maxLineWidth;
        }
        return width;
    }
}
