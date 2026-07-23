package dev.aethel.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.event.list.Event3DRender;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.SliderSetting;
import dev.aethel.util.render.providers.ColorProvider;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.*;

@ModuleInformation(
    moduleName = "Wings",
    moduleCategory = ModuleCategory.RENDER,
    moduleDesc = "Рисует крылья за спиной"
)
public class Wings extends Module {

    private final Map<UUID, WingAnimationState> animationStates = new HashMap<>();

    private static final int[] RIB_BONE = {2, 4, 7, 9, 11};

    private static final WingPoint[] WING_SHAPE = WingBuilder.create()
        .add(0.08f, 0.10f, 0.88f)
        .add(0.28f, 0.34f, 0.78f)
        .add(0.56f, 0.82f, 0.62f)
        .add(0.86f, 0.30f, 0.52f)
        .add(1.14f, 0.46f, 0.40f)
        .add(1.24f, 0.04f, 0.30f)
        .add(1.02f, -0.18f, 0.28f)
        .add(1.18f, -0.64f, 0.22f)
        .add(0.86f, -0.46f, 0.20f)
        .add(0.80f, -0.98f, 0.14f)
        .add(0.54f, -0.74f, 0.16f)
        .add(0.30f, -1.16f, 0.12f)
        .add(0.10f, -0.54f, 0.18f)
        .build();

    private final BooleanSetting otherPlayers = new BooleanSetting("Other players", false);
    private final SliderSetting wingSize = new SliderSetting("Size", 1.0f, 0.8f, 1.5f, 0.05f);
    private final SliderSetting opacity = new SliderSetting("Opacity", 0.4f, 0.1f, 1.0f, 0.01f);

    @Subscribe
    public void onRender3D(Event3DRender event) {
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) return;

        MatrixStack matrixStack = event.getMatrixStack();
        float deltaTracker = event.getTickDelta();
        Vec3d camPos = event.getCamera().getPos();

        matrixStack.push();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        if (!mc.options.getPerspective().isFirstPerson() && mc.player.isAlive()
                && !mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            renderWings(matrixStack, mc.player, deltaTracker, camPos);
        }

        if (otherPlayers.getValue()) {
            for (Object entityObj : mc.world.getEntities()) {
                if (!(entityObj instanceof PlayerEntity targetPlayer) || targetPlayer == mc.player) continue;
                if (!targetPlayer.isAlive() || targetPlayer.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) continue;

                renderWings(matrixStack, targetPlayer, deltaTracker, camPos);
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        RenderSystem.blendFuncSeparate(
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ZERO);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        matrixStack.pop();
    }

    private void renderWings(MatrixStack matrixStack, PlayerEntity targetPlayer, float deltaTracker, Vec3d camPos) {
        WingAnimationState state = animationStates.computeIfAbsent(targetPlayer.getUuid(), uuid -> new WingAnimationState());

        double interpolatedX = MathHelper.lerp(deltaTracker, targetPlayer.prevX, targetPlayer.getX()) - camPos.getX();
        double interpolatedY = MathHelper.lerp(deltaTracker, targetPlayer.prevY, targetPlayer.getY()) - camPos.getY();
        double interpolatedZ = MathHelper.lerp(deltaTracker, targetPlayer.prevZ, targetPlayer.getZ()) - camPos.getZ();

        float currentBodyYaw = yaw(targetPlayer, deltaTracker, state);

        float move = MathHelper.clamp(targetPlayer.limbAnimator.getSpeed(deltaTracker), 0f, 1f);

        float targetWaterTransition = targetPlayer.isTouchingWater() ? 1f : 0f;
        state.waterAnim += (targetWaterTransition - state.waterAnim) * 0.08f;

        double motionX = targetPlayer.getX() - targetPlayer.prevX;
        double motionZ = targetPlayer.getZ() - targetPlayer.prevZ;

        float bodyYawRad = (float) Math.toRadians(currentBodyYaw);

        float targetForward = (float) (-(motionX * Math.sin(bodyYawRad)) + (motionZ * Math.cos(bodyYawRad)));
        targetForward = MathHelper.clamp(targetForward * 22f, -1f, 1f);
        state.forwardAnim += (targetForward - state.forwardAnim) * 0.08f;

        WingPose flightPose = pose(targetPlayer, deltaTracker);

        float waterPitch = 25f;
        float waterScale = 0.9f;
        float waterOpen = 0.85f;

        flightPose.pitchRotation += (waterPitch - flightPose.pitchRotation) * state.waterAnim;
        flightPose.scaleFactor += (waterScale - flightPose.scaleFactor) * state.waterAnim;
        flightPose.opennessMultiplier += (waterOpen - flightPose.opennessMultiplier) * state.waterAnim;

        float targetFlapStrength = flightPose.flapStrength + (move * 6f);
        state.flapAnim += (targetFlapStrength - state.flapAnim) * 0.08f;

        float flapAngle = (float) Math.sin((targetPlayer.age + deltaTracker) * flightPose.flapFrequency) * state.flapAnim;

        float spread = (8 + flapAngle + move * flightPose.motionSpreadBonus * 1.8f) * flightPose.opennessMultiplier;
        float motionSpread = state.forwardAnim * 16f;
        float dynamicSpread = spread + motionSpread;

        float fWingScale = wingSize.getFloatValue() * flightPose.scaleFactor;

        int highlightColor = ColorProvider.getThemeColor();
        int glowShade = ColorProvider.interpolateColor(highlightColor, ColorProvider.rgb(255, 255, 255), 0.28f);
        int coreShade = ColorProvider.interpolateColor(highlightColor, ColorProvider.rgb(255, 255, 255), 0.55f);

        matrixStack.push();
        matrixStack.translate(interpolatedX, interpolatedY + targetPlayer.getHeight() * 0.75f, interpolatedZ);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-currentBodyYaw));
        matrixStack.translate(0f, 0f, -0.23f);
        matrixStack.scale(fWingScale, fWingScale, fWingScale);

        if (flightPose.pitchRotation != 0f) {
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(flightPose.pitchRotation));
        }
        if (flightPose.rollRotation != 0f) {
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(flightPose.rollRotation));
        }

        renderWing(matrixStack, -1f, dynamicSpread, highlightColor, glowShade, coreShade, highlightColor, flightPose, state);
        renderWing(matrixStack, 1f, dynamicSpread, highlightColor, glowShade, coreShade, highlightColor, flightPose, state);

        matrixStack.pop();
    }

    private void renderWing(MatrixStack matrixStack, float direction, float wingOpenness,
                            int primaryColor, int glowColor, int coreColor, int outlineColor,
                            WingPose wingPose, WingAnimationState state) {
        matrixStack.push();

        matrixStack.translate(direction * wingPose.sideOffset, wingPose.sideVerticalOffset, wingPose.sideDepthOffset);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * wingOpenness));
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * wingPose.sideRollAngle));

        float dynamicPitch = wingPose.sidePitchAngle + (state.forwardAnim * 10f);
        int opacity = 220;

        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(dynamicPitch));

        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
        drawWing(matrixStack, direction, ColorProvider.setAlpha(glowColor, (int) (opacity * 0.25f)));
        drawWing(matrixStack, direction, ColorProvider.setAlpha(coreColor, (int) (opacity * 0.30f)));

        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        drawWing(matrixStack, direction, ColorProvider.setAlpha(primaryColor, opacity));

        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
        drawLines(matrixStack, direction,
                ColorProvider.setAlpha(outlineColor, (int) (opacity * 0.6f)),
                ColorProvider.setAlpha(glowColor, (int) (opacity * 0.20f)));

        matrixStack.pop();
    }

    private void drawWing(MatrixStack matrixStack, float direction, int surfaceColor) {
        Matrix4f transformMatrix = matrixStack.peek().getPositionMatrix();

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        int red = ColorProvider.red(surfaceColor);
        int green = ColorProvider.green(surfaceColor);
        int blue = ColorProvider.blue(surfaceColor);
        float baseAlphaValue = (ColorProvider.alpha(surfaceColor) / 255f) * opacity.getFloatValue();

        float centerX = 0f;
        float centerY = 0f;
        for (WingPoint point : WING_SHAPE) {
            centerX += point.x;
            centerY += point.y;
        }
        centerX /= WING_SHAPE.length;
        centerY /= WING_SHAPE.length;

        bufferBuilder.vertex(transformMatrix, direction * centerX, centerY, 0f)
                .color(red / 255f, green / 255f, blue / 255f, baseAlphaValue);

        float maxDistanceY = 0f;
        for (WingPoint point : WING_SHAPE) {
            maxDistanceY = Math.max(maxDistanceY, Math.abs(point.y - centerY));
        }

        for (WingPoint point : WING_SHAPE) {
            float vertexX = direction * point.x;
            float vertexY = point.y;
            float distanceFromCenter = Math.abs(point.y - centerY);
            float normalizedDistance = distanceFromCenter / maxDistanceY;
            float fadeFactor = normalizedDistance * normalizedDistance * (3f - 2f * normalizedDistance);
            float vertexAlpha = baseAlphaValue * (1f - fadeFactor);

            bufferBuilder.vertex(transformMatrix, vertexX, vertexY, 0f)
                    .color(red / 255f, green / 255f, blue / 255f, vertexAlpha);
        }

        WingPoint firstPoint = WING_SHAPE[0];
        float firstPointDistance = Math.abs(firstPoint.y - centerY);
        float firstPointNormalized = firstPointDistance / maxDistanceY;
        float firstPointFade = firstPointNormalized * firstPointNormalized * (3f - 2f * firstPointNormalized);
        float firstPointAlpha = baseAlphaValue * (1f - firstPointFade);

        bufferBuilder.vertex(transformMatrix, direction * firstPoint.x, firstPoint.y, 0f)
                .color(red / 255f, green / 255f, blue / 255f, firstPointAlpha);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private void drawLines(MatrixStack matrixStack, float direction, int outlineColor, int ribColor) {
        Matrix4f transformMatrix = matrixStack.peek().getPositionMatrix();

        RenderSystem.lineWidth(1.35f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        BufferBuilder outlineBuffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

        for (WingPoint point : WING_SHAPE) {
            outlineBuffer.vertex(transformMatrix, direction * point.x, point.y, 0f)
                    .color(ColorProvider.red(outlineColor) / 255f,
                           ColorProvider.green(outlineColor) / 255f,
                           ColorProvider.blue(outlineColor) / 255f,
                           ColorProvider.alpha(outlineColor) / 255f);
        }

        outlineBuffer.vertex(transformMatrix, direction * WING_SHAPE[0].x, WING_SHAPE[0].y, 0f)
                .color(ColorProvider.red(outlineColor) / 255f,
                       ColorProvider.green(outlineColor) / 255f,
                       ColorProvider.blue(outlineColor) / 255f,
                       ColorProvider.alpha(outlineColor) / 255f);

        BufferRenderer.drawWithGlobalProgram(outlineBuffer.end());
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        RenderSystem.lineWidth(0.9f);

        BufferBuilder ribsBuffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);

        for (int ribIndex : RIB_BONE) {
            WingPoint ribPoint = WING_SHAPE[ribIndex];

            ribsBuffer.vertex(transformMatrix, 0f, 0f, 0f)
                    .color(ColorProvider.red(ribColor) / 255f,
                           ColorProvider.green(ribColor) / 255f,
                           ColorProvider.blue(ribColor) / 255f,
                           Math.max(8, (int) (ColorProvider.alpha(ribColor) * 0.75f)) / 255f);

            ribsBuffer.vertex(transformMatrix, direction * ribPoint.x * 0.96f, ribPoint.y * 0.96f, 0f)
                    .color(ColorProvider.red(ribColor) / 255f,
                           ColorProvider.green(ribColor) / 255f,
                           ColorProvider.blue(ribColor) / 255f,
                           (int) (ColorProvider.alpha(ribColor) * ribPoint.alphaMul) / 255f);
        }

        BufferRenderer.drawWithGlobalProgram(ribsBuffer.end());
    }

    private float yaw(PlayerEntity targetPlayer, float deltaTracker, WingAnimationState state) {
        float targetAngle = MathHelper.lerpAngleDegrees(deltaTracker, targetPlayer.prevBodyYaw, targetPlayer.bodyYaw);

        if (!state.yawInitialized || targetPlayer.age < 2) {
            state.smoothYaw = targetAngle;
            state.yawInitialized = true;
            return state.smoothYaw;
        }

        float angleDelta = MathHelper.wrapDegrees(targetAngle - state.smoothYaw);
        angleDelta = MathHelper.clamp(angleDelta, -14f, 14f);
        state.smoothYaw = state.smoothYaw + angleDelta;

        return state.smoothYaw;
    }

    private WingPose pose(PlayerEntity targetPlayer, float deltaTracker) {
        float currentPitch = MathHelper.lerp(deltaTracker, targetPlayer.prevPitch, targetPlayer.getPitch());

        if (targetPlayer.isGliding()) {
            float glidingDuration = (float) targetPlayer.getGlidingTicks() + deltaTracker;
            float glideProgress = MathHelper.clamp(glidingDuration * glidingDuration / 100f, 0f, 1f);
            float pitchAngle = glideProgress * (-90f - currentPitch);

            return new WingPose(0.34f, 0.46f, 0f, 0f, pitchAngle, 0f,
                    0.76f, 0.92f, 0.10f, 0.58f,
                    0.05f, 0f, 0.06f, -5f, -2f, 0.13f);
        }

        if (targetPlayer.isSneaking()) {
            return new WingPose(0f, 0f, 0.96f, 0.10f, 18f, 0f,
                    1f, 1f, 0.18f, 4.5f,
                    0.06f, 0f, 0.02f, -11f, -4f, 0.12f);
        }

        return new WingPose(0f, 0f, 1.38f, 0.10f, 0f, 0f,
                1f, 1f, 0.18f, 4.5f,
                0.06f, 0f, 0.02f, -11f, -4f, 0.12f);
    }

    @Override
    public void onDisable() {
        animationStates.clear();
        super.onDisable();
    }

    private record WingPoint(float x, float y, float alphaMul) {}

    private static class WingAnimationState {
        float smoothYaw;
        boolean yawInitialized;
        float forwardAnim;
        float flapAnim;
        float waterAnim;
    }

    private static class WingPose {
        float preShiftY, preShiftZ;
        float anchorY, anchorZ;
        float pitchRotation, rollRotation;
        float opennessMultiplier, scaleFactor;
        float motionSpreadBonus, flapStrength;
        float sideOffset, sideVerticalOffset, sideDepthOffset;
        float sideRollAngle, sidePitchAngle, flapFrequency;

        WingPose(float preShiftY, float preShiftZ, float anchorY, float anchorZ,
                 float pitchRotation, float rollRotation,
                 float opennessMultiplier, float scaleFactor,
                 float motionSpreadBonus, float flapStrength,
                 float sideOffset, float sideVerticalOffset, float sideDepthOffset,
                 float sideRollAngle, float sidePitchAngle, float flapFrequency) {
            this.preShiftY = preShiftY;
            this.preShiftZ = preShiftZ;
            this.anchorY = anchorY;
            this.anchorZ = anchorZ;
            this.pitchRotation = pitchRotation;
            this.rollRotation = rollRotation;
            this.opennessMultiplier = opennessMultiplier;
            this.scaleFactor = scaleFactor;
            this.motionSpreadBonus = motionSpreadBonus;
            this.flapStrength = flapStrength;
            this.sideOffset = sideOffset;
            this.sideVerticalOffset = sideVerticalOffset;
            this.sideDepthOffset = sideDepthOffset;
            this.sideRollAngle = sideRollAngle;
            this.sidePitchAngle = sidePitchAngle;
            this.flapFrequency = flapFrequency;
        }
    }

    private static final class WingBuilder {
        final List<WingPoint> points = new ArrayList<>();

        public static WingBuilder create() {
            return new WingBuilder();
        }

        public WingBuilder add(float x, float y, float alpha) {
            points.add(new WingPoint(x, y, alpha));
            return this;
        }

        public WingPoint[] build() {
            return points.toArray(WingPoint[]::new);
        }
    }
}
