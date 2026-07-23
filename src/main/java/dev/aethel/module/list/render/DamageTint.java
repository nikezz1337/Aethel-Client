package dev.aethel.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.event.list.EventAfterWorldRender;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.SliderSetting;
import dev.aethel.util.providers.ResourceProvider;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@ModuleInformation(
        moduleName = "Damage Tint",
        moduleCategory = ModuleCategory.RENDER,
        moduleDesc = "Тонирование экрана при получении урона"
)
public class DamageTint extends Module {

    private static final ShaderProgramKey DAMAGE_TINT_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("damage_tint"),
            VertexFormats.POSITION_COLOR, Defines.EMPTY);

    private static final float START_HP = 8.0f;
    private static final float FULL_HP = 3.0f;
    private static final float VIGNETTE = 1.0f;
    private static final float DESAT = 0.6f;
    private static final boolean PULSE_ENABLED = true;
    private static final float PULSE_SPEED = 1.2f;
    private static final float PULSE_MIN = 0.1f;
    private static final float PULSE_MAX = 0.4f;
    private static final float FADE_IN = 0.2f;
    private static final float FADE_OUT = 0.4f;

    private final SliderSetting contrastSetting = new SliderSetting("Контраст", 0.6, 0.0, 1.0, 0.01);

    private SimpleFramebuffer tempFBO;
    private float smoothT;
    private long lastUpdateMs;

    @Override
    public void onDisable() {
        smoothT = 0.0f;
        lastUpdateMs = 0L;
        releaseFBO();
        super.onDisable();
    }

    @Subscribe
    public void onAfterWorld(EventAfterWorldRender event) {
        if (mc.player == null || mc.world == null) return;

        float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float hpGate = MathHelper.floor(hp * 2.0f) / 2.0f;

        float range = Math.max(0.1f, START_HP - FULL_HP);
        float targetT = MathHelper.clamp((START_HP - hpGate) / range, 0.0f, 1.0f);
        float t = smoothTowards(targetT);

        if (t <= 0.0f) return;

        float strength = t * VIGNETTE;
        float desat = t * DESAT;
        float contrast = t * (float) contrastSetting.getValue();

        if (PULSE_ENABLED) {
            float time = (System.currentTimeMillis() % 100000L) / 1000.0f;
            float wave = MathHelper.sin(time * PULSE_SPEED * 6.2831855f);
            float amp = MathHelper.lerp(t, PULSE_MIN, PULSE_MAX);
            strength *= 1.0f + (wave * amp);
        }

        if (strength <= 0.001f && desat <= 0.001f && contrast <= 0.001f) return;

        Framebuffer main = mc.getFramebuffer();
        int w = main.textureWidth;
        int h = main.textureHeight;
        if (w <= 0 || h <= 0) return;

        tempFBO = ensureFBO(tempFBO, w, h);

        tempFBO.beginWrite(false);
        GL11.glViewport(0, 0, w, h);
        main.draw(w, h);
        main.beginWrite(false);
        GL11.glViewport(0, 0, w, h);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        ShaderProgram shader = RenderSystem.setShader(DAMAGE_TINT_KEY);
        RenderSystem.setShaderTexture(0, tempFBO.getColorAttachment());
        shader.getUniform("Strength").set(strength);
        shader.getUniform("Desat").set(desat);
        shader.getUniform("Contrast").set(contrast);
        shader.getUniform("VignetteColor").set(0.8f, 0.0f, 0.0f);

        drawFullQuad();

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private float smoothTowards(float target) {
        long now = System.currentTimeMillis();
        float dt = (lastUpdateMs == 0L) ? 0.016f : (now - lastUpdateMs) / 1000.0f;
        lastUpdateMs = now;

        float rate = target >= smoothT ? (1.0f / FADE_IN) : (1.0f / FADE_OUT);
        float delta = target - smoothT;
        float step = MathHelper.clamp(rate * dt, 0.0f, 1.0f);
        smoothT += delta * step;
        smoothT = MathHelper.clamp(smoothT, 0.0f, 1.0f);
        return smoothT;
    }

    private void drawFullQuad() {
        Matrix4f identity = new Matrix4f();
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(identity, -1, 1, 0).color(1, 1, 1, 1);
        buf.vertex(identity, 1, 1, 0).color(1, 1, 1, 1);
        buf.vertex(identity, 1, -1, 0).color(1, 1, 1, 1);
        buf.vertex(identity, -1, -1, 0).color(1, 1, 1, 1);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private SimpleFramebuffer ensureFBO(SimpleFramebuffer fbo, int w, int h) {
        if (fbo == null || fbo.textureWidth != w || fbo.textureHeight != h) {
            if (fbo != null) fbo.delete();
            fbo = new SimpleFramebuffer(w, h, false);
        }
        return fbo;
    }

    private void releaseFBO() {
        if (tempFBO != null) {
            tempFBO.delete();
            tempFBO = null;
        }
    }
}
