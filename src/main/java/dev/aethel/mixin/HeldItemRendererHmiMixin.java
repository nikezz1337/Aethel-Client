package dev.aethel.mixin;

import dev.aethel.module.list.render.SwingAnimations;
import dev.aethel.module.list.render.ViewModel;
import dev.aethel.util.base.Instance;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererHmiMixin {

    private boolean repPower = false;
    private float prevAge = 0.0F;
    private double previousRotation = 0.0;
    private float swingAngleY = 0.0F;
    private float swingAngleX = 0.0F;
    private float swingVelocityY = 0.0F;
    private float swingVelocityX = 0.0F;
    private float swingVelocityZ = 0.0F;
    private static final float GRAVITY = 0.1F;
    private static final float DAMPING = 0.88F;
    private static final float SENSITIVITY = 0.015F;
    private float vertAngleY = 0.0F;
    private float vertVelocityY = 0.0F;
    private float vertVelocityYSlime = 0.0F;
    private float vertAngleYSlime = 0.0F;
    private float riptideCounter = 0.0F;
    private float netherCounter = 0.0F;
    @Shadow
    private ItemStack mainHand;
    @Shadow
    @Final
    private MinecraftClient client;
    private float fallCounter = 0.0F;
    private float inWaterCounter = 0.0F;
    private float freezeCounter = 0.0F;
    private float clCount = 0.0F;
    private float crawlCount = 0.0F;
    private float directionalCrawlCount = 0.0F;
    private float climbCount = 0.0F;
    private float mouseHolding = 1.0F;
    private boolean isSwinging = false;
    private float swingProgress = 0.0F;
    private boolean isForward = false;
    private boolean isAttacking = false;
    private boolean left = false;
    private long lastNanoTime = System.nanoTime();
    private double deltaTime = 0.016;

    private double getDeltaTime() {
        long now = System.nanoTime();
        double dt = (now - lastNanoTime) / 1_000_000_000.0;
        lastNanoTime = now;
        deltaTime = Math.min(dt, 0.05);
        return deltaTime;
    }

    private float easeInOutBack(float x) {
        float c1 = 1.70158F;
        float c2 = c1 * 1.525F;
        return (float) (x < 0.5F
                ? Math.pow(2.0F * x, 2) * ((c2 + 1.0F) * 2.0F * x - c2) / 2.0F
                : (Math.pow(2.0F * x - 2.0F, 2) * ((c2 + 1.0F) * (x * 2.0F - 2.0F) + c2) + 2.0F) / 2.0F);
    }

    private float getAttackDamage(ItemStack stack) {
        AttributeModifiersComponent modifiers = stack.getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return 0.0F;
        float totalDamage = 0.0F;
        for (AttributeModifiersComponent.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() == EntityAttributes.ATTACK_DAMAGE.value()) {
                totalDamage += (float) entry.modifier().value();
            }
        }
        return totalDamage;
    }

    private boolean isSharpAnimation(SwingAnimations config) {
        return config != null && config.hmiAnimationType.is("Шарп");
    }

    private void altSwing(MatrixStack matrices, Arm arm, float swingProgress, ItemStack item) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * 3.14F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + f * 0.0F)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void onRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        SwingAnimations swings = Instance.get(SwingAnimations.class);
        if (swings == null || !swings.isEnabled() || !swings.hmiEnable.getValue()) return;

        boolean isMainHand = hand == Hand.MAIN_HAND;
        Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        float sideFactor = isMainHand ? 1.0F : -1.0F;

        renderCustomFirstPersonItem(player, tickDelta, pitch, hand, arm, sideFactor, swingProgress, item, equipProgress, matrices, vertexConsumers, light);

        ci.cancel();
    }

    private void renderCustomFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, Arm arm, float sideFactor, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        SwingAnimations config = Instance.get(SwingAnimations.class);
        if (config == null || !config.isEnabled() || !config.hmiEnable.getValue()) return;

        if (!player.isUsingSpyglass()) {
            float yaw = player.getYaw();
            double radians = Math.toRadians(yaw);
            double forwardX = -Math.sin(radians);
            double forwardZ = Math.cos(radians);
            Vec3d horizontalVelocity = player.getVelocity();
            double dotProduct = horizontalVelocity.x * forwardX + horizontalVelocity.z * forwardZ;
            double crossProduct = player.getVelocity().getHorizontal().x * forwardZ - horizontalVelocity.z * forwardX;
            float al;
            if (player.getPitch() != 0.0F) {
                al = 90.0F / player.getPitch() / 10.0F;
            } else {
                al = 1.0F;
            }
            if (al > 1.0F) al = 1.0F;
            if (al < 0.0F) al = 1.0F;

            boolean bl = hand == Hand.MAIN_HAND;
            matrices.push();
            matrices.push();

            var viewModelHmi = Instance.get(ViewModel.class);
            if (viewModelHmi != null && viewModelHmi.isEnabled()) {
                viewModelHmi.applyHandPosition(matrices, arm);
            }

            getDeltaTime();
            double tt = deltaTime * 30.0;

            float smoothness = MathHelper.clamp((float) config.hmiSmoothness.getValue(), 0.35F, 2.5F);
            float hmiProgress = (float) Math.pow(MathHelper.clamp(swingProgress, 0.0F, 1.0F), smoothness);
            float swing_rot = hmiProgress < 0.6
                    ? MathHelper.sin(MathHelper.clamp(hmiProgress, 0.0F, 0.12506F) * 12.56F)
                    : MathHelper.sin(MathHelper.clamp(hmiProgress, 0.62532F, 0.75038F) * 12.56F);
            float swing = MathHelper.sin(hmiProgress * 3.14F);
            swing = easeInOutBack(swing);
            boolean sharpSword = item.isIn(ItemTags.SWORDS) && isSharpAnimation(config);

            if ((item.isOf(Items.EXPERIENCE_BOTTLE) || item.isOf(Items.WIND_CHARGE) || item.isOf(Items.EGG) || item.isOf(Items.ENDER_EYE) || item.isOf(Items.SNOWBALL) || item.getItem() instanceof SplashPotionItem || item.getItem() instanceof LingeringPotionItem) && player.getOffHandStack().isEmpty() && item.getUseAction() != UseAction.SPEAR && !item.isOf(Items.FIRE_CHARGE) && !player.isSwimming() && !player.isCrawling() && !player.isClimbing()) {
                if (player.getMainArm() == Arm.LEFT) bl = !bl;
                matrices.push();
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25.0F * sideFactor));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * sideFactor * swing));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
                matrices.translate(-0.15 * sideFactor, 0.1, 0.1);
                matrices.translate(0.0F, -0.55 * swing, 0.4 * swing * 3.14F);
                ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, 0.0F, arm.getOpposite());
                matrices.pop();
            }

            if (client.options.attackKey.isPressed() && !isAttacking && swingProgress == 0.0F) {
                left = !left;
            }

            if (!item.isEmpty()) {
                if (player.getMainArm() == Arm.LEFT) bl = !bl;

                if ((left || item.isIn(ItemTags.AXES) || item.getUseAction() == UseAction.SPEAR || item.getUseAction() == UseAction.BLOCK) && !item.isIn(ItemTags.SHOVELS)) {
                    if (sharpSword) {
                        matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.5F * swing);
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                    } else if (!item.isIn(ItemTags.SWORDS) && !item.isIn(ItemTags.AXES)) {
                        if (item.getUseAction() == UseAction.SPEAR) {
                            matrices.translate(0.0F, 0.0F, 0.45 * swing_rot);
                            matrices.translate(-0.25F * sideFactor * swing, -0.35 * swing_rot, -0.6 * swing);
                            matrices.translate(0.0F, 0.1 * swing, 0.0F);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30.0F * swing_rot * sideFactor));
                        } else if (item.getUseAction() != UseAction.BLOCK) {
                            matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.5F * swing);
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                        } else {
                            matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.2 * swing);
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-10.0F * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(20.0F * swing));
                        }
                    } else {
                        matrices.translate(0.8 * sideFactor * swing_rot, 0.3 * swing_rot, -0.5F * swing);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-20.0F * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0F * swing_rot * sideFactor));
                        if (item.isIn(ItemTags.SWORDS)) {
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                        } else {
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(30.0F * swing));
                        }
                    }
                } else if (!item.isIn(ItemTags.SHOVELS)) {
                    if (sharpSword) {
                        matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.5F * swing);
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                    } else if (item.isIn(ItemTags.SWORDS)) {
                        matrices.translate(-0.55 * sideFactor * swing_rot, -0.8 * swing_rot, -0.77 * swing);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(5.0F * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(70.0F * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(50.0F * swing));
                    } else {
                        matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.5F * swing);
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                    }
                } else if (item.isIn(ItemTags.SHOVELS)) {
                    matrices.translate(0.0F, 0.15 * swing_rot, -0.25F * swing_rot);
                    matrices.translate(0.0F, 0.0F, -0.2 * swing);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F * swing_rot));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
                }
            } else if (Block.getBlockFromItem(item.getItem()) != Blocks.AIR && item.getUseAction() != UseAction.EAT && !item.isEnchantable() && item.getUseAction() != UseAction.BOW && item.getUseAction() != UseAction.SPYGLASS && getAttackDamage(item) == 0.0F && item.getUseAction() != UseAction.BLOCK && !item.isOf(Items.WARPED_FUNGUS_ON_A_STICK) && !item.isOf(Items.CARROT_ON_A_STICK) && !item.isOf(Items.FISHING_ROD) && !item.isOf(Items.SHEARS)) {
                swingProgress = (float) (swingProgress * 1.2);
                if (swingProgress > 1.0F) swingProgress = 0.0F;
            } else if (!item.isIn(ItemTags.SHOVELS)) {
                swingProgress = (float) (swingProgress * 1.5);
                if (swingProgress > 1.0F) swingProgress = 0.0F;
            }

            if (player.getVelocity().length() >= 0.08) {
                crawlCount = (float) (crawlCount + 0.1 * player.getVelocity().length() * 2.0 * tt);
                directionalCrawlCount = (float) (directionalCrawlCount + 0.1 * dotProduct * 4.0 * tt);
                directionalCrawlCount = (float) (directionalCrawlCount + (dotProduct > 0.0 ? 0.1 * Math.abs(crossProduct) * 4.0 * tt : 0.1 * Math.abs(crossProduct) * -1.0 * 4.0 * tt));
            }

            if (player.getVelocity().getY() > 0.0) climbCount = (float) (climbCount + 0.1 * tt);
            if (player.getVelocity().getY() < 0.0) climbCount = (float) (climbCount - 0.1 * tt);

            if ((player.isCrawling() || player.isClimbing() && !player.isOnGround() && Math.abs(player.getVelocity().getY()) > 0.0) && !player.isUsingItem() && swingProgress == 0.0F) {
                clCount = (float) (clCount + 0.1 * tt);
                if (clCount > 1.0F) clCount = 1.0F;
                if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0F * clCount));
                }
            } else {
                clCount = (float) (clCount * Math.pow(0.88, tt));
            }

            if (swingProgress == 0.0F) {
                matrices.translate(bl ? player.getPitch() / 650.0F * clCount * -1.0F : player.getPitch() / 650.0F * clCount, 0.0F, 0.0F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(player.getPitch() * clCount));
            }

            if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
                matrices.translate(0.0F, 0.0F, player.getPitch() / 120.0F * clCount);
            } else if (swingProgress == 0.0F) {
                matrices.translate(0.0F, 0.0F, player.getPitch() / 80.0F * clCount);
            }

            if (player.isClimbing() && !player.isOnGround() && !item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN) && !player.isUsingItem()) {
                matrices.translate(0.0F, 0.1, -0.2);
            }

            if ((player.isInFluid() || player.inPowderSnow) && !player.isSwimming() && !player.isSubmergedInWater()) {
                inWaterCounter = (float) (inWaterCounter + 0.1 * tt);
                if (inWaterCounter >= 1.0F) inWaterCounter = 1.0F;
            } else {
                inWaterCounter = (float) (inWaterCounter * Math.pow(0.88, tt));
            }

            if (player.inPowderSnow && player.getFreezingScale() > 0.1) {
                freezeCounter = (float) (freezeCounter + 0.1 * tt);
            } else {
                freezeCounter = (float) (freezeCounter * Math.pow(0.88, tt));
            }

            matrices.translate(0.0F, 0.02 * inWaterCounter, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8.0F * sideFactor * inWaterCounter));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.3F * MathHelper.sin(freezeCounter * 5.0F)));

            if (player.getVelocity().getY() < -0.85 && item.isOf(Items.MACE) && player.getMainHandStack() == item) {
                fallCounter = (float) (fallCounter + 0.1 * tt);
                if (fallCounter >= 1.0F) fallCounter = 1.0F;
            } else {
                fallCounter = (float) (fallCounter * Math.pow(0.88, tt));
            }

            if (bl) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F * fallCounter));
                matrices.translate(0.0F, -0.2 * fallCounter, 0.0F);
            }

            vertAngleY = (float) (vertAngleY + player.getVelocity().getY() * 0.015 * tt);
            vertAngleY = (float) (vertAngleY - (0.1F * vertAngleY) * tt);
            vertAngleY = (float) (vertAngleY * Math.pow(0.88, tt));
            vertVelocityYSlime = (float) (vertVelocityYSlime + player.getVelocity().getY() * 0.015 * tt);
            vertVelocityYSlime = (float) (vertVelocityYSlime - (0.1F * vertAngleYSlime) * tt);
            vertVelocityYSlime = (float) (vertVelocityYSlime * Math.pow(0.88, tt));
            vertAngleYSlime = (float) (vertAngleYSlime + vertVelocityYSlime * tt);
            matrices.translate(0.0F, vertAngleY * -1.0F, 0.0F);
            matrices.translate(0.0F, Math.sin(player.age * 0.1) * 0.007 * sideFactor, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.15F * MathHelper.sin(player.age * 0.15F) * sideFactor));

            if (!item.isEmpty() || player.isCrawling() || player.isClimbing() && !player.isOnGround() || player.isSwimming()) {
                if (player.getMainArm() == Arm.LEFT) bl = !bl;
                if (item.getUseAction() == UseAction.BLOCK) {
                    matrices.translate(0.0F, 0.0F, 0.0F);
                } else {
                    matrices.translate(0.0F, -0.1, 0.1);
                }
            }

            if (item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN) || item.isIn(ItemTags.HANGING_SIGNS)) {
                matrices.translate(0.0F, 0.1, 0.0F);
                if (player.isSwimming()) matrices.translate(0.0F, -0.1, 0.1);
            }

            if (player.isSwimming() && swingProgress == 0.0F) {
                double distance = crawlCount;
                double swingAmplitude = 1.5;
                double frequency = 2.0;
                double s = distance * frequency;
                double handRotation = Math.sin(s) * swingAmplitude;
                double smoothRotation = handRotation * 0.8 + previousRotation * 0.2;
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) (bl ? smoothRotation : -smoothRotation)));
                matrices.translate(0.0F, 0.0F, smoothRotation * 0.2);
                double k = crawlCount * 2.0;
                double a = Math.cos(k);
                double b = a;
                if (a <= 0.0) b = a * 0.5;
                matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) (bl ? b * 30.0 : b * 30.0 * -1.0)));
                matrices.translate(0.0F, 0.0F, a * 0.2);
                if (item.isEmpty() && !bl && !player.isInvisible()) {
                    matrices.translate(1.0F * sideFactor, 0.0F - equipProgress * 0.3, 0.3);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                    altSwing(matrices, arm, swingProgress, item);
                    matrices.scale(0.9F, 0.9F, 0.9F);
                    ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                }
                previousRotation = smoothRotation;
            }

            if ((player.isClimbing() && !player.isOnGround() || player.isCrawling() && swingProgress == 0.0F) && !player.isUsingItem()) {
                double s = climbCount;
                float v = (float) player.getVelocity().getY();
                float a = MathHelper.cos((float) s * 2.0F);
                if (player.isClimbing()) {
                    if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0F * a * sideFactor));
                    } else {
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(1.0F * a * sideFactor));
                    }
                }

                if (player.isCrawling() && !player.isUsingItem() && swingProgress == 0.0F) {
                    float crawlProgress = MathHelper.sin(directionalCrawlCount * 4.0F * mouseHolding);
                    float upAndDown = MathHelper.cos(directionalCrawlCount * 4.0F * mouseHolding);
                    if (item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN)) {
                        crawlProgress *= 0.14F;
                        upAndDown *= 0.14F;
                    }
                    matrices.translate(0.2 * crawlProgress, 0.3 * crawlProgress * sideFactor, -0.2 * crawlProgress * sideFactor * al);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * crawlProgress));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.clamp(20.0F * upAndDown * sideFactor, 0.0F, 20.0F)));
                }

                if (item.isEmpty() && !bl && !player.isInvisible() && (!player.isOnGround() && player.isClimbing() || player.isCrawling())) {
                    matrices.translate(1.0F * sideFactor, 0.0F - equipProgress * 0.3, 0.3);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                    altSwing(matrices, arm, swingProgress, item);
                    matrices.scale(0.9F, 0.9F, 0.9F);
                    ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                }
            }

            if (item.isEmpty()) {
                if (bl && !player.isInvisible()) {
                    if ((player.isOnGround() || !player.isClimbing()) && !player.isSwimming() && !player.isCrawling()) {
                        if (player.getMainArm() == Arm.LEFT) bl = !bl;
                        matrices.translate(0.0F, 0.2 * swing_rot, 0.15 * swing_rot);
                        matrices.translate(0.1 * sideFactor * swing, 0.15 * swing, -0.45 * swing);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0F * swing * sideFactor));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * swing));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10.0F * swing_rot * sideFactor));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F * swing_rot));
                        ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                    } else {
                        matrices.translate(1.0F * sideFactor, 0.0F - equipProgress * 0.3, 0.3);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                        altSwing(matrices, arm, swingProgress, item);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                    }
                }
            } else if (item.contains(DataComponentTypes.MAP_ID)) {
                if (bl && mainHand.isEmpty()) {
                    matrices.translate(0.0F, 0.1, 0.0F);
                    ((HeldItemRendererInvoker) this).invokeRenderMapInBothHands(matrices, vertexConsumers, light, pitch, equipProgress, swingProgress);
                } else {
                    matrices.translate(bl ? -0.1 : 0.1, 0.1, 0.0F);
                    ((HeldItemRendererInvoker) this).invokeRenderMapInOneHand(matrices, vertexConsumers, light, equipProgress, arm, swingProgress, item);
                }
            } else if (item.getUseAction() == UseAction.CROSSBOW) {
                matrices.push();
                boolean bl2 = CrossbowItem.isCharged(item);
                boolean bl3 = arm == Arm.RIGHT;
                int i = bl3 ? 1 : -1;
                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    ((HeldItemRendererInvoker) this).invokeApplyEquipOffset(matrices, arm, equipProgress);
                    matrices.translate(i * -0.4785682F, -0.24387F, 0.05731531F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 65.3F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 9.785F));
                    float f = (float) item.getMaxUseTime(player) - ((float) player.getItemUseTimeLeft() - tickDelta + 1.0F);
                    float g = f / (float) CrossbowItem.getPullTime(item, player);
                    if (g > 1.0F) g = 1.0F;
                    if (g > 0.1F) {
                        float h = MathHelper.sin((f - 0.1F) * 1.3F);
                        float j = g - 0.1F;
                        float k = h * j;
                        matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                    }
                    matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                    matrices.scale(1.0F, 1.0F, 1.0F);
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(i * 45.0F));
                } else {
                    ((HeldItemRendererInvoker) this).invokeSwingArm(swingProgress, equipProgress, matrices, i, arm);
                    if (bl2 && swingProgress < 0.001F && bl) {
                        matrices.translate(i * -0.341864F, 0.0F, 0.0F);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 10.0F));
                    }
                }
                matrices.translate(0.0F, 0.0F, -1.0F);
                matrices.translate(-0.45 * i, 0.45, 1.7);
                matrices.translate(1.0F * sideFactor, 0.0F - equipProgress * 0.3, 0.3);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                altSwing(matrices, arm, swingProgress, item);
                matrices.scale(0.9F, 0.9F, 0.9F);
                ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                matrices.translate(-0.25F * i, 1.25F, 0.05);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90 * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(77.0F));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(85 * i));
                matrices.scale(1.2F, 1.2F, 1.2F);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
                matrices.translate(0.0F, -0.15, 0.15);
                ((HeldItemRendererInvoker) this).invokeRenderItem(player, item, bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl3, matrices, vertexConsumers, light);
                matrices.pop();
                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    float f = (float) item.getMaxUseTime(player) - ((float) player.getItemUseTimeLeft() - tickDelta + 1.0F);
                    float g = f / (float) CrossbowItem.getPullTime(item, player);
                    if (g > 1.0F) g = 1.0F;
                    if (g > 0.1F) {
                        float h = MathHelper.sin((f - 0.1F) * 1.3F);
                        float j = g - 0.1F;
                        float k = h * j;
                        matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                    }
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(g <= 0.2 ? 75.0F * g * 5.0F * i : 75 * i));
                    matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(10.0F * g * 1.5F));
                    matrices.translate(-0.37 * i, 0.0F, 0.6);
                    matrices.translate(0.15 * g * i, 0.0F, 0.0F);
                    ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
                }
            } else {
                boolean bl2 = arm == Arm.RIGHT;
                int l = bl2 ? 1 : -1;
                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    switch (item.getUseAction()) {
                        case NONE:
                            ((HeldItemRendererInvoker) this).invokeApplyEquipOffset(matrices, arm, equipProgress);
                            break;
                        case EAT:
                        case DRINK:
                            float u = (float) item.getMaxUseTime(player) - ((float) player.getItemUseTimeLeft() - tickDelta + 1.0F);
                            float y = u / 5.0F;
                            if (y > 1.0F) y = 1.0F;
                            float q = MathHelper.sin(u / 2.0F * 3.14F);
                            q /= 10.0F;
                            matrices.translate(1 * l, 0.1, 0.3);
                            matrices.translate(0.2 * l * y, -0.7 * y, -0.2 * y);
                            matrices.translate(0.0F, -0.2 * q, -0.2 * q);
                            matrices.translate(0.0F, 0.1 * easeInOutBack(MathHelper.sin(y * 3.14F)), 0.0F);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                            altSwing(matrices, arm, swingProgress, item);
                            matrices.scale(0.9F, 0.9F, 0.9F);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * y * l));
                            ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, swingProgress, arm);
                            break;
                        case BLOCK:
                            float k = (float) item.getMaxUseTime(player) - ((float) player.getItemUseTimeLeft() - tickDelta + 1.0F);
                            float s = k / 4.0F;
                            float s2 = k / 6.0F;
                            if (s > 1.0F) s = 1.0F;
                            if (s2 > 1.0F) s2 = 1.0F;
                            matrices.translate(0.0F, -0.2, 0.0F);
                            matrices.translate(1 * l, 0.0F, 0.3);
                            matrices.translate(0.7 * s * l, 0.0F, -1.3 * s);
                            matrices.translate(-0.2 * l * s2, 0.0F, 0.0F);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) (10.0 * Math.sin(s2 * 3.14))));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0F * s * l));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(5 * l * s));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F * s));
                            matrices.translate(0.0F, 0.0F, -0.2 * s);
                            altSwing(matrices, arm, swingProgress, item);
                            matrices.scale(0.9F, 0.9F, 0.9F);
                            ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, swingProgress, arm);
                            matrices.translate(0.35 * l, -0.13, -0.12);
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(10.0F * l));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * l));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.0F));
                            matrices.translate(-0.2 * l, -0.04, 0.15);
                            matrices.scale(1.0F, 1.0F, 1.0F);
                            break;
                        case BOW:
                            matrices.push();
                            if (player.getMainArm() == Arm.LEFT) bl = !bl;
                            float m1 = (float) item.getMaxUseTime(player) - ((float) player.getItemUseTimeLeft() - tickDelta + 1.0F);
                            float f1 = m1 / 20.0F;
                            float f = (f1 * f1 + f1 * 2.0F) / 3.0F;
                            if (f1 > 1.0F) f1 = 1.0F;
                            float smoothPull = f1 * f1 * (3.0F - 2.0F * f1);
                            matrices.translate(bl ? -0.1 : 0.1, 0.0, smoothPull * 0.12);
                            ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                            matrices.pop();
                            matrices.translate(bl ? -0.5 : 0.5, -0.45, 0.1);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotation(0.3F));
                            if (bl) {
                                matrices.multiply(RotationAxis.NEGATIVE_Z.rotation(-0.3F));
                                matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(1.0F));
                            } else {
                                matrices.multiply(RotationAxis.POSITIVE_Z.rotation(-0.3F));
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(1.0F));
                            }
                            ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
                            if (bl) {
                                matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(2.5F));
                            } else {
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotation(2.5F));
                            }
                            matrices.translate(bl ? -0.65 : 0.65, -0.35, 0.27);
                            matrices.pop();
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(75.0F));
                            matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(-15 * l));
                            matrices.translate(0.8 * l, 0.0F - equipProgress * 0.3F, -0.1);
                            float pullOffset = smoothPull * 0.03F;
                            matrices.translate(0.0, 0.0, pullOffset);
                            matrices.push();
                            break;
                        case SPEAR:
                            if (player.getOffHandStack().isEmpty() && !player.isCrawling() && !player.isSwimming() && !player.isClimbing()) {
                                matrices.push();
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25 * l));
                                matrices.translate(-0.15 * l, 0.1, 0.1);
                                ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm.getOpposite());
                                matrices.pop();
                            }
                            float m = (float) item.getMaxUseTime(player) - ((float) player.getItemUseTimeLeft() - tickDelta + 1.0F);
                            f = m / 10.0F;
                            if (f > 1.0F) f = 1.0F;
                            if (f > 0.1F) {
                                float g = MathHelper.sin((m - 0.1F) * 1.3F);
                                float h = f - 0.1F;
                                float j = g * h;
                                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                            }
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25 * l));
                            matrices.translate(0.2 * l, 0.0F, 0.8);
                            ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0F));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-65 * l));
                            matrices.translate(0.65F * l, -1.0F, -0.6);
                            break;
                        case BRUSH:
                            float f5 = (float) (player.getItemUseTimeLeft() % 10);
                            float g5 = f5 - tickDelta + 1.0F;
                            float h5 = 1.0F - g5 / 10.0F;
                            float n = -15.0F + 75.0F * MathHelper.cos(h5 * 2.0F * (float) Math.PI);
                            float z = (float) item.getMaxUseTime(player) - ((float) player.getItemUseTimeLeft() - tickDelta + 1.0F);
                            float x = z / 4.0F;
                            if (x > 1.0F) x = 1.0F;
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25 * l * x));
                            matrices.translate(0.3F * l * x, 0.3 * x, 0.1 * x);
                            if (x == 1.0F) {
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n / 20.0F));
                            }
                            ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                            break;
                        case BUNDLE:
                            matrices.translate(1 * l, 0.0F - equipProgress * 0.3, 0.3);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                            altSwing(matrices, arm, swingProgress, item);
                            matrices.scale(0.9F, 0.9F, 0.9F);
                            ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                    }
                } else if (player.isUsingRiptide() && item.getUseAction() == UseAction.SPEAR) {
                    riptideCounter = (float) (riptideCounter + 0.15 * tt);
                    float m = (float) item.getMaxUseTime(player) - ((float) player.getItemUseTimeLeft() - tickDelta + 1.0F);
                    float f = m / 10.0F;
                    if (f > 1.0F) f = 1.0F;
                    if (f > 0.1F) {
                        float g = MathHelper.sin((m - 0.1F) * 1.3F);
                        float h = f - 0.1F;
                        float j = g * h;
                        matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                    }
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F - riptideCounter * 2.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25 * l));
                    matrices.translate(0.2 * l, 0.0F, 0.75F);
                    matrices.translate(0.0F, 0.0F, 0.01 * MathHelper.sin(riptideCounter * 6.28F));
                    ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equipProgress, swingProgress, arm);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-65 * l));
                    matrices.translate(0.65F * l, -1.0F, -0.6);
                } else {
                    riptideCounter = 0.0F;
                    if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN) && !item.isIn(ItemTags.HANGING_SIGNS)) {
                        if (item.getUseAction() == UseAction.BLOCK) matrices.translate(0.0F, -0.2, 0.0F);
                    } else {
                        matrices.translate(0.1 * l, 0.0F, -0.1);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
                    }
                    matrices.translate(1 * l, 0.0F - equipProgress * 0.3, 0.3);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                    altSwing(matrices, arm, swingProgress, item);
                    matrices.scale(0.9F, 0.9F, 0.9F);
                    ((HeldItemRendererInvoker) this).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm);
                }

                matrices.translate(-0.3 * l, 0.65, -0.1);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-65 * l));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
                if (item.isIn(ItemTags.WOOL_CARPETS)) matrices.translate(0.2 * l, -0.1, 0.0F);

                if (Block.getBlockFromItem(item.getItem()) != Blocks.AIR && item.getUseAction() != UseAction.EAT && !item.isOf(Items.WARPED_FUNGUS_ON_A_STICK) && !item.isOf(Items.CARROT_ON_A_STICK) && !item.isOf(Items.FISHING_ROD) && !item.isOf(Items.SHEARS)) {
                    if (item.getName().toString().toLowerCase().contains("TORCH".toLowerCase())) {
                        matrices.scale(1.5F, 1.5F, 1.5F);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(25 * l));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
                        matrices.translate(0.2 * l, 0.2, 0.05);
                    } else if ((item.isOf(Items.STRING) || item.isOf(Items.REDSTONE) || item.isOf(Items.LEVER) || item.isOf(Items.TRIPWIRE_HOOK)) && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES)) {
                        matrices.translate(0.0F, 0.0F, -0.1);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(5 * l));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
                    } else if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN) && !item.isIn(ItemTags.HANGING_SIGNS)) {
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(25 * l));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
                        matrices.translate(0.2 * l, 0.2, 0.05);
                    } else {
                        float dt = (float) (deltaTime * 30.0);
                        float yawDelta = player.prevHeadYaw - player.getHeadYaw();
                        float pitchDelta = player.prevPitch - player.getPitch();
                        swingVelocityY += yawDelta * 0.015F * dt;
                        swingVelocityY += swingProgress * 2.0F * dt;
                        swingVelocityX += pitchDelta * 0.015F * dt;
                        swingVelocityY -= 0.1F * swingAngleY * dt;
                        swingVelocityX -= 0.1F * swingAngleX * dt;
                        swingVelocityY = (float) (swingVelocityY * Math.pow(0.88, dt));
                        swingVelocityX = (float) (swingVelocityX * Math.pow(0.88, dt));
                        swingAngleY += swingVelocityY * dt;
                        swingAngleX += swingVelocityX * dt;
                        double currentSpeed = player.getVelocity().length();
                        swingVelocityZ = (float) (swingVelocityZ + (bl ? currentSpeed * -1.0 * 15.0 - swingVelocityZ : currentSpeed * 15.0 - swingVelocityZ) * 0.1 * dt);
                        if ((currentSpeed > 0.09 && player.isOnGround() || player.isSwimming() || player.isClimbing() && !player.isOnGround()) && client.options.getBobView().getValue()) {
                            Random random = new Random();
                            boolean randomBoolean = random.nextBoolean();
                            swingVelocityY += (float) (randomBoolean ? -5.5 * currentSpeed * dt : 5.5 * currentSpeed * dt);
                        }
                        matrices.translate(0.0F, 0.0F, -0.1);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(35 * l + swingAngleY));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F + swingAngleX));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l + swingVelocityZ));
                        if (item.isIn(ItemTags.HANGING_SIGNS)) {
                            matrices.translate(0.0F, -0.1, 0.0F);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45 * l));
                        }
                        matrices.translate(0.3 * l, -0.35, 0.0F);
                        matrices.translate(0.0F, 0.0F, 0.1);
                        matrices.scale(1.5F, 1.5F, 1.5F);
                    }
                } else {
                    if ((!item.isIn(ItemTags.SHOVELS)) && item.getUseAction() != UseAction.BOW && item.getUseAction() != UseAction.SPYGLASS && getAttackDamage(item) == 0.0F && item.getUseAction() != UseAction.BLOCK && !item.isOf(Items.WARPED_FUNGUS_ON_A_STICK) && !item.isOf(Items.CARROT_ON_A_STICK) && !item.isOf(Items.FISHING_ROD) && !item.isOf(Items.SHEARS) && !item.isIn(ItemTags.HOES)) {
                        if (item.getUseAction() == UseAction.BRUSH) {
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(25.0F));
                            matrices.translate(bl ? 0.0 : 0.35, bl ? 0.0 : 0.25, bl ? 0.0 : 0.37);
                            if (!bl) matrices.scale(0.75F, 0.75F, 0.75F);
                            matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(-75 * l));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(35.0F));
                            matrices.translate(bl ? -0.05 : 0.85, bl ? 0.0 : 0.05, bl ? 0.08 : -0.2);
                        } else {
                            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(5 * l));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
                            matrices.translate(0.0F, -0.05, -0.1);
                            matrices.scale(0.7F, 0.7F, 0.7F);
                        }
                        if (item.isOf(Items.FEATHER) || item.isOf(Items.SLIME_BALL) || item.isOf(Items.PUFFERFISH) || item.isOf(Items.NETHER_STAR) || item.isOf(Items.END_CRYSTAL)) {
                            vertVelocityYSlime = (float) (vertVelocityYSlime + swingProgress * 0.03 * deltaTime * 30.0);
                            if ((player.getVelocity().length() > 0.09 && player.isOnGround() || player.isSwimming() || player.isCrawling() || player.isClimbing() && !player.isOnGround()) && client.options.getBobView().getValue()) {
                                Random random = new Random();
                                boolean randomBoolean = random.nextBoolean();
                                vertVelocityYSlime += (float) (-0.05 * player.getVelocity().length() * deltaTime * 30.0);
                            }
                            matrices.scale(1.0F, 1.0F + vertAngleYSlime * -2.0F, 1.0F);
                        }
                    } else if (item.getUseAction() == UseAction.BLOCK && item.getUseAction() != UseAction.SPEAR) {
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(160 * l));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60 * l));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
                        matrices.scale(0.75F, 0.75F, 0.75F);
                        matrices.translate(0.15 * l, bl ? 0.35 : 0.45, bl ? -0.15 : -0.1);
                        matrices.translate(0.17 * l, 0.0F, 0.3);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90 * l));
                    } else if (item.getUseAction() == UseAction.SPEAR) {
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75 * l));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45 * l));
                        matrices.translate(-0.3F * l, 0.0F, 0.0F);
                    } else if (item.getUseAction() != UseAction.SPEAR) {
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75 * l));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45 * l));
                    }
                    if (item.getUseAction() != UseAction.BLOCK) matrices.scale(1.2F, 1.2F, 1.2F);
                    if (item.getUseAction() == UseAction.BOW && !player.isUsingItem()) matrices.translate(-0.1 * l, -0.2, 0.0F);
                    if (item.isOf(Items.MACE)) {
                        matrices.translate(0.1 * l, 0.0F, 0.0F);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                    }
                }

                if (item.getItem() instanceof BlockItem && !item.isOf(Items.STRING) && !item.isOf(Items.REDSTONE) && !item.isOf(Items.LEVER) && !item.isOf(Items.TRIPWIRE_HOOK) && !item.isIn(ItemTags.BANNERS) && !item.isIn(ItemTags.DOORS) && item.getUseAction() != UseAction.EAT) {
                    BlockItem blockItem = (BlockItem) item.getItem();
                    BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
                    blockRenderManager.getModel(blockItem.getBlock().getDefaultState());
                    matrices.push();
                    if (!bl2) matrices.translate(-0.4F, 0.0F, 0.0F);
                    matrices.scale(0.4F, 0.4F, 0.4F);
                    matrices.translate(-0.9 * l, -0.45, -0.5F);
                    if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.BUTTONS)) matrices.translate(0.2 * l, -0.15, -0.2);
                    if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.PRESSURE_PLATES)) matrices.translate(0.0F, 0.1, 0.0F);
                    if (item.isOf(Items.SLIME_BLOCK) || item.isOf(Items.HONEY_BLOCK) || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.FLOWERS) || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES) || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.SAPLINGS) || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.SWORD_EFFICIENT)) {
                        vertVelocityYSlime = (float) (vertVelocityYSlime + swingProgress * 0.03 * deltaTime * 30.0);
                        if ((player.getVelocity().length() > 0.09 && player.isOnGround() || player.isSwimming() || player.isCrawling() || player.isClimbing() && !player.isOnGround()) && client.options.getBobView().getValue()) {
                            Random random = new Random();
                            boolean randomBoolean = random.nextBoolean();
                            vertVelocityYSlime += (float) (-0.05 * player.getVelocity().length() * deltaTime * 30.0);
                        }
                        matrices.scale(1.0F, 1.0F + vertAngleYSlime * -2.0F, 1.0F);
                    }
                    BlockState blockState = blockItem.getBlock().getDefaultState();
                    if (player.age - prevAge >= 100.0F) {
                        repPower = !repPower;
                        prevAge = player.age;
                    }
                    if (blockItem.getBlock() == Blocks.REPEATER && repPower) blockState = blockState.with(RepeaterBlock.POWERED, true);
                    if (blockItem.getBlock() == Blocks.COMPARATOR && repPower) blockState = blockState.with(ComparatorBlock.POWERED, true);
                    if (blockItem.getBlock() == Blocks.REDSTONE_TORCH && player.isSubmergedInWater()) blockState = blockState.with(RedstoneTorchBlock.LIT, false);
                    if ((blockItem.getBlock() == Blocks.CAMPFIRE || blockItem.getBlock() == Blocks.SOUL_CAMPFIRE) && player.isSubmergedInWater()) blockState = blockState.with(CampfireBlock.LIT, false);
                    if (item.isIn(ItemTags.BEDS)) {
                        if (bl) matrices.translate(0.9, 0.0F, 0.8);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * l));
                    }
                    blockRenderManager.renderBlockAsEntity(blockState, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV);
                    matrices.pop();
                } else {
                    if (!item.isIn(ItemTags.SHOVELS) && item.getUseAction() != UseAction.EAT && (item.isEnchantable() || item.getUseAction() == UseAction.BOW || item.getUseAction() == UseAction.SPYGLASS || getAttackDamage(item) != 0.0F || item.getUseAction() == UseAction.BLOCK || item.isOf(Items.WARPED_FUNGUS_ON_A_STICK) || item.isOf(Items.CARROT_ON_A_STICK) || item.isOf(Items.FISHING_ROD) || item.isOf(Items.SHEARS) || item.isIn(ItemTags.HOES))) {
                        if (item.isIn(ItemTags.SWORDS) && !sharpSword) {
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F * swing));
                            matrices.translate(0.0F, 0.1 * swing, -0.1 * swing);
                        }
                        if (item.isIn(ItemTags.SHOVELS)) {
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
                        } else if (item.getUseAction() == UseAction.SPEAR) {
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40.0F * swing_rot));
                            matrices.translate(0.0F, 0.1 * swing_rot, -0.1 * swing_rot);
                        } else if (item.getUseAction() != UseAction.BLOCK) {
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0F * swing));
                            matrices.translate(0.0F, 0.05 * swing, -0.05 * swing);
                        }
                    }

                    if (item.isOf(Items.NETHER_STAR) || item.isOf(Items.END_CRYSTAL)) {
                        netherCounter = (float) (netherCounter + 0.9 * tt);
                        matrices.translate(0.0F, 0.25F + 0.02 * MathHelper.sin(netherCounter * 0.1F), 0.0F);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(3.0F * MathHelper.sin(netherCounter * 0.2F)));
                        matrices.scale(1.0F + 0.01F * MathHelper.sin(netherCounter), 1.0F + 0.01F * MathHelper.sin(netherCounter), 1.0F + 0.01F * MathHelper.sin(netherCounter));
                    } else {
                        netherCounter = 0.0F;
                    }

                    if (item.isIn(ItemTags.SHOVELS)) {
                        matrices.translate(0.07 * l, 0.0F, 0.05);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * l));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.0F));
                    }

                    if (item.isOf(Items.TORCH)) {
                        player.getWorld().addParticle(ParticleTypes.ITEM_SLIME, player.getPos().getX(), player.getPos().getY(), player.getPos().getZ(), 0.1, 0.1, 0.1);
                    }

                    ((HeldItemRendererInvoker) this).invokeRenderItem(player, item, bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl2, matrices, vertexConsumers, light);
                }
            }

            matrices.pop();
            matrices.pop();
            isAttacking = client.options.attackKey.isPressed();
        }
    }

    @Shadow
    private float equipProgressMainHand;
    @Shadow
    private float prevEquipProgressMainHand;
    @Shadow
    private float prevEquipProgressOffHand;
    @Shadow
    private float equipProgressOffHand;
    @Shadow
    private ItemStack offHand;
}
