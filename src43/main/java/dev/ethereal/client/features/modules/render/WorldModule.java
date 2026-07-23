package dev.ethereal.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ColorSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.system.files.FileUtil;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.math.TimerUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "World", category = Category.RENDER)
public class WorldModule extends Module {
    @Getter private static final WorldModule instance = new WorldModule();

    private static final int MAX_PARTICLES = 100;
    private static final Vec3d[] CUBE_POINTS = {
            new Vec3d(-0.5, -0.5, -0.5), new Vec3d(0.5, -0.5, -0.5),
            new Vec3d(0.5, -0.5, 0.5), new Vec3d(-0.5, -0.5, 0.5),
            new Vec3d(-0.5, 0.5, -0.5), new Vec3d(0.5, 0.5, -0.5),
            new Vec3d(0.5, 0.5, 0.5), new Vec3d(-0.5, 0.5, 0.5)
    };
    private static final int[][] CUBE_EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}, {0, 6}, {1, 7}, {2, 4}, {3, 5}
    };
    private static final Vec3d[] PYRAMID_POINTS = {
            new Vec3d(-0.55, -0.45, -0.55), new Vec3d(0.55, -0.45, -0.55),
            new Vec3d(0.55, -0.45, 0.55), new Vec3d(-0.55, -0.45, 0.55),
            new Vec3d(0.0, 0.65, 0.0)
    };
    private static final int[][] PYRAMID_EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0}, {0, 4}, {1, 4}, {2, 4}, {3, 4}, {0, 2}, {1, 3}
    };
    private static final Vec3d[] PRISM_POINTS = {
            new Vec3d(0.0, 0.55, -0.45), new Vec3d(-0.55, -0.45, -0.45), new Vec3d(0.55, -0.45, -0.45),
            new Vec3d(0.0, 0.55, 0.45), new Vec3d(-0.55, -0.45, 0.45), new Vec3d(0.55, -0.45, 0.45)
    };
    private static final int[][] PRISM_EDGES = {
            {0, 1}, {1, 2}, {2, 0}, {3, 4}, {4, 5}, {5, 3}, {0, 3}, {1, 4}, {2, 5}, {0, 4}, {2, 3}
    };
    private static final Vec3d[] DIAMOND_POINTS = {
            new Vec3d(0.0, 0.65, 0.0), new Vec3d(-0.55, 0.0, 0.0), new Vec3d(0.0, 0.0, -0.55),
            new Vec3d(0.55, 0.0, 0.0), new Vec3d(0.0, 0.0, 0.55), new Vec3d(0.0, -0.65, 0.0)
    };
    private static final int[][] DIAMOND_EDGES = {
            {0, 1}, {0, 2}, {0, 3}, {0, 4}, {5, 1}, {5, 2}, {5, 3}, {5, 4}, {1, 2}, {2, 3}, {3, 4}, {4, 1}
    };

    private final List<Particle> particles = new ArrayList<>();
    private final ColorSetting color = new ColorSetting("Color").value(new Color(255, 255, 255));
    private final SliderSetting amount = new SliderSetting("Amount").value(100f).range(20f, 180f).step(5f);

    public WorldModule() {
        addSettings(color, amount);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(this::onRender));

        addEvents(updateEvent, render3DEvent);
    }

    private void onRender(Render3DEvent.Render3DEventData event) {
        if (mc.player == null || mc.world == null) return;

        for (Particle particle : particles) {
            particle.updateAlpha();
        }

        MatrixStack ms = event.matrixStack();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();

        renderBloom(event, ms, camera, cameraPos);
        renderShapes(event, ms, cameraPos);
    }

    private void renderBloom(Render3DEvent.Render3DEventData event, MatrixStack ms, Camera camera, Vec3d cameraPos) {
        ms.push();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, FileUtil.getImage("target/bloom"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (Particle particle : particles) {
            Vec3d pos = getInterpolatedPos(particle.prev, particle.pos, event.partialTicks());
            float bloomSize = 4.2f * particle.size * particle.visualScale();

            ms.push();
            ms.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
            ms.multiply(camera.getRotation());
            drawImage(ms, builder, -bloomSize / 2.0f, -bloomSize / 2.0f, bloomSize, bloomSize,
                    withAlpha(color.getValue(), particle.alpha * 0.34f));
            ms.pop();
        }

        BuiltBuffer builtBloom = builder.endNullable();
        if (builtBloom != null) {
            BufferRenderer.drawWithGlobalProgram(builtBloom);
        }

        RenderSystem.depthMask(true);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        ms.pop();
    }

    private void renderShapes(Render3DEvent.Render3DEventData event, MatrixStack ms, Vec3d cameraPos) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder linesBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (Particle particle : particles) {
            Vec3d pos = getInterpolatedPos(particle.prev, particle.pos, event.partialTicks());
            Vec3d rot = getInterpolatedPos(particle.prevRot, particle.rotate, event.partialTicks());
            float scale = particle.size * particle.visualScale();

            ms.push();
            ms.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
            ms.multiply(new Quaternionf().rotationXYZ((float) rot.x, (float) rot.y, (float) rot.z));
            ms.scale(scale, scale, scale);

            int outlineColor = withAlpha(color.getValue(), particle.alpha * 0.86f);
            int innerColor = withAlpha(color.getValue(), particle.alpha * 0.38f);
            renderShape(ms, linesBuffer, particle.shape, outlineColor, innerColor);

            ms.pop();
        }

        BuiltBuffer builtLinesBuffer = linesBuffer.endNullable();
        if (builtLinesBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtLinesBuffer);
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void onTick() {
        if (mc.player == null || mc.world == null) return;

        particles.removeIf(particle -> particle.alpha <= 0.0f && particle.timer.finished(particle.liveTicks));

        for (Particle particle : particles) {
            particle.tick();
        }

        int targetAmount = Math.min(MAX_PARTICLES, amount.getValue().intValue());
        while (particles.size() < targetAmount) {
            particles.add(new Particle(
                    mc.player.getPos().add(
                            MathUtil.random(-20.0, 20.0),
                            MathUtil.random(0.0, 5.0),
                            MathUtil.random(-20.0, 20.0)
                    ),
                    Vec3d.ZERO,
                    new Vec3d(
                            MathUtil.random(-1.0, 1.0),
                            MathUtil.random(0.0, 2.0),
                            MathUtil.random(-1.0, 1.0)
                    ),
                    new Vec3d(
                            MathUtil.random(-1.0, 1.0),
                            MathUtil.random(-1.0, 1.0),
                            MathUtil.random(-1.0, 1.0)
                    ),
                    (long) MathUtil.random(1800.0, 5200.0),
                    MathUtil.random(0.12f, 0.34f),
                    Shape.random()
            ));
        }
    }

    private Vec3d getInterpolatedPos(Vec3d prev, Vec3d current, float delta) {
        return new Vec3d(
                prev.x + (current.x - prev.x) * delta,
                prev.y + (current.y - prev.y) * delta,
                prev.z + (current.z - prev.z) * delta
        );
    }

    private void drawImage(MatrixStack ms, BufferBuilder builder, float x, float y, float w, float h, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        Matrix4f m = ms.peek().getPositionMatrix();

        builder.vertex(m, x, y + h, 0).texture(0, 1).color(r, g, b, a);
        builder.vertex(m, x + w, y + h, 0).texture(1, 1).color(r, g, b, a);
        builder.vertex(m, x + w, y, 0).texture(1, 0).color(r, g, b, a);
        builder.vertex(m, x, y, 0).texture(0, 0).color(r, g, b, a);
    }

    private void renderShape(MatrixStack ms, BufferBuilder builder, Shape shape, int outlineColor, int innerColor) {
        Vec3d[] points = switch (shape) {
            case CUBE -> CUBE_POINTS;
            case PYRAMID -> PYRAMID_POINTS;
            case TRIANGLE_PRISM -> PRISM_POINTS;
            case DIAMOND -> DIAMOND_POINTS;
        };
        int[][] edges = switch (shape) {
            case CUBE -> CUBE_EDGES;
            case PYRAMID -> PYRAMID_EDGES;
            case TRIANGLE_PRISM -> PRISM_EDGES;
            case DIAMOND -> DIAMOND_EDGES;
        };

        for (int i = 0; i < edges.length; i++) {
            drawLine(ms, builder, points[edges[i][0]], points[edges[i][1]], i < edges.length - 4 ? outlineColor : innerColor);
        }
    }

    private void drawLine(MatrixStack ms, BufferBuilder builder, Vec3d from, Vec3d to, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        Matrix4f m = ms.peek().getPositionMatrix();

        builder.vertex(m, (float) from.x, (float) from.y, (float) from.z).color(r, g, b, a);
        builder.vertex(m, (float) to.x, (float) to.y, (float) to.z).color(r, g, b, a);
    }

    private int withAlpha(Color baseColor, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | (baseColor.getRed() << 16) | (baseColor.getGreen() << 8) | baseColor.getBlue();
    }

    private enum Shape {
        CUBE,
        PYRAMID,
        TRIANGLE_PRISM,
        DIAMOND;

        private static Shape random() {
            Shape[] values = values();
            return values[MathUtil.random(0, values.length - 1)];
        }
    }

    private static class Particle {
        Vec3d prev;
        Vec3d prevRot;
        Vec3d pos;
        Vec3d rotate;
        Vec3d motion;
        Vec3d rotateMotion;
        final long liveTicks;
        final long fadeInTime = 420;
        final long fadeOutTime = 950;
        final float size;
        final Shape shape;
        final TimerUtil timer = new TimerUtil();
        float alpha = 0.0f;
        float fadeProgress = 0.0f;

        Particle(Vec3d pos, Vec3d rotate, Vec3d motion, Vec3d rotateMotion, long liveTicks, float size, Shape shape) {
            this.pos = pos;
            this.rotate = rotate;
            this.motion = motion.multiply(0.04);
            this.rotateMotion = rotateMotion.multiply(0.04);
            this.liveTicks = liveTicks;
            this.size = size;
            this.shape = shape;
            this.prevRot = rotate;
            this.prev = pos;
            this.timer.reset();
        }

        void tick() {
            this.prev = this.pos;
            this.prevRot = this.rotate;
            this.pos = this.pos.add(this.motion);
            this.rotate = this.rotate.add(this.rotateMotion);
            this.motion = this.motion.multiply(0.982);
            this.rotateMotion = this.rotateMotion.multiply(0.986);
        }

        void updateAlpha() {
            long elapsed = timer.getElapsedTime();

            if (elapsed < fadeInTime) {
                fadeProgress = elapsed / (float) fadeInTime;
                alpha = easeOutCubic(fadeProgress);
            } else if (elapsed > liveTicks - fadeOutTime) {
                long timeLeft = Math.max(0, liveTicks - elapsed);
                fadeProgress = timeLeft / (float) fadeOutTime;
                alpha = easeOutCubic(fadeProgress);
            } else {
                fadeProgress = 1.0f;
                alpha = 1.0f;
            }
        }

        float visualScale() {
            return 0.72f + easeOutCubic(fadeProgress) * 0.28f;
        }

        private float easeOutCubic(float value) {
            float clamped = Math.max(0.0f, Math.min(1.0f, value));
            return 1.0f - (float) Math.pow(1.0f - clamped, 3.0);
        }
    }
}
