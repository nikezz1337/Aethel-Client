package dev.aethel.mixin;

import dev.aethel.Aethel;
import dev.aethel.event.list.Event3DRender;
import dev.aethel.event.list.EventBeforeEntitiesRender;
import dev.aethel.event.list.EventAfterWorldRender;
import dev.aethel.module.list.render.BlockOverlay;
import dev.aethel.module.list.render.Removals;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.*;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "renderParticles", at = @At("HEAD"), cancellable = true)
    private void aethel$renderParticles(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog, CallbackInfo ci) {
        Removals removals = Aethel.getInstance().getModuleStorage().get(Removals.class);
        if (removals != null && removals.isEnabled("Частицы")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void aethel$renderWeather(FrameGraphBuilder frameGraphBuilder, Vec3d pos, float tickDelta, Fog fog, CallbackInfo ci) {
        Removals removals = Aethel.getInstance().getModuleStorage().get(Removals.class);
        if (removals != null && removals.isEnabled("Погода")) {
            ci.cancel();
        }
    }

    @Inject(method = "addWeatherParticlesAndSound", at = @At("HEAD"), cancellable = true)
    private void aethel$addWeatherParticlesAndSound(Camera camera, CallbackInfo ci) {
        Removals removals = Aethel.getInstance().getModuleStorage().get(Removals.class);
        if (removals != null && removals.isEnabled("Погода")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void aethel$renderClouds(FrameGraphBuilder frameGraphBuilder, Matrix4f positionMatrix, Matrix4f projectionMatrix, CloudRenderMode renderMode, Vec3d cameraPos, float ticks, int color, float cloudHeight, CallbackInfo ci) {
        Removals removals = Aethel.getInstance().getModuleStorage().get(Removals.class);
        if (removals != null && removals.isEnabled("Облака")) {
            ci.cancel();
            return;
        }

    }

    @Inject(method = "renderBlockEntities", at = @At("HEAD"), cancellable = true)
    private void aethel$renderBlockEntities(MatrixStack matrices, VertexConsumerProvider.Immediate mainConsumers, VertexConsumerProvider.Immediate translucentConsumers, Camera camera, float tickDelta, CallbackInfo ci) {
        Removals removals = Aethel.getInstance().getModuleStorage().get(Removals.class);
        if (removals != null && removals.isEnabled("Блок-сущности")) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
    private boolean disableBlockOutlineOnModule(boolean renderBlockOutline) {
        BlockOverlay overlay = Aethel.getInstance().getModuleStorage().get(BlockOverlay.class);
        if (overlay != null && overlay.isEnabled()) return false;
        return renderBlockOutline;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void aethel$onRenderHead(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        new EventBeforeEntitiesRender(positionMatrix, projectionMatrix, camera, tickCounter.getTickDelta(false)).post();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        new EventAfterWorldRender(positionMatrix, projectionMatrix, camera, tickCounter.getTickDelta(false)).post();
        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(positionMatrix);
        new Event3DRender(matrices, camera, tickCounter.getTickDelta(false), projectionMatrix).post();
    }
}
