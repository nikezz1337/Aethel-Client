package dev.aethel.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.event.list.EventAfterWorldRender;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.SliderSetting;
import dev.aethel.util.providers.ResourceProvider;
import dev.aethel.util.render.providers.ColorProvider;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@ModuleInformation(
        moduleName = "Test",
        moduleCategory = ModuleCategory.RENDER,
        moduleDesc = "Свечение руки и предмета"
)
public class TestModule extends Module {

    private static final ShaderProgramKey BLUR_KEY = new ShaderProgramKey(
            ResourceProvider.getShaderIdentifier("shader_esp"),
            VertexFormats.POSITION_COLOR, Defines.EMPTY);

    public final SliderSetting glowStrength = new SliderSetting("Glow Strength", 1.0F, 0.1F, 5.0F, 0.1F);
    public final SliderSetting intensity = new SliderSetting("Intensity", 1.0F, 0.1F, 3.0F, 0.1F);

    private SimpleFramebuffer handFBO;
    private SimpleFramebuffer pingFBO;
    private SimpleFramebuffer pongFBO;
    private boolean hasCapture;

    @Override
    public void onDisable() {
        releaseFBOs();
        super.onDisable();
    }

    public void beginHandFBO(Framebuffer main) {
        int w = main.textureWidth;
        int h = main.textureHeight;
        if (w <= 0 || h <= 0) return;

        handFBO = ensureFBO(handFBO, w, h, true);
        pingFBO = ensureFBO(pingFBO, w, h, false);
        pongFBO = ensureFBO(pongFBO, w, h, false);

        handFBO.beginWrite(false);
        GL11.glViewport(0, 0, w, h);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public void endHandFBO(Framebuffer main) {
        handFBO.endWrite();
        main.beginWrite(false);
        GL11.glViewport(0, 0, main.textureWidth, main.textureHeight);
        hasCapture = true;
    }

    @Subscribe
    public void onAfterWorld(EventAfterWorldRender event) {
        if (!hasCapture) return;
        hasCapture = false;

        Framebuffer main = mc.getFramebuffer();
        int w = main.textureWidth;
        int h = main.textureHeight;
        if (w <= 0 || h <= 0) return;

        applyGlow(main, w, h);
    }

    private void applyGlow(Framebuffer main, int w, int h) {
        int color = ColorProvider.getThemeColor();
        float r = ColorProvider.red(color) / 255.0F;
        float g = ColorProvider.green(color) / 255.0F;
        float b = ColorProvider.blue(color) / 255.0F;
        float strength = glowStrength.getFloatValue();
        float intens = intensity.getFloatValue();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        ShaderProgram shader = RenderSystem.setShader(BLUR_KEY);
        Matrix4f identity = new Matrix4f();
        float radius = intens * 4.0F;

        pingFBO.beginWrite(false);
        GL11.glViewport(0, 0, w, h);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.setShaderTexture(0, handFBO.getColorAttachment());
        shader.getUniform("Direction").set(1.0F, 0.0F);
        shader.getUniform("Radius").set(radius);
        drawQuadWhite(identity, w, h);
        pingFBO.endWrite();

        pongFBO.beginWrite(false);
        GL11.glViewport(0, 0, w, h);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.setShaderTexture(0, pingFBO.getColorAttachment());
        shader.getUniform("Direction").set(0.0F, 1.0F);
        shader.getUniform("Radius").set(radius);
        drawQuadWhite(identity, w, h);
        pongFBO.endWrite();

        pingFBO.beginWrite(false);
        GL11.glViewport(0, 0, w, h);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.setShaderTexture(0, pongFBO.getColorAttachment());
        shader.getUniform("Direction").set(1.0F, 0.0F);
        shader.getUniform("Radius").set(radius);
        drawQuadWhite(identity, w, h);
        pingFBO.endWrite();

        pongFBO.beginWrite(false);
        GL11.glViewport(0, 0, w, h);
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.setShaderTexture(0, pingFBO.getColorAttachment());
        shader.getUniform("Direction").set(0.0F, 1.0F);
        shader.getUniform("Radius").set(radius);
        drawQuadWhite(identity, w, h);
        pongFBO.endWrite();

        main.beginWrite(false);
        GL11.glViewport(0, 0, w, h);
        RenderSystem.setShaderTexture(0, pongFBO.getColorAttachment());
        drawQuad(identity, w, h, r * strength, g * strength, b * strength);

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private void drawQuad(Matrix4f m, float w, float h, float r, float g, float b) {
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(m, -1, 1, 0).color(r, g, b, 1);
        buf.vertex(m, 1, 1, 0).color(r, g, b, 1);
        buf.vertex(m, 1, -1, 0).color(r, g, b, 1);
        buf.vertex(m, -1, -1, 0).color(r, g, b, 1);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void drawQuadWhite(Matrix4f m, float w, float h) {
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(m, -1, 1, 0).color(1, 1, 1, 1);
        buf.vertex(m, 1, 1, 0).color(1, 1, 1, 1);
        buf.vertex(m, 1, -1, 0).color(1, 1, 1, 1);
        buf.vertex(m, -1, -1, 0).color(1, 1, 1, 1);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private SimpleFramebuffer ensureFBO(SimpleFramebuffer fbo, int w, int h, boolean depth) {
        if (fbo == null || fbo.textureWidth != w || fbo.textureHeight != h) {
            if (fbo != null) fbo.delete();
            fbo = new SimpleFramebuffer(w, h, depth);
        }
        return fbo;
    }

    private void releaseFBOs() {
        if (handFBO != null) { handFBO.delete(); handFBO = null; }
        if (pingFBO != null) { pingFBO.delete(); pingFBO = null; }
        if (pongFBO != null) { pongFBO.delete(); pongFBO = null; }
    }
}
