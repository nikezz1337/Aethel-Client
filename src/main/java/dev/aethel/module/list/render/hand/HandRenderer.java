package dev.aethel.module.list.render.hand;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.module.settings.impl.ThemeManager;
import dev.aethel.util.providers.ResourceProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import net.minecraft.util.Arm;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class HandRenderer {

    private static final float[] WHITE = {1f, 1f, 1f};
    private static final float[] BLACK = {0f, 0f, 0f};

    private static final ShaderProgramKey BLIT_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("hands_blit"),
            VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    private static final ShaderProgramKey MASK_DIFF_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("hands_mask_diff"),
            VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    private static final ShaderProgramKey KAWA_DOWN_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("hands_kawase_down"),
            VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    private static final ShaderProgramKey KAWA_UP_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("hands_kawase_up"),
            VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    private static final ShaderProgramKey TRAIL_MASK_DIFF_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("handstrail/mask_diff"),
            VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    private static final ShaderProgramKey HAND_TRAIL_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("handstrail/hand_trail"),
            VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    private static final ShaderProgramKey HAND_FIRE_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("handstrail/hand_fire"),
            VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    private static final ShaderProgramKey HANDS_FIRE_PRETTY_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("handstrail/hands_fire_pretty"),
            VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    private static final ShaderProgramKey HAND_GLOW_SINGLE_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("handstrail/hand_glow_single"),
            VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    private static final ShaderProgramKey HAND_LIGHTNING_SINGLE_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("handstrail/hand_lightning_single"),
            VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);

    private static final HandRenderer INSTANCE = new HandRenderer();
    private static final Matrix4f IDENTITY = new Matrix4f();

    private SimpleFramebuffer fboBefore;
    private SimpleFramebuffer fboAfter;
    private SimpleFramebuffer fboMask;
    private SimpleFramebuffer fboBlurA;
    private SimpleFramebuffer fboBlurB;
    private SimpleFramebuffer fboTrailA;
    private SimpleFramebuffer fboTrailB;
    private SimpleFramebuffer fboTrailMask;
    private SimpleFramebuffer fboComposite;

    private boolean enabled;
    private boolean hasCapture;
    private int lastWidth, lastHeight;

    private float time;
    private float prevTime;

    public static HandRenderer getInstance() {
        return INSTANCE;
    }

    public void setEnabled(boolean e) {
        this.enabled = e;
        if (!e) releaseFBOs();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void onAfterWorld(HandModule module) {
        if (!enabled) return;
        module.updateCameraDelta(
                MinecraftClient.getInstance().player.getYaw(),
                MinecraftClient.getInstance().player.getPitch()
        );
    }

    public void captureSceneBeforeHands() {
        if (!enabled) return;
        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        int w = main.textureWidth;
        int h = main.textureHeight;
        if (w <= 0 || h <= 0) return;
        ensureTargets(w, h);
        copyTexture(main.getColorAttachment(), fboBefore, w, h);
    }

    public void captureSceneAfterHands(HandModule module) {
        if (!enabled || fboBefore == null) return;
        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        int w = main.textureWidth;
        int h = main.textureHeight;
        if (w <= 0 || h <= 0) return;
        ensureTargets(w, h);
        copyTexture(main.getColorAttachment(), fboAfter, w, h);
        hasCapture = true;

        float now = (System.currentTimeMillis() % 100000L) / 1000.0F;
        float dt = Math.min(now - prevTime, 0.1F);
        prevTime = now;
        time += dt * module.speed.getFloatValue() * 0.5F;

        renderEffect(module);
    }

    private void renderEffect(HandModule module) {
        if (!hasCapture) return;
        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
        int w = main.textureWidth;
        int h = main.textureHeight;
        if (w <= 0 || h <= 0) return;

        begin2D();

        // 1) mask_diff: compare before vs after → hand silhouette
        fboMask.beginWrite(false);
        GL11.glViewport(0, 0, w, h);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(MASK_DIFF_KEY);
        RenderSystem.setShaderTexture(0, fboBefore.getColorAttachment());
        RenderSystem.setShaderTexture(1, fboAfter.getColorAttachment());
        drawFullScreenQuad();
        RenderSystem.setShaderTexture(1, 0);

        if (module.isSingle()) {
            renderSingleMode(main, w, h, module);
        } else {
            renderLayeredMode(main, w, h, module);
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        end2D();
        hasCapture = false;
    }

    private void renderSingleMode(Framebuffer main, int w, int h, HandModule module) {
        main.beginWrite(false);
        GL11.glViewport(0, 0, w, h);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        float[] gc = getGlowColor(module);
        float[] gc2 = getGlowColor2(module);

        ShaderProgram shader = RenderSystem.setShader(HAND_LIGHTNING_SINGLE_KEY);
        if (shader != null) {
            RenderSystem.setShaderTexture(0, fboMask.getColorAttachment());
            setUniform(shader, "texSize", (float) w, (float) h);
            setUniform(shader, "time", time);
            setUniform(shader, "intensity", module.intensity.getFloatValue());
            setUniform(shader, "radius", module.blurRadius.getFloatValue());
            setUniform(shader, "glowColor", gc[0], gc[1], gc[2], 1.0F);
            setUniform(shader, "glowColor2", gc2[0], gc2[1], gc2[2], 1.0F);
            drawFullScreenQuad();
        }
        RenderSystem.setShaderTexture(0, 0);
    }

    private void renderLayeredMode(Framebuffer main, int w, int h, HandModule module) {
        int trailTex = buildTrailMask(w, h, fboMask.getColorAttachment(), module);

        main.beginWrite(false);
        GL11.glViewport(0, 0, w, h);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        if (module.mode.is("Pretty")) {
            renderPrettyComposite(main, w, h, module);
        } else {
            renderSmokeComposite(main, w, h, trailTex, module);
        }
    }

    private void renderSmokeComposite(Framebuffer main, int w, int h, int trailTex, HandModule module) {
        ShaderProgram blitShader = RenderSystem.setShader(BLIT_KEY);
        RenderSystem.setShaderTexture(0, trailTex);
        if (blitShader != null) {
            GlUniform mul = blitShader.getUniform("colorMul");
            if (mul != null) {
                mul.set(1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
        drawFullScreenQuad();
        RenderSystem.setShaderTexture(0, 0);
    }

    private void renderPrettyComposite(Framebuffer main, int w, int h, HandModule module) {
        float[] gc = getGlowColor(module);
        float[] gc2 = getGlowColor2(module);

        ShaderProgram shader = RenderSystem.setShader(HANDS_FIRE_PRETTY_KEY);
        if (shader != null) {
            RenderSystem.setShaderTexture(0, fboMask.getColorAttachment());
            RenderSystem.setShaderTexture(1, fboMask.getColorAttachment());
            setUniform(shader, "color", gc[0], gc[1], gc[2]);
            setUniform(shader, "color2", gc2[0], gc2[1], gc2[2]);
            setUniform(shader, "time", time);
            setUniform(shader, "height", module.prettyHeight.getFloatValue());
            setUniform(shader, "speed", module.prettySpeed.getFloatValue());
            setUniform(shader, "intensity", module.intensity.getFloatValue() * module.prettyGlow.getFloatValue());
            setUniform(shader, "windStrength", module.prettyWind.getFloatValue());
            setUniform(shader, "waveStrength", module.prettyWave.getFloatValue());
            setUniform(shader, "camOffset", module.getInterpolatedDeltaYaw(0F) * 0.003F, module.getInterpolatedDeltaPitch(0F) * 0.003F);
            drawFullScreenQuad();
            RenderSystem.setShaderTexture(1, 0);
        }
        RenderSystem.setShaderTexture(0, 0);
    }

    private int buildTrailMask(int w, int h, int maskTex, HandModule module) {
        ensureTrailTargets(w, h);

        // trail_A = hand_trail(trail_B, scene, mask) — ping-pong
        fboTrailA.beginWrite(false);
        GL11.glViewport(0, 0, w, h);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        ShaderProgram trailShader = RenderSystem.setShader(HAND_TRAIL_KEY);
        if (trailShader != null) {
            RenderSystem.setShaderTexture(0, fboTrailB != null ? fboTrailB.getColorAttachment() : maskTex);
            RenderSystem.setShaderTexture(1, fboBefore != null ? fboBefore.getColorAttachment() : maskTex);
            RenderSystem.setShaderTexture(2, maskTex);
            setUniform(trailShader, "texSize", (float) w, (float) h);
            setUniform(trailShader, "time", time);
            setUniform(trailShader, "intensity", module.intensity.getFloatValue());
            setUniform(trailShader, "speed", module.speed.getFloatValue());
            setUniform(trailShader, "length", module.trailLength.getFloatValue());
            setUniform(trailShader, "trailSoftness", module.trailSoftness.getFloatValue());
            setUniform(trailShader, "trailBlur", module.trailBlur.getFloatValue());
            setUniform(trailShader, "smoke", module.smokeAmount.getFloatValue());
            setUniform(trailShader, "activity", module.getInterpolatedActivity(0F));
            setUniform(trailShader, "trailFade", module.trailFade.getFloatValue());
            setUniform(trailShader, "slash", 0.0F);
            setUniform(trailShader, "slashDir", 1.0F);
            setUniform(trailShader, "swingHand", 0.0F);
            setUniform(trailShader, "camShift", module.getInterpolatedDeltaYaw(0F) * 0.004F, module.getInterpolatedDeltaPitch(0F) * 0.004F);
            float[] gc = getGlowColor(module);
            setUniform(trailShader, "glowColor", gc[0], gc[1], gc[2], 1.0F);
        }
        drawFullScreenQuad();
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.setShaderTexture(1, 0);
        RenderSystem.setShaderTexture(2, 0);

        // swap trail A ↔ B
        SimpleFramebuffer tmp = fboTrailA;
        fboTrailA = fboTrailB;
        fboTrailB = tmp;

        // hand_fire: composite trail onto scene
        fboComposite.beginWrite(false);
        GL11.glViewport(0, 0, w, h);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        ShaderProgram fireShader = RenderSystem.setShader(HAND_FIRE_KEY);
        if (fireShader != null) {
            RenderSystem.setShaderTexture(0, fboTrailB != null ? fboTrailB.getColorAttachment() : maskTex);
            RenderSystem.setShaderTexture(2, maskTex);
            setUniform(fireShader, "texSize", (float) w, (float) h);
            setUniform(fireShader, "time", time);
            setUniform(fireShader, "intensity", module.intensity.getFloatValue());
            setUniform(fireShader, "handSoftness", module.handSoftness.getFloatValue());
            setUniform(fireShader, "handBlur", module.handBlur.getFloatValue());
            setUniform(fireShader, "smoke", module.smokeAmount.getFloatValue());
            setUniform(fireShader, "activity", module.getInterpolatedActivity(0F));
            float[] gc = getGlowColor(module);
            setUniform(fireShader, "glowColor", gc[0], gc[1], gc[2], 1.0F);
        }
        drawFullScreenQuad();
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.setShaderTexture(1, 0);
        RenderSystem.setShaderTexture(2, 0);

        return fboComposite.getColorAttachment();
    }

    private int buildBlur(int w, int h, int srcTex, float radius) {
        ensureBlurTargets(w, h);
        float o = Math.max(1f, 1f + radius);

        kawasePass(KAWA_DOWN_KEY, fboBlurA, srcTex, o, w, h, null);
        kawasePass(KAWA_DOWN_KEY, fboBlurB, fboBlurA.getColorAttachment(), o * 2f, w, h, null);
        kawasePass(KAWA_UP_KEY, fboBlurA, fboBlurB.getColorAttachment(), o * 2f, w, h, WHITE);
        kawasePass(KAWA_UP_KEY, fboBlurB, fboBlurA.getColorAttachment(), o, w, h, WHITE);

        return fboBlurB.getColorAttachment();
    }

    private void kawasePass(ShaderProgramKey program, SimpleFramebuffer dst, int srcTex,
                            float offset, int width, int height, float[] color) {
        dst.beginWrite(false);
        GL11.glViewport(0, 0, width, height);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, srcTex);
        ShaderProgram shader = RenderSystem.setShader(program);
        if (shader != null) {
            setUniform(shader, "uOffset", offset, offset);
            setUniform(shader, "uHalfPixel", 0.5f / width, 0.5f / height);
            setUniform(shader, "uSize", (float) width, (float) height);
            if (color != null) {
                setUniform(shader, "color", color[0], color[1], color[2]);
            }
        }
        drawFullScreenQuad();
        RenderSystem.setShaderTexture(0, 0);
    }

    // ==================== Color helpers ====================

    private float[] getGlowColor(HandModule module) {
        if (module.useThemeColor.getValue()) {
            int tc = ThemeManager.getInstance().getCurrentTheme().getColorFirst();
            return new float[]{
                    ((tc >> 16) & 0xFF) / 255.0F,
                    ((tc >> 8) & 0xFF) / 255.0F,
                    (tc & 0xFF) / 255.0F
            };
        }

        return new float[]{0.6F, 0.2F, 1.0F};
    }

    private float[] getGlowColor2(HandModule module) {
        if (module.useThemeColor.getValue()) {
            int tc = ThemeManager.getInstance().getCurrentTheme().getColorSecond();
            return new float[]{
                    ((tc >> 16) & 0xFF) / 255.0F,
                    ((tc >> 8) & 0xFF) / 255.0F,
                    (tc & 0xFF) / 255.0F
            };
        }
        float[] base = getGlowColor(module);
        return new float[]{base[0] * 0.8F, base[1] * 0.4F, base[2] * 0.2F};
    }

    // ==================== GL state management ====================

    private Matrix4f savedProjection;

    private void begin2D() {
        savedProjection = RenderSystem.getProjectionMatrix();
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.setProjectionMatrix(new Matrix4f(), ProjectionType.ORTHOGRAPHIC);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
    }

    private void end2D() {
        RenderSystem.setProjectionMatrix(savedProjection, ProjectionType.PERSPECTIVE);
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // ==================== Texture copy ====================

    private void copyTexture(int srcTex, SimpleFramebuffer dst, int w, int h) {
        dst.beginWrite(false);
        GL11.glViewport(0, 0, w, h);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        ShaderProgram shader = RenderSystem.setShader(BLIT_KEY);
        RenderSystem.setShaderTexture(0, srcTex);
        if (shader != null) {
            GlUniform mul = shader.getUniform("colorMul");
            if (mul != null) mul.set(1f, 1f, 1f, 1f);
        }
        drawFullScreenQuad();
        RenderSystem.setShaderTexture(0, 0);
    }

    // ==================== Quad drawing ====================

    private void drawFullScreenQuad() {
        BufferBuilder buf = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(IDENTITY, -1, -1, 0).texture(0, 0).color(-1);
        buf.vertex(IDENTITY, -1, 1, 0).texture(0, 1).color(-1);
        buf.vertex(IDENTITY, 1, 1, 0).texture(1, 1).color(-1);
        buf.vertex(IDENTITY, 1, -1, 0).texture(1, 0).color(-1);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    // ==================== FBO management ====================

    private SimpleFramebuffer createFBO(int w, int h) {
        SimpleFramebuffer fb = new SimpleFramebuffer(w, h, false);
        GlStateManager._bindTexture(fb.getColorAttachment());
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        return fb;
    }

    private void ensureTargets(int w, int h) {
        if (fboBefore != null && lastWidth == w && lastHeight == h) return;
        releaseFBOs();
        fboBefore = createFBO(w, h);
        fboAfter = createFBO(w, h);
        fboMask = createFBO(w, h);
        fboTrailA = createFBO(w, h);
        fboTrailB = createFBO(w, h);
        fboTrailMask = createFBO(w, h);
        fboComposite = createFBO(w, h);
        lastWidth = w;
        lastHeight = h;
    }

    private void ensureBlurTargets(int w, int h) {
        if (fboBlurA != null && fboBlurA.textureWidth == w && fboBlurA.textureHeight == h) return;
        if (fboBlurA != null) fboBlurA.delete();
        if (fboBlurB != null) fboBlurB.delete();
        fboBlurA = createFBO(w, h);
        fboBlurB = createFBO(w, h);
    }

    private void ensureTrailTargets(int w, int h) {
        ensureBlurTargets(w, h);
    }

    private void releaseFBOs() {
        if (fboBefore != null) { fboBefore.delete(); fboBefore = null; }
        if (fboAfter != null) { fboAfter.delete(); fboAfter = null; }
        if (fboMask != null) { fboMask.delete(); fboMask = null; }
        if (fboBlurA != null) { fboBlurA.delete(); fboBlurA = null; }
        if (fboBlurB != null) { fboBlurB.delete(); fboBlurB = null; }
        if (fboTrailA != null) { fboTrailA.delete(); fboTrailA = null; }
        if (fboTrailB != null) { fboTrailB.delete(); fboTrailB = null; }
        if (fboTrailMask != null) { fboTrailMask.delete(); fboTrailMask = null; }
        if (fboComposite != null) { fboComposite.delete(); fboComposite = null; }
        hasCapture = false;
        lastWidth = 0;
        lastHeight = 0;
    }

    // ==================== Utility ====================

    private void setUniform(ShaderProgram shader, String name, float... values) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            if (values.length == 1) uniform.set(values[0]);
            else if (values.length == 2) uniform.set(values[0], values[1]);
            else if (values.length == 3) uniform.set(values[0], values[1], values[2]);
            else if (values.length == 4) uniform.set(values[0], values[1], values[2], values[3]);
        }
    }
}
