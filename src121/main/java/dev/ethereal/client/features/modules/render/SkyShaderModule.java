package dev.ethereal.client.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.system.files.FileUtil;
import lombok.Getter;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@ModuleRegister(name = "Sky Shader", category = Category.RENDER)
public class SkyShaderModule extends Module {
    @Getter private static final SkyShaderModule instance = new SkyShaderModule();

    private static final ShaderProgramKey AURORA_SHADER_KEY =
            new ShaderProgramKey(FileUtil.getShader("aurora_overlay"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
    private static final AuroraShaderState AURORA_SHADER_STATE = new AuroraShaderState();
    private static final int AURORA_BANDS = 12;
    private static final int AURORA_CURTAIN_SEGMENTS = 34;
    private static final int AURORA_CANOPY_RINGS = 7;
    private static final float AURORA_DISTANCE = 116.0f;
    private static final float AURORA_HALF_SPAN = 92.0f;
    private static final float AURORA_BOTTOM_Y = 6.0f;
    private static final float AURORA_TOP_Y = 170.0f;
    private static final float AURORA_CANOPY_RADIUS = 154.0f;
    private static final float AURORA_CANOPY_Y = 184.0f;

    private final SliderSetting alpha = new SliderSetting("Alpha").value(10.55f).range(0.1f, 10.55f).step(0.05f);

    public SkyShaderModule() {
        addSettings(alpha);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        Matrix4f matrix = event.matrixStack().peek().getPositionMatrix();

        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);

        AuroraShaderState shaderState = AURORA_SHADER_STATE;
        shaderState.use();

        if (shaderState.time != null) {
            shaderState.time.set((System.currentTimeMillis() % 1_000_000L) / 1000.0f);
        }
        if (shaderState.resolution != null) {
            shaderState.resolution.set(
                    Math.max(1.0f, (float) mc.getWindow().getFramebufferWidth()),
                    Math.max(1.0f, (float) mc.getWindow().getFramebufferHeight())
            );
        }
        if (shaderState.overlayAlpha != null) {
            shaderState.overlayAlpha.set(alpha.getValue());
        }
        if (shaderState.rain != null) {
            shaderState.rain.set(1.0f - mc.world.getRainGradient(event.partialTicks()));
        }

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float time = (System.currentTimeMillis() % 1_000_000L) / 1000.0f;
        renderAuroraLayer(buffer, matrix, time, 0, AURORA_DISTANCE, AURORA_HALF_SPAN, AURORA_BOTTOM_Y, AURORA_TOP_Y, 1.0f);
        renderAuroraLayer(buffer, matrix, time, 1, AURORA_DISTANCE * 0.74f, AURORA_HALF_SPAN * 1.08f, 58.0f, 214.0f, 0.46f);
        renderAuroraCanopy(buffer, matrix, time);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    private void renderAuroraCanopy(BufferBuilder buffer, Matrix4f matrix, float time) {
        for (int band = 0; band < AURORA_BANDS; band++) {
            float nextBand = band + 1.0f;
            float bandUv = band / (float) AURORA_BANDS;
            float nextBandUv = nextBand / (float) AURORA_BANDS;
            float angle0 = (float) (bandUv * Math.PI * 2.0);
            float angle1 = (float) (nextBandUv * Math.PI * 2.0);
            for (int ring = 0; ring < AURORA_CANOPY_RINGS; ring++) {
                float inner = ring / (float) AURORA_CANOPY_RINGS;
                float outer = (ring + 1) / (float) AURORA_CANOPY_RINGS;
                float radius0 = inner * AURORA_CANOPY_RADIUS;
                float radius1 = outer * AURORA_CANOPY_RADIUS;
                float y0 = canopyHeight(time, bandUv, inner);
                float y1 = canopyHeight(time, bandUv, outer);
                float y2 = canopyHeight(time, nextBandUv, outer);
                float y3 = canopyHeight(time, nextBandUv, inner);

                putAuroraPolarVertex(buffer, matrix, angle0, radius0, y0, inner, bandUv, 0.34f);
                putAuroraPolarVertex(buffer, matrix, angle0, radius1, y1, outer, bandUv, 0.48f);
                putAuroraPolarVertex(buffer, matrix, angle1, radius1, y2, outer, nextBandUv, 0.48f);
                putAuroraPolarVertex(buffer, matrix, angle1, radius0, y3, inner, nextBandUv, 0.34f);
            }
        }
    }

    private float canopyHeight(float time, float angleUv, float radiusUv) {
        return AURORA_CANOPY_Y
                + 20.0f * MathHelper.sin((float) (time * 0.42f + angleUv * Math.PI * 2.0))
                - 34.0f * radiusUv;
    }

    private void putAuroraPolarVertex(BufferBuilder buffer, Matrix4f matrix, float angle, float radius, float y,
                                      float vertical, float band, float alpha) {
        float x = MathHelper.sin(angle) * radius;
        float z = MathHelper.cos(angle) * radius;
        buffer.vertex(matrix, x, y, z).color(vertical, MathHelper.clamp(0.62f + vertical * 0.38f, 0.0f, 1.0f), band, alpha);
    }

    private void renderAuroraLayer(BufferBuilder buffer, Matrix4f matrix, float time, int layer, float baseDistance,
                                   float halfSpan, float bottomY, float topY, float layerAlpha) {
        float angleOffset = layer == 0 ? 0.0f : (float) Math.PI / AURORA_BANDS;
        for (int band = 0; band < AURORA_BANDS; band++) {
            float bandProgress = (band + layer * 0.43f) / (float) AURORA_BANDS;
            float angle = (float) (bandProgress * Math.PI * 2.0 + angleOffset);
            float dirX = MathHelper.sin(angle);
            float dirZ = MathHelper.cos(angle);
            float tangentX = dirZ;
            float tangentZ = -dirX;
            float bandDistance = baseDistance + 16.0f * MathHelper.sin(band * 1.91f + layer * 0.83f);
            float bandUv = bandProgress - MathHelper.floor(bandProgress);
            for (int i = 0; i < AURORA_CURTAIN_SEGMENTS; i++) {
                float left = MathHelper.lerp(i / (float) AURORA_CURTAIN_SEGMENTS, -halfSpan, halfSpan);
                float right = MathHelper.lerp((i + 1) / (float) AURORA_CURTAIN_SEGMENTS, -halfSpan, halfSpan);
                float phase = time * (0.86f + layer * 0.24f) + band * 1.35f;
                float diagonalLeft = (layer == 0 ? 0.14f : 0.34f) * left + 14.0f * MathHelper.sin(phase + band);
                float diagonalRight = (layer == 0 ? 0.14f : 0.34f) * right + 14.0f * MathHelper.sin(phase + band);
                float baseWaveLeft = 10.0f * MathHelper.sin(left * 0.045f + phase);
                float baseWaveRight = 10.0f * MathHelper.sin(right * 0.045f + phase);
                float topWaveLeft = 18.0f * MathHelper.sin(left * 0.025f + phase * 0.72f);
                float topWaveRight = 18.0f * MathHelper.sin(right * 0.025f + phase * 0.72f);

                putAuroraVertex(buffer, matrix, dirX, dirZ, tangentX, tangentZ, left, bandDistance, bottomY + baseWaveLeft, 0.0f, bandUv, halfSpan, layerAlpha);
                putAuroraVertex(buffer, matrix, dirX, dirZ, tangentX, tangentZ, right, bandDistance, bottomY + baseWaveRight, 0.0f, bandUv, halfSpan, layerAlpha);
                putAuroraVertex(buffer, matrix, dirX, dirZ, tangentX, tangentZ, right + diagonalRight, bandDistance, topY + topWaveRight, 1.0f, bandUv, halfSpan, layerAlpha);
                putAuroraVertex(buffer, matrix, dirX, dirZ, tangentX, tangentZ, left + diagonalLeft, bandDistance, topY + topWaveLeft, 1.0f, bandUv, halfSpan, layerAlpha);
            }
        }
    }

    private void putAuroraVertex(BufferBuilder buffer, Matrix4f matrix, float dirX, float dirZ, float tangentX, float tangentZ,
                                 float lateral, float distance, float y, float vertical, float band, float halfSpan, float layerAlpha) {
        float x = dirX * distance + tangentX * lateral;
        float z = dirZ * distance + tangentZ * lateral;
        float lateralUv = MathHelper.clamp(lateral / halfSpan * 0.5f + 0.5f, 0.0f, 1.0f);
        float alphaValue = MathHelper.clamp(0.20f + vertical * 0.80f, 0.0f, 1.0f) * layerAlpha;
        buffer.vertex(matrix, x, y, z).color(lateralUv, vertical, band, alphaValue);
    }

    private static final class AuroraShaderState {
        private ShaderProgram lastShader;
        private GlUniform time;
        private GlUniform resolution;
        private GlUniform overlayAlpha;
        private GlUniform rain;

        private void use() {
            ShaderProgram shader = RenderSystem.setShader(AURORA_SHADER_KEY);
            if (shader != lastShader) {
                time = shader.getUniform("Time");
                resolution = shader.getUniform("Resolution");
                overlayAlpha = shader.getUniform("OverlayAlpha");
                rain = shader.getUniform("RainVisibility");
                lastShader = shader;
            }
        }
    }
}
