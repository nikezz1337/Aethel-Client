package antileak.base.api.utils.render.hands;

import antileak.base.api.QClient;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.api.utils.color.ColorUtils;
import antileak.base.api.utils.render.ShaderUtils;
import antileak.base.client.modules.impl.render.ShaderHands;
import antileak.base.elysium;
import antileak.base.mixin.HeldItemRendererInvoker;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;

public class ShaderHandsRenderer implements QClient {
    private static final float EPSILON = 0.001f;

    private static ShaderHandsRenderer instance;

    private Framebuffer beforeBuffer;
    private Framebuffer afterBuffer;
    private Framebuffer maskBuffer;
    private final List<Framebuffer> bloomBuffers = new ArrayList<>();
    private int width = -1;
    private int height = -1;
    private boolean hasBeforeCapture;
    private boolean pendingComposite;
    private int configuredBeforeDepthTex = -1;
    private int configuredAfterDepthTex = -1;

    public static ShaderHandsRenderer getInstance() {
        if (instance == null) instance = new ShaderHandsRenderer();
        return instance;
    }

    public void captureBeforeHands() {
        ShaderHands module = getModule();
        if (!isEffectEnabled(module)) {
            invalidateState();
            return;
        }
        ensureBuffers();
        if (beforeBuffer == null) return;
        copyMainFramebuffer(beforeBuffer);
        hasBeforeCapture = true;
    }

    public void captureAfterHands() {
        ShaderHands module = getModule();
        if (!isEffectEnabled(module)) {
            invalidateState();
            return;
        }
        ensureBuffers();
        if (beforeBuffer == null || afterBuffer == null || maskBuffer == null) return;
        if (!hasBeforeCapture) return;

        copyMainFramebuffer(afterBuffer);
        pendingComposite = true;
    }

    public void renderOverlayIfPending(float partialTicks) {
        if (!pendingComposite) return;
        ensureBuffers();
        if (beforeBuffer == null || afterBuffer == null || maskBuffer == null) return;
        ShaderHands module = getModule();
        if (!isEffectEnabled(module)) {
            invalidateState();
            return;
        }

        ShaderProgram maskShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsMaskDiff);
        if (maskShader == null) {
            invalidateState();
            return;
        }

        maskBuffer.setClearColor(0f, 0f, 0f, 0f);
        maskBuffer.clear();
        maskBuffer.beginWrite(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(ShaderUtils.shaderHandsMaskDiff);
        RenderSystem.setShaderTexture(0, beforeBuffer.getColorAttachment());
        RenderSystem.setShaderTexture(1, afterBuffer.getColorAttachment());
        int beforeDepth = beforeBuffer.getDepthAttachment();
        int afterDepth = afterBuffer.getDepthAttachment();
        if (beforeDepth != 0 && beforeDepth != configuredBeforeDepthTex) {
            configureDepthTexture(beforeDepth);
            configuredBeforeDepthTex = beforeDepth;
        }
        if (afterDepth != 0 && afterDepth != configuredAfterDepthTex) {
            configureDepthTexture(afterDepth);
            configuredAfterDepthTex = afterDepth;
        }
        RenderSystem.setShaderTexture(2, beforeDepth);
        RenderSystem.setShaderTexture(3, afterDepth);
        drawFullscreenQuad();
        RenderSystem.enableDepthTest();

        float glowValue = module.glow.get();
        float fillValue = module.fill.get();
        float alphaValue = module.alpha.get();
        float outlineValue = module.outline.get();

        boolean hasGlow = glowValue > EPSILON;
        boolean hasFill = fillValue > EPSILON && alphaValue > EPSILON;
        int color1 = elysium.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")
                ? ColorUtils.getThemeColor(0)
                : elysium.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        int color2 = color1;

        if (module.mode.is("Красивый")) {
            renderPrettyMode(module, color1, color2, glowValue, fillValue, alphaValue, outlineValue);
            invalidateState();
            return;
        }

        int blurredMaskTexture = maskBuffer.getColorAttachment();
        if (hasGlow) {
            int iterations = Math.max(3, Math.min(8, 4 + Math.round(outlineValue * 0.7f)));
            blurredMaskTexture = runKawaseBloom(iterations);
        }

        mc.getFramebuffer().beginWrite(true);
        RenderSystem.enableBlend();
        RenderSystem.colorMask(true, true, true, false);
        RenderSystem.disableDepthTest();

        ShaderProgram glowShader = hasGlow ? mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsGlow) : null;
        if (glowShader != null) {
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SrcFactor.SRC_ALPHA,
                    GlStateManager.DstFactor.ONE,
                    GlStateManager.SrcFactor.ZERO,
                    GlStateManager.DstFactor.ONE
            );
            RenderSystem.setShader(ShaderUtils.shaderHandsGlow);
            RenderSystem.setShaderTexture(0, blurredMaskTexture);
            RenderSystem.setShaderTexture(1, maskBuffer.getColorAttachment());
            setUniform(glowShader, "color", ColorUtils.redf(color1), ColorUtils.greenf(color1), ColorUtils.bluef(color1));
            setUniform(glowShader, "color2", ColorUtils.redf(color2), ColorUtils.greenf(color2), ColorUtils.bluef(color2));
            setUniform(glowShader, "exposure", 1.0f + glowValue * 1.8f);
            drawFullscreenQuad();
        }

        if (hasFill) {
            ShaderProgram overlayShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsOverlay);
            if (overlayShader == null) {
                restoreCompositeState();
                invalidateState();
                return;
            }
            RenderSystem.blendFuncSeparate(
                    GlStateManager.SrcFactor.SRC_ALPHA,
                    GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SrcFactor.ZERO,
                    GlStateManager.DstFactor.ONE
            );
            RenderSystem.setShader(ShaderUtils.shaderHandsOverlay);
            RenderSystem.setShaderTexture(0, maskBuffer.getColorAttachment());
            setUniform(overlayShader, "color", ColorUtils.redf(color1), ColorUtils.greenf(color1), ColorUtils.bluef(color1));
            setUniform(overlayShader, "fill", fillValue);
            setUniform(overlayShader, "alpha", alphaValue);
            drawFullscreenQuad();
        }

        restoreCompositeState();
        invalidateState();
    }

    
    public void invalidateState() {
        hasBeforeCapture = false;
        pendingComposite = false;
        configuredBeforeDepthTex = -1;
        configuredAfterDepthTex = -1;
    }

    private int runKawaseBloom(int iterations) {
        ensureBloomBuffers(iterations);
        if (bloomBuffers.isEmpty()) {
            return maskBuffer.getColorAttachment();
        }

        int currentTexture = maskBuffer.getColorAttachment();
        ShaderProgram downShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsKawaseDown);
        ShaderProgram upShader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.shaderHandsKawaseUp);
        if (downShader == null || upShader == null) {
            return currentTexture;
        }

        for (int i = 0; i < iterations; i++) {
            Framebuffer dst = bloomBuffers.get(i);
            dst.setClearColor(0f, 0f, 0f, 0f);
            dst.clear();
            dst.beginWrite(true);

            RenderSystem.setShader(ShaderUtils.shaderHandsKawaseDown);
            RenderSystem.setShaderTexture(0, currentTexture);
            setHandsKawaseUniforms(downShader, dst.textureWidth, dst.textureHeight, 1.0f + i);
            drawFullscreenQuad();

            currentTexture = dst.getColorAttachment();
        }

        for (int i = iterations - 1; i >= 1; i--) {
            Framebuffer dst = bloomBuffers.get(i - 1);
            dst.setClearColor(0f, 0f, 0f, 0f);
            dst.clear();
            dst.beginWrite(true);

            RenderSystem.setShader(ShaderUtils.shaderHandsKawaseUp);
            RenderSystem.setShaderTexture(0, currentTexture);
            setHandsKawaseUniforms(upShader, dst.textureWidth, dst.textureHeight, 1.0f + i);
            setUniform(upShader, "color", 1.0f, 1.0f, 1.0f);
            drawFullscreenQuad();

            currentTexture = dst.getColorAttachment();
        }

        mc.getFramebuffer().beginWrite(true);
        return currentTexture;
    }
    private void copyMainFramebuffer(Framebuffer target) {
        int readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mc.getFramebuffer().fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.fbo);

        GL30.glBlitFramebuffer(
                0, 0, width, height,
                0, 0, width, height,
                GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT,
                GL11.GL_NEAREST
        );

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
        mc.getFramebuffer().beginWrite(true);
    }

    private void configureDepthTexture(int depthTex) {
        RenderSystem.bindTexture(depthTex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        RenderSystem.bindTexture(0);
    }

    private void ensureBuffers() {
        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        if (w == width && h == height && beforeBuffer != null && afterBuffer != null && maskBuffer != null) return;

        if (beforeBuffer != null) beforeBuffer.delete();
        if (afterBuffer != null) afterBuffer.delete();
        if (maskBuffer != null) maskBuffer.delete();
        for (Framebuffer fb : bloomBuffers) {
            fb.delete();
        }
        bloomBuffers.clear();
        beforeBuffer = new SimpleFramebuffer(w, h, true);
        afterBuffer = new SimpleFramebuffer(w, h, true);
        maskBuffer = new SimpleFramebuffer(w, h, true);
        width = w;
        height = h;
        configuredBeforeDepthTex = -1;
        configuredAfterDepthTex = -1;
    }

    private Framebuffer ensureBuffer(Framebuffer buf, boolean depth, int divisor) {
        int fw = mc.getWindow().getFramebufferWidth();
        int fh = mc.getWindow().getFramebufferHeight();
        int w = Math.max(2, fw / divisor);
        int h = Math.max(2, fh / divisor);
        if (buf == null) {
            buf = new SimpleFramebuffer(w, h, depth);
            setLinearFiltering(buf);
            return buf;
        }
        if (buf.textureWidth != w || buf.textureHeight != h) {
            buf.delete();
            buf = new SimpleFramebuffer(w, h, depth);
            setLinearFiltering(buf);
        }
        return buf;
    }

    private void ensureBloomBuffers(int iterations) {
        while (bloomBuffers.size() > iterations) {
            int last = bloomBuffers.size() - 1;
            bloomBuffers.get(last).delete();
            bloomBuffers.remove(last);
        }

        for (int i = 0; i < iterations; i++) {
            int w = Math.max(2, width >> (i + 1));
            int h = Math.max(2, height >> (i + 1));

            if (i >= bloomBuffers.size()) {
                Framebuffer fb = new SimpleFramebuffer(w, h, false);
                setLinearFiltering(fb);
                bloomBuffers.add(fb);
                continue;
            }

            Framebuffer fb = bloomBuffers.get(i);
            if (fb.textureWidth != w || fb.textureHeight != h) {
                fb.delete();
                fb = new SimpleFramebuffer(w, h, false);
                setLinearFiltering(fb);
                bloomBuffers.set(i, fb);
            }
        }
    }

    private void setLinearFiltering(Framebuffer fb) {
        RenderSystem.bindTexture(fb.getColorAttachment());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.bindTexture(0);
    }

    private ShaderHands getModule() {
        if (elysium.INSTANCE == null || ModuleClass.INSTANCE == null) return null;
        return ModuleClass.INSTANCE.shaderHands;
    }

    private void renderPrettyMode(ShaderHands module, int color1, int color2, float glowValue, float fillValue, float alphaValue, float outlineValue) {
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(ShaderUtils.blockOverlay);
        if (shader == null) return;

        mc.getFramebuffer().beginWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        RenderSystem.setShader(ShaderUtils.blockOverlay);
        RenderSystem.setShaderTexture(0, maskBuffer.getColorAttachment());

        setUniform(shader, "texelSize",
                1.0f / Math.max(1, mc.getWindow().getFramebufferWidth()),
                1.0f / Math.max(1, mc.getWindow().getFramebufferHeight()));
        setUniform(shader, "color", ColorUtils.redf(color1), ColorUtils.greenf(color1), ColorUtils.bluef(color1));
        setUniform(shader, "color2", ColorUtils.redf(color2), ColorUtils.greenf(color2), ColorUtils.bluef(color2));
        setUniform(shader, "time", (System.currentTimeMillis() % 100000L) / 1000.0f);
        setUniform(shader, "speed", module.waveSpeed.get());
        setUniform(shader, "scale", module.waveScale.get());
        setUniform(shader, "outline", outlineValue);
        setUniform(shader, "glow", glowValue);
        setUniform(shader, "fill", fillValue);
        setUniform(shader, "alpha", alphaValue);
        setUniform(shader, "outlineOnly", 0.0f);
        drawFullscreenQuad();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        restoreCompositeState();
    }

    private void restoreCompositeState() {
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.setShaderTexture(1, 0);
        RenderSystem.setShaderTexture(2, 0);
        RenderSystem.setShaderTexture(3, 0);
        mc.getFramebuffer().beginWrite(true);
    }

    private boolean isEffectEnabled(ShaderHands module) {
        if (module == null || !module.isEnable()) return false;
        boolean hasGlow = module.glow.get() > EPSILON;
        boolean hasFill = module.fill.get() > EPSILON && module.alpha.get() > EPSILON;
        return hasGlow || hasFill;
    }

    private void setUniform(ShaderProgram shader, String name, float v) {
        GlUniform u = shader.getUniform(name);
        if (u != null) u.set(v);
    }

    private void setUniform(ShaderProgram shader, String name, float x, float y) {
        GlUniform u = shader.getUniform(name);
        if (u != null) u.set(x, y);
    }

    private void setUniform(ShaderProgram shader, String name, float x, float y, float z) {
        GlUniform u = shader.getUniform(name);
        if (u != null) u.set(x, y, z);
    }

    private void u1i(int program, String name, int value) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) GL20.glUniform1i(location, value);
    }

    private void u1f(int program, String name, float value) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) GL20.glUniform1f(location, value);
    }

    private void u2f(int program, String name, float x, float y) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) GL20.glUniform2f(location, x, y);
    }

    private void u3f(int program, String name, float x, float y, float z) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) GL20.glUniform3f(location, x, y, z);
    }

    private void setHandsKawaseUniforms(ShaderProgram shader, int texWidth, int texHeight, float offset) {
        setUniform(shader, "uSize", Math.max(1, texWidth), Math.max(1, texHeight));
        setUniform(shader, "uOffset", offset, offset);
        setUniform(shader, "uHalfPixel", 0.5f / Math.max(1, texWidth), 0.5f / Math.max(1, texHeight));
    }

    private int createProgram(String vs, String fs) {
        int v = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        int f = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        int p = GL20.glCreateProgram();
        GL20.glShaderSource(v, vs);
        GL20.glCompileShader(v);
        if (GL20.glGetShaderi(v, GL20.GL_COMPILE_STATUS) == 0) {
            System.err.println("[Hands] VS error: " + GL20.glGetShaderInfoLog(v, 1024));
        }
        GL20.glShaderSource(f, fs);
        GL20.glCompileShader(f);
        if (GL20.glGetShaderi(f, GL20.GL_COMPILE_STATUS) == 0) {
            System.err.println("[Hands] FS error: " + GL20.glGetShaderInfoLog(f, 1024));
        }
        GL20.glAttachShader(p, v);
        GL20.glAttachShader(p, f);
        GL20.glLinkProgram(p);
        return p;
    }

    private void bindTex(int unit, int tex) {
        GL13.glActiveTexture(unit);
        RenderSystem.bindTexture(tex);
    }

    private void drawFullscreenQuad() {
        float sw = Math.max(mc.getWindow().getScaledWidth(), 1);
        float sh = Math.max(mc.getWindow().getScaledHeight(), 1);
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(0, 0, 0).texture(0, 1).color(1f, 1f, 1f, 1f);
        b.vertex(0, sh, 0).texture(0, 0).color(1f, 1f, 1f, 1f);
        b.vertex(sw, sh, 0).texture(1, 0).color(1f, 1f, 1f, 1f);
        b.vertex(sw, 0, 0).texture(1, 1).color(1f, 1f, 1f, 1f);
        BufferRenderer.drawWithGlobalProgram(b.end());
    }
}


