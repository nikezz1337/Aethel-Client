package antileak.base.client.modules.impl.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import antileak.base.api.utils.color.ColorUtils;
import antileak.base.api.utils.render.ShaderUtils;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.settings.implement.BooleanSetting;
import antileak.base.client.modules.settings.implement.FloatSetting;
import antileak.base.client.modules.settings.implement.ListSetting;
import antileak.base.client.modules.settings.implement.ModeSetting;
import antileak.base.elysium;

public class WorldTweaks extends Module {

    public static WorldTweaks INSTANCE = new WorldTweaks();

    private final ListSetting worldSettings = new ListSetting("World Settings",
            new BooleanSetting("Time", true),
            new BooleanSetting("Fog", true),
            new BooleanSetting("Sky", true));

    private final FloatSetting timeSetting = new FloatSetting("Time", 12f, 0f, 24f, 1f)
            .visible(() -> worldSettings.is("Time"));
    private final FloatSetting fogDistanceSetting = new FloatSetting("Fog Distance", 100f, 20f, 200f, 1f)
            .visible(() -> worldSettings.is("Fog"));
    private final ModeSetting skyMode = new ModeSetting("Sky Mode", "Summer",
            "Summer", "Plasma", "Water", "Ocean", "Off")
            .visible(() -> worldSettings.is("Sky"));
    private final FloatSetting skySpeed = new FloatSetting("Sky Speed", 0.1f, 0.01f, 0.35f, 0.01f)
            .visible(() -> worldSettings.is("Sky") && !skyMode.is("Off"));
    private final FloatSetting skyScale = new FloatSetting("Sky Scale", 3.0f, 0.5f, 8.0f, 0.1f)
            .visible(() -> worldSettings.is("Sky") && !skyMode.is("Off"));

    public WorldTweaks() {
        super("CustomWorld", "World tweaks", ModuleCategory.RENDER);
        addSettings(worldSettings, timeSetting, fogDistanceSetting, skyMode, skySpeed, skyScale);
    }

    public boolean isTimeEnabled() {
        return isEnable() && worldSettings.is("Time");
    }

    public boolean isFogEnabled() {
        return isEnable() && worldSettings.is("Fog");
    }

    public boolean isSkyEnabled() {
        return isEnable() && worldSettings.is("Sky") && !skyMode.is("Off");
    }

    public boolean shouldCancelClouds() {
        return isSkyEnabled();
    }

    public long getForcedTime() {
        return ((long) timeSetting.get()) * 1000L;
    }

    public float getFogDistance() {
        return fogDistanceSetting.get();
    }

    public int getFogColor() {
        return getThemeBaseColor();
    }

    public void renderSky(Camera camera) {
        if (!isSkyEnabled() || mc.player == null || mc.world == null || camera == null) {
            return;
        }

        ShaderProgramKey key = getShaderKey();
        ShaderProgram shader = mc.getShaderLoader().getOrCreateProgram(key);
        if (shader == null) {
            return;
        }

        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;
        float fw = Math.max(1.0f, mc.getWindow().getFramebufferWidth());
        float fh = Math.max(1.0f, mc.getWindow().getFramebufferHeight());
        int themeColor = getThemeBaseColor();
        int secondaryColor = ColorUtils.darken(themeColor, 0.72f);
        int accentColor = ColorUtils.overCol(themeColor, ColorUtils.rgb(255, 255, 255), 0.35f);
        float modeValue = switch (skyMode.getCurrent()) {
            case "Plasma" -> 0.0f;
            case "Water" -> 1.0f;
            case "Ocean" -> 2.0f;
            case "Summer" -> 3.0f;
            default -> 0.0f;
        };

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(key);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        setUniform(shader, "uTime", time);
        setUniform(shader, "u_Time", time);
        setUniform(shader, "uResolution", fw, fh);
        setUniform(shader, "u_Resolution", fw, fh);
        setUniform(shader, "uColor", ColorUtils.redf(themeColor), ColorUtils.greenf(themeColor), ColorUtils.bluef(themeColor));
        setUniform(shader, "u_Color", ColorUtils.redf(themeColor), ColorUtils.greenf(themeColor), ColorUtils.bluef(themeColor), 1.0f);
        setUniform(shader, "u_Color2", ColorUtils.redf(secondaryColor), ColorUtils.greenf(secondaryColor), ColorUtils.bluef(secondaryColor), 1.0f);
        setUniform(shader, "uAlpha", 1.0f);
        setUniform(shader, "u_Alpha", 1.0f);
        setUniform(shader, "uSpeed", skySpeed.get());
        setUniform(shader, "u_Speed", skySpeed.get());
        setUniform(shader, "uScale", skyScale.get());
        setUniform(shader, "u_Scale", skyScale.get());
        setUniform(shader, "uIntensity", 1.0f);
        setUniform(shader, "u_Intensity", 1.0f);
        setUniform(shader, "uCameraDir", (float) Math.toRadians(-camera.getYaw()), (float) Math.toRadians(camera.getPitch()));
        setUniform(shader, "u_CameraDir", (float) Math.toRadians(-camera.getYaw()), (float) Math.toRadians(camera.getPitch()));
        setUniform(shader, "uFov", 70.0f);
        setUniform(shader, "u_Fov", 70.0f);
        setUniform(shader, "time", time);
        setUniform(shader, "scale", skyScale.get());
        setUniform(shader, "mode", modeValue);
        setUniform(shader, "alpha", 1.0f);
        setUniform(shader, "primaryColor", ColorUtils.redf(themeColor), ColorUtils.greenf(themeColor), ColorUtils.bluef(themeColor), 1.0f);
        setUniform(shader, "secondaryColor", ColorUtils.redf(secondaryColor), ColorUtils.greenf(secondaryColor), ColorUtils.bluef(secondaryColor), 1.0f);
        setUniform(shader, "accentColor", ColorUtils.redf(accentColor), ColorUtils.greenf(accentColor), ColorUtils.bluef(accentColor), 1.0f);

        drawSkyCube(camera);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private ShaderProgramKey getShaderKey() {
        return switch (skyMode.getCurrent()) {
            case "Summer" -> ShaderUtils.customSkySummer;
            case "Plasma" -> ShaderUtils.customSkyPlasma;
            case "Water" -> ShaderUtils.customSkyWater;
            case "Ocean" -> ShaderUtils.customSkyOcean;
            default -> ShaderUtils.customSkyShader;
        };
    }

    private void drawSkyCube(Camera camera) {
        MatrixStack matrices = new MatrixStack();
        matrices.translate(camera.getPos().x, camera.getPos().y, camera.getPos().z);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        for (int face = 0; face < 6; face++) {
            matrices.push();
            if (face == 1) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            } else if (face == 2) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
            } else if (face == 3) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
            } else if (face == 4) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0F));
            } else if (face == 5) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
            }

            Matrix4f matrix = matrices.peek().getPositionMatrix();
            buffer.vertex(matrix, -100.0f, -100.0f, -100.0f);
            buffer.vertex(matrix, -100.0f, -100.0f, 100.0f);
            buffer.vertex(matrix, 100.0f, -100.0f, 100.0f);
            buffer.vertex(matrix, 100.0f, -100.0f, -100.0f);
            matrices.pop();
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void setUniform(ShaderProgram shader, String name, float value) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x, float y) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x, float y, float z) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y, z);
        }
    }

    private void setUniform(ShaderProgram shader, String name, float x, float y, float z, float w) {
        GlUniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y, z, w);
        }
    }

    private int getThemeBaseColor() {
        if (elysium.INSTANCE == null
                || elysium.INSTANCE.themeStorage == null
                || elysium.INSTANCE.themeStorage.getThemes() == null
                || elysium.INSTANCE.themeStorage.getThemes().getTheme() == null) {
            return ColorUtils.getThemeColor();
        }

        var theme = elysium.INSTANCE.themeStorage.getThemes().getTheme();
        if (!"Rainbow".equals(theme.getName()) && theme.color != null && theme.color.length > 0) {
            return theme.color[0];
        }
        return ColorUtils.getThemeColor();
    }
}
