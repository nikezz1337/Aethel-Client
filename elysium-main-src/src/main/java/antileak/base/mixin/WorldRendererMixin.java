package antileak.base.mixin;

import antileak.base.api.QClient;
import antileak.base.api.events.EventInvoker;
import antileak.base.api.events.implement.Event3DRender;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.client.modules.impl.render.Chams;
import antileak.base.client.modules.impl.render.Removals;
import antileak.base.client.modules.impl.render.WorldTweaks;
import antileak.base.client.modules.impl.render.ShaderEsp;
import antileak.base.client.modules.impl.render.Sonar;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profilers;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin implements QClient {

    @Inject(method = "renderParticles", at = @At("HEAD"), cancellable = true)
    private void elysium$renderParticles(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;

        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("\u0427\u0430\u0441\u0442\u0438\u0446\u044b")) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void elysium$renderCustomSky(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        WorldTweaks tweaks = ModuleClass.INSTANCE != null ? ModuleClass.worldTweaks : null;
        if (tweaks != null) {
            tweaks.renderSky(camera);
        }
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void elysium$renderWeather(FrameGraphBuilder frameGraphBuilder, Vec3d pos, float tickDelta, Fog fog, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;

        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("\u041f\u043e\u0433\u043e\u0434\u0430")) {
            ci.cancel();
        }
    }

    @Inject(method = "addWeatherParticlesAndSound", at = @At("HEAD"), cancellable = true)
    private void elysium$addWeatherParticlesAndSound(Camera camera, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;

        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("\u041f\u043e\u0433\u043e\u0434\u0430")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void elysium$renderClouds(FrameGraphBuilder frameGraphBuilder, Matrix4f positionMatrix, Matrix4f projectionMatrix, CloudRenderMode renderMode, Vec3d cameraPos, float ticks, int color, float cloudHeight, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;

        Removals removals = ModuleClass.removals;
        WorldTweaks tweaks = ModuleClass.worldTweaks;
        if ((removals != null && removals.isEnabled("\u041e\u0431\u043b\u0430\u043a\u0430"))
                || (tweaks != null && tweaks.shouldCancelClouds())) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBlockEntities", at = @At("HEAD"), cancellable = true)
    private void elysium$renderBlockEntities(MatrixStack matrices, VertexConsumerProvider.Immediate mainConsumers, VertexConsumerProvider.Immediate translucentConsumers, Camera camera, float tickDelta, CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;

        Removals removals = ModuleClass.removals;
        if (removals != null && removals.isEnabled("\u0411\u043b\u043e\u043a-\u0441\u0443\u0449\u043d\u043e\u0441\u0442\u0438")) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void render(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        Sonar sonar = ModuleClass.INSTANCE != null ? ModuleClass.INSTANCE.sonar : null;
        boolean has3DListeners = EventInvoker.hasListeners(Event3DRender.class);
        boolean renderSonar = sonar != null && sonar.isEnable();
        if (!has3DListeners && !renderSonar) {
            return;
        }

        Profilers.get().swap("elysium_renderWorld");
        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(positionMatrix);
        if (has3DListeners) {
            new Event3DRender(matrices, positionMatrix, projectionMatrix, camera, tickCounter.getTickDelta(false)).call();
        }
        if (renderSonar) {
            sonar.renderFromMixin(positionMatrix, projectionMatrix, camera.getPos());
        }
    }

    @Inject(method = "drawEntityOutlinesFramebuffer", at = @At("HEAD"), cancellable = true)
    private void elysium$drawEntityOutlinesFramebuffer(CallbackInfo ci) {
        if (ModuleClass.INSTANCE == null) return;
        ShaderEsp shaderEsp = ModuleClass.INSTANCE.shaderEsp;
        if (shaderEsp != null && shaderEsp.isEnable()) {
            ci.cancel();
            return;
        }

        Chams chams = ModuleClass.INSTANCE.chams;
        if (chams != null && chams.shouldHideOutlineFramebuffer()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    public void onDrawBlockOutline(CallbackInfo ci) {
        if (ModuleClass.INSTANCE.blockOverlay.isEnable()) ci.cancel();
    }
}
