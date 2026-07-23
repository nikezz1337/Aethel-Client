package dev.aethel.mixin;

import com.google.common.base.MoreObjects;
import dev.aethel.module.list.render.SwingAnimations;
import dev.aethel.module.list.render.ViewModel;
import dev.aethel.module.list.render.hand.HandModule;
import dev.aethel.module.list.render.hand.HandRenderer;
import dev.aethel.util.base.Instance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow private ItemStack mainHand;
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow private float prevEquipProgressOffHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private ItemStack offHand;

    @Shadow
    protected abstract void renderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light);

    @Shadow
    private static HeldItemRenderer.HandRenderType getHandRenderType(ClientPlayerEntity player) {
        throw new AssertionError();
    }

    @Overwrite
    public void renderItem(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, ClientPlayerEntity player, int light) {
        HandRenderer handRenderer = HandRenderer.getInstance();
        HandModule handModule = Instance.get(HandModule.class);
        boolean glow = handRenderer.isEnabled() && handModule != null;
        if (glow) handRenderer.captureSceneBeforeHands();

        float f = player.getHandSwingProgress(tickDelta);
        Hand hand = (Hand) MoreObjects.firstNonNull(player.preferredHand, Hand.MAIN_HAND);
        float g = player.getLerpedPitch(tickDelta);
        HeldItemRenderer.HandRenderType handRenderType = getHandRenderType(player);

        float j;
        float k;
        if (handRenderType.renderMainHand) {
            j = hand == Hand.MAIN_HAND ? f : 0.0F;
            k = 1.0F - MathHelper.lerp(tickDelta, this.prevEquipProgressMainHand, this.equipProgressMainHand);
            this.renderFirstPersonItem(player, tickDelta, g, Hand.MAIN_HAND, j, this.mainHand, k, matrices, vertexConsumers, light);
        }

        if (handRenderType.renderOffHand) {
            j = hand == Hand.OFF_HAND ? f : 0.0F;
            k = 1.0F - MathHelper.lerp(tickDelta, this.prevEquipProgressOffHand, this.equipProgressOffHand);
            this.renderFirstPersonItem(player, tickDelta, g, Hand.OFF_HAND, j, this.offHand, k, matrices, vertexConsumers, light);
        }

        vertexConsumers.draw();

        if (glow) {
            if (handModule != null) {
                handModule.updateActivity(f, player.isUsingItem(), (float) player.getVelocity().horizontalLength());
            }
            handRenderer.captureSceneAfterHands(handModule);
        }
    }

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    private void onRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ViewModel viewModel = Instance.get(ViewModel.class);
        if (viewModel != null && viewModel.isEnabled() && !item.isEmpty() && !item.contains(DataComponentTypes.MAP_ID)) {
            Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            viewModel.applyHandPosition(matrices, arm);
        }
    }

    @Redirect(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V"
            )
    )
    
    private void onSwingArm(HeldItemRenderer instance, float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm) {
        var swing = Instance.get(SwingAnimations.class);
        if (swing == null || !swing.isEnabled() || swing.hmiEnable.getValue() || !swing.swingEnabled.getValue()) {
            ((HeldItemRendererInvoker) instance).invokeSwingArm(swingProgress, equipProgress, matrices, armX, arm);
            return;
        }

        Arm expectedArm = MinecraftClient.getInstance().player != null
                ? MinecraftClient.getInstance().player.getMainArm() : Arm.RIGHT;
        if (arm != expectedArm) {
            ((HeldItemRendererInvoker) instance).invokeSwingArm(swingProgress, equipProgress, matrices, armX, arm);
            return;
        }

        float strength = (float) swing.swingStrength.getValue();
        float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        int i = arm == Arm.RIGHT ? 1 : -1;

        switch (swing.swingType.getValue()) {
            case "Smooth" -> {
                matrices.translate(i * 0.56f, -0.52f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45f + sin1 * -20f * strength)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * sin2 * -20f * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80f * strength));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45f));
            }
            case "Down" -> {
                matrices.translate(i * 0.56f, -0.32f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5 * strength));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100 * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155 * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100));
            }
            case "Poke" -> {
                float anim = (float) Math.sin(swingProgress * (float) (Math.PI / 2) * 2);
                float tilt = strength / 3f;
                matrices.translate(i * 0.56f, -0.52f, -0.72f);
                matrices.translate(0.0f, 0.0f, tilt * -anim);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(75f * i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-75f * (strength / 4f) * anim - 60f) * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-75f));
            }
            case "Static" -> {
                matrices.translate(i * 0.56f, -0.42f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -60f * strength));
                matrices.translate(0, -0.1, 0);
            }
            case "Feast" -> {
                matrices.translate(i * 0.56f, -0.32f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75 * i * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -65 * strength));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35 * i));
            }
            case "Akrien" -> {
                matrices.translate(i * 0.65f, -0.32f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5 * strength));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100 * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155 * strength));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 25 * strength));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -25 * strength));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin1 * 15 * strength));
                matrices.translate(sin2 * 0.18f * strength, sin2 * 0.59f * strength, 0);
            }
            case "Block" -> {
                if (swingProgress > 0) {
                    float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    matrices.translate(0.56f * i, equipProgress * -0.2f - 0.5f, -0.7f);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * i));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -85f * strength));
                    matrices.translate(-0.1f * i, 0.28f, 0.2f);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85f));
                } else {
                    applyEquipOffset(matrices, i, equipProgress);
                    applySwingOffset(matrices, i, swingProgress, strength);
                }
            }
            case "ToBack" -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                matrices.translate(0.65f * i, -0.45f, -0.9f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((-30f * (1f - g * strength) - 30f) * i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f * i));
            }
            case "SelfBack" -> {
                float anim = (float) Math.sin(swingProgress * (float) (Math.PI / 2) * 2);
                matrices.translate(0.65f * i, -0.3f, -0.8f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70 * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100 - (60 * strength) * anim));
            }
            case "Break" -> {
                matrices.translate(0.66f * i, -0.3f, -0.38f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270 * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * 10f * strength));
                matrices.scale(0.5f, 0.5f, 0.5f);
                matrices.translate(-0.1f * i, 0.2f, 0.0f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10.0f * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-105f * i));
            }
            case "DropDown" -> {
                float anim = (float) Math.sin(swingProgress * (float) (Math.PI / 2) * 2);
                applyEquipOffset(matrices, i, 0);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(80f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(12f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-12f * anim * strength));
            }
            case "Pander" -> {
                float panderAnim = MathHelper.sin(swingProgress * (float) Math.PI);
                float panderF = 1f - equipProgress;
                matrices.translate(i * 0.56f, -0.52f, -0.72f);
                matrices.translate((0.3f - panderAnim * 0.15f) * i, 0.2f - panderF * 0.12f, -0.15f - panderAnim * 0.13f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((76f - 10f * panderAnim) * i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-16f - 8f * panderAnim) * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-83f - 26f * panderAnim));
            }
            case "Slant" -> {
                float anim = (float) Math.sin(swingProgress * (float) (Math.PI / 2.0) * 2.0);
                float rotate = 35.0f * strength;
                matrices.translate(i * 0.56f, -0.52f, -0.72f);
                matrices.translate(0.0f, 0.0f, -0.3f * anim * strength);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(anim * -rotate));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(anim * rotate));
            }
            default ->
                ((HeldItemRendererInvoker) instance).invokeSwingArm(swingProgress, equipProgress, matrices, armX, arm);
        }
    }

    private void applyEquipOffset(MatrixStack matrices, int i, float equipProgress) {
        matrices.translate(i * 0.56f, -0.52f + equipProgress * -0.6f, -0.72f);
    }

    private void applySwingOffset(MatrixStack matrices, int i, float swingProgress, float strength) {
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        matrices.translate(0.56f * i, -0.52f, -0.72f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45f + f * -20f * strength)));
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * g * -20f * strength));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80f * strength));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45f));
    }

    
}
