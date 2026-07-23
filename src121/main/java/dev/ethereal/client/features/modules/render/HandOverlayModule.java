package dev.ethereal.client.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.system.files.FileUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.render.ShaderCompat;
import dev.ethereal.inject.accessors.MultiPhaseParametersAccessor;
import dev.ethereal.inject.accessors.RenderLayerMultiPhaseAccessor;
import dev.ethereal.inject.accessors.RenderPhaseTextureBaseAccessor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

@ModuleRegister(name = "HandOverlay", category = Category.RENDER)
public class HandOverlayModule extends Module {
    @Getter private static final HandOverlayModule instance = new HandOverlayModule();

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

    private static final class HandOverlayShaderState {
        private final ShaderProgramKey key;
        private ShaderProgram lastShader;
        private GlUniform time;
        private GlUniform mouse;
        private GlUniform resolution;
        private GlUniform alpha;
        private GlUniform mode;

        private HandOverlayShaderState(ShaderProgramKey key) {
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

    private static final ShaderProgramKey HAND_OVERLAY_SHADER_KEY =
            new ShaderProgramKey(FileUtil.getShader("hand_overlay"), VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, Defines.EMPTY);
    private static final RenderPhase.ShaderProgram HAND_OVERLAY_PROGRAM = new RenderPhase.ShaderProgram(HAND_OVERLAY_SHADER_KEY);
    private static final HandOverlayShaderState HAND_SHADER_STATE = new HandOverlayShaderState(HAND_OVERLAY_SHADER_KEY);
    private static final Map<Identifier, RenderLayer> TEXTURE_LAYER_CACHE = new HashMap<>();

    private final Map<RenderLayer, RenderLayer> remappedLayers = new IdentityHashMap<>();

    private final ModeSetting shaderMode = new ModeSetting("Shader Mode").value(ShaderMode.CLASSIC).values(ShaderMode.values());
    private final SliderSetting alpha = new SliderSetting("Alpha").value(105f).range(0f, 255f).step(5f);

    public HandOverlayModule() {
        addSettings(shaderMode, alpha);
    }

    @Override
    public void onDisable() {
        remappedLayers.clear();
    }

    public RenderLayer getLayerAndPrepare(Identifier texture) {
        if (texture == null) {
            return RenderLayer.getEntityTranslucent(Identifier.ofVanilla("textures/entity/steve.png"));
        }
        if (ShaderCompat.isIrisShaderPackActive()) {
            return RenderLayer.getEntityTranslucent(texture);
        }

        updateShaderUniforms(getOverlayStrength());
        return TEXTURE_LAYER_CACHE.computeIfAbsent(texture, this::createLayer);
    }

    public VertexConsumerProvider wrap(VertexConsumerProvider delegate) {
        if (ShaderCompat.isIrisShaderPackActive()) {
            return delegate;
        }

        updateShaderUniforms(getOverlayStrength());
        return renderLayer -> delegate.getBuffer(remapLayer(renderLayer));
    }

    public boolean isShaderPackCompatMode() {
        return ShaderCompat.isIrisShaderPackActive();
    }

    public void beginCompatOverlay() {
        float strength = getOverlayStrength();
        int overlayAlpha = MathHelper.clamp(Math.round(alpha.getValue() * 0.72f), 0, 255);
        Color accent = UIColors.primary(overlayAlpha);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(
                accent.getRed() / 255.0f,
                accent.getGreen() / 255.0f,
                accent.getBlue() / 255.0f,
                MathHelper.clamp(strength * 0.82f, 0.0f, 1.0f)
        );
    }

    public void endCompatOverlay() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private RenderLayer remapLayer(RenderLayer sourceLayer) {
        if (sourceLayer == null) {
            return RenderLayer.getEntityTranslucent(Identifier.ofVanilla("textures/entity/steve.png"));
        }
        if (!sourceLayer.getVertexFormat().equals(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL)) {
            return sourceLayer;
        }
        return remappedLayers.computeIfAbsent(sourceLayer, this::createMappedLayer);
    }

    private RenderLayer createMappedLayer(RenderLayer sourceLayer) {
        Identifier texture = extractTextureId(sourceLayer);
        if (texture == null) {
            return sourceLayer;
        }
        return TEXTURE_LAYER_CACHE.computeIfAbsent(texture, this::createLayer);
    }

    private Identifier extractTextureId(RenderLayer sourceLayer) {
        if (!(sourceLayer instanceof RenderLayer.MultiPhase multiPhase)) {
            return null;
        }

        RenderLayer.MultiPhaseParameters phases =
                ((RenderLayerMultiPhaseAccessor) (Object) multiPhase).ethereal$getPhases();
        if (phases == null) {
            return null;
        }

        RenderPhase.TextureBase textureBase =
                ((MultiPhaseParametersAccessor) (Object) phases).ethereal$getTexture();
        if (textureBase == null) {
            return null;
        }

        return ((RenderPhaseTextureBaseAccessor) (Object) textureBase).ethereal$getId().orElse(null);
    }

    private RenderLayer createLayer(Identifier texture) {
        RenderLayer.MultiPhaseParameters parameters = RenderLayer.MultiPhaseParameters.builder()
                .program(HAND_OVERLAY_PROGRAM)
                .texture(new RenderPhase.Texture(texture, TriState.FALSE, false))
                .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                .cull(RenderPhase.DISABLE_CULLING)
                .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                .build(true);

        return RenderLayer.of(
                "hand_overlay_" + Math.abs(texture.hashCode()),
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS,
                1536,
                true,
                true,
                parameters
        );
    }

    private void updateShaderUniforms(float overlayAlpha) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        HAND_SHADER_STATE.use();

        if (HAND_SHADER_STATE.time != null) {
            HAND_SHADER_STATE.time.set((System.currentTimeMillis() % 1_000_000L) / 1000.0f);
        }

        float resolutionX = Math.max(1.0f, (float) mc.getWindow().getFramebufferWidth());
        float resolutionY = Math.max(1.0f, (float) mc.getWindow().getFramebufferHeight());
        if (HAND_SHADER_STATE.resolution != null) {
            HAND_SHADER_STATE.resolution.set(resolutionX, resolutionY);
        }

        double width = Math.max(1.0, mc.getWindow().getWidth());
        double height = Math.max(1.0, mc.getWindow().getHeight());
        float mouseX = (float) MathHelper.clamp(mc.mouse.getX() / width, 0.0, 1.0);
        float mouseY = (float) MathHelper.clamp(1.0 - (mc.mouse.getY() / height), 0.0, 1.0);
        if (HAND_SHADER_STATE.mouse != null) {
            HAND_SHADER_STATE.mouse.set(mouseX, mouseY);
        }

        if (HAND_SHADER_STATE.alpha != null) {
            HAND_SHADER_STATE.alpha.set(MathHelper.clamp(overlayAlpha, 0.0f, 1.0f));
        }
        if (HAND_SHADER_STATE.mode != null) {
            HAND_SHADER_STATE.mode.set((float) getModeOrdinal());
        }
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

    private float getOverlayStrength() {
        return MathHelper.clamp(alpha.getValue() / 255.0f, 0.0f, 1.0f);
    }
}
