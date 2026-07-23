package dev.ethereal.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.client.features.modules.combat.Aura;

@ModuleRegister(name = "Swing Animation", category = Category.RENDER)
public class SwingAnimationModule extends Module {
    @Getter private static final SwingAnimationModule instance = new SwingAnimationModule();

    public final ModeSetting mode = new ModeSetting("Mode").value("Smooth").values(
            "Smooth", "Static", "Down", "DropDown", "Poke", "SelfBack",
            "Feast", "ToBack", "Block", "Akrien", "Break", "Pander", "Slant"
    );
    private final BooleanSetting auraOnly = new BooleanSetting("Only with aura").value(false);
    public final SliderSetting strength = new SliderSetting("Strength").value(1.0f).range(0.1f, 3.0f).step(0.01f);

    public final BooleanSetting slow = new BooleanSetting("Slow").value(false);
    public final SliderSetting speed = new SliderSetting("Speed").value(12f).range(1f,50f).step(1f).setVisible(slow::getValue);

    public SwingAnimationModule() {
        addSettings(mode, auraOnly, strength, slow, speed);
    }

    private void handleSwordAnim(MatrixStack matrices, float swingProgress, Arm arm) {
        float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        int i = arm == Arm.RIGHT ? 1 : -1;
        float str = strength.getValue();

        switch (mode.getValue()) {
            case "Smooth" -> {
                matrices.translate(i * 0.56f, -0.52f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45f + sin1 * -20f * str)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * sin2 * -20f * str));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80f * str));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45f));
            }
            case "Down" -> {
                matrices.translate(i * 0.56f, -0.32f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5 * str));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100 * str));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155 * str));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100));
            }
            case "Poke" -> {
                float anim = (float) Math.sin(swingProgress * (float) (Math.PI / 2) * 2);
                float tilt = str / 3f;
                matrices.translate(i * 0.56f, -0.52f, -0.72f);
                matrices.translate(0.0f, 0.0f, tilt * -anim);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(75f * i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-75f * (str / 4f) * anim - 60f) * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-75f));
            }
            case "Static" -> {
                matrices.translate(i * 0.56f, -0.42f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -60f * str));
                matrices.translate(0, -0.1, 0);
            }
            case "Feast" -> {
                matrices.translate(i * 0.56f, -0.32f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75 * i * str));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -65 * str));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30 * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35 * i));
            }
            case "Akrien" -> {
                matrices.translate(i * 0.65f, -0.32f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76 * i));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5 * str));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100 * str));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155 * str));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 25 * str));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -25 * str));
                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin1 * 15 * str));
                matrices.translate(sin2 * 0.18f * str, sin2 * 0.59f * str, 0);
            }
            case "Block" -> {
                if (swingProgress > 0) {
                    float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    matrices.translate(0.56f * i, -0.5f, -0.7f);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * i));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -85f * str));
                    matrices.translate(-0.1f * i, 0.28f, 0.2f);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-85f));
                } else {
                    applyEquipOffset(matrices, i, 0);
                    applySwingOffset(matrices, i, swingProgress, str);
                }
            }
            case "ToBack" -> {
                float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                matrices.translate(0.65f * i, -0.45f, -0.9f);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(50f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((-30f * (1f - g * str) - 30f) * i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(110f * i));
            }
            case "SelfBack" -> {
                float anim = (float) Math.sin(swingProgress * (float) (Math.PI / 2) * 2);
                matrices.translate(0.65f * i, -0.3f, -0.8f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70 * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100 - (60 * str) * anim));
            }
            case "Break" -> {
                matrices.translate(0.66f * i, -0.3f, -0.38f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(270 * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * 10f * str));
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
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-12f * anim * str));
            }
            case "Pander" -> {
                float panderAnim = MathHelper.sin(swingProgress * (float) Math.PI);
                matrices.translate(i * 0.56f, -0.52f, -0.72f);
                matrices.translate((0.3f - panderAnim * 0.15f) * i, 0.2f, -0.15f - panderAnim * 0.13f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((76f - 10f * panderAnim) * i));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((-16f - 8f * panderAnim) * i));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-83f - 26f * panderAnim));
            }
            case "Slant" -> {
                float anim = (float) Math.sin(swingProgress * (float) (Math.PI / 2.0) * 2.0);
                float rotate = 35.0f * str;
                matrices.translate(i * 0.56f, -0.52f, -0.72f);
                matrices.translate(0.0f, 0.0f, -0.3f * anim * str);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(anim * -rotate));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(anim * rotate));
            }
            default -> {
                // Fallback to default smooth animation
                matrices.translate(i * 0.56f, -0.52f, -0.72f);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45f + sin1 * -20f * str)));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * sin2 * -20f * str));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80f * str));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45f));
            }
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

    public void handleRenderItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!player.isUsingSpyglass()) {
            boolean bl = hand == Hand.MAIN_HAND;
            Arm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
            matrices.push();
            if (item.isOf(Items.CROSSBOW)) {
                boolean bl2 = CrossbowItem.isCharged(item);
                boolean bl3 = arm == Arm.RIGHT;
                int i = bl3 ? 1 : -1;
                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    matrices.translate((float) i * -0.4785682F, -0.094387F, 0.05731531F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * 65.3F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * -9.785F));
                    float f = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                    float g = f / (float) CrossbowItem.getPullTime(item, mc.player);
                    if (g > 1.0F) {
                        g = 1.0F;
                    }

                    if (g > 0.1F) {
                        float h = MathHelper.sin((f - 0.1F) * 1.3F);
                        float j = g - 0.1F;
                        float k = h * j;
                        matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                    }

                    matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                    matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) i * 45.0F));
                } else {
                    float fx = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    float gx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
                    float h = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                    matrices.translate((float) i * fx, gx, h);
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    this.applySwingOffset(matrices, arm, swingProgress);
                    if (bl2 && swingProgress < 0.001F && bl) {
                        matrices.translate((float) i * -0.641864F, 0.0F, 0.0F);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * 10.0F));
                    }
                }
                this.renderItem(player, item, bl3 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl3, matrices, vertexConsumers, light);
            } else {
                boolean bl2 = arm == Arm.RIGHT;

                ViewModelModule viewModel = ViewModelModule.getInstance();
                if (viewModel.isEnabled()) {
                    if (bl2) {
                        matrices.translate(viewModel.rightX.getValue().doubleValue(), viewModel.rightY.getValue().doubleValue(), viewModel.rightZ.getValue().doubleValue());
                    } else {
                        matrices.translate(-viewModel.leftX.getValue().doubleValue(), viewModel.leftY.getValue().doubleValue(), viewModel.leftZ.getValue().doubleValue());
                    }
                }

                if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                    int l = bl2 ? 1 : -1;
                    switch (item.getUseAction()) {
                        case NONE, BLOCK:
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            break;
                        case EAT:
                        case DRINK:
                            this.applyEatOrDrinkTransformation(matrices, tickDelta, arm, item);
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            break;
                        case BOW:
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            matrices.translate((float) l * -0.2785682F, 0.18344387F, 0.15731531F);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-13.935F));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 35.3F));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -9.785F));
                            float mx = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                            float fxx = mx / 20.0F;
                            fxx = (fxx * fxx + fxx * 2.0F) / 3.0F;
                            if (fxx > 1.0F) {
                                fxx = 1.0F;
                            }

                            if (fxx > 0.1F) {
                                float gx = MathHelper.sin((mx - 0.1F) * 1.3F);
                                float h = fxx - 0.1F;
                                float j = gx * h;
                                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                            }

                            matrices.translate(fxx * 0.0F, fxx * 0.0F, fxx * 0.04F);
                            matrices.scale(1.0F, 1.0F, 1.0F + fxx * 0.2F);
                            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) l * 45.0F));
                            break;
                        case SPEAR:
                            this.applyEquipOffset(matrices, arm, equipProgress);
                            matrices.translate((float) l * -0.5F, 0.7F, 0.1F);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-55.0F));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 35.3F));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -9.785F));
                            float m = (float) item.getMaxUseTime(mc.player) - ((float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F);
                            float fx = m / 10.0F;
                            if (fx > 1.0F) {
                                fx = 1.0F;
                            }

                            if (fx > 0.1F) {
                                float gx = MathHelper.sin((m - 0.1F) * 1.3F);
                                float h = fx - 0.1F;
                                float j = gx * h;
                                matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                            }

                            matrices.translate(0.0F, 0.0F, fx * 0.2F);
                            matrices.scale(1.0F, 1.0F, 1.0F + fx * 0.2F);
                            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float) l * 45.0F));
                            break;
                        case BRUSH:
                            this.applyBrushTransformation(matrices, tickDelta, arm, item, equipProgress);
                    }
                } else if (player.isUsingRiptide()) {
                    this.applyEquipOffset(matrices, arm, equipProgress);
                    int l = bl2 ? 1 : -1;
                    matrices.translate((float) l * -0.4F, 0.8F, 0.3F);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) l * 65.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) l * -85.0F));
                } else {
                    if (arm == mc.options.getMainArm().getValue() && isEnabled() && auraCheck()) {
                        handleSwordAnim(matrices, swingProgress, arm);
                    } else {
                        float n = -0.4F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                        float mxx = 0.2F * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) (Math.PI * 2));
                        float fxxx = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
                        int o = bl2 ? 1 : -1;
                        matrices.translate((float) o * n, mxx, fxxx);
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        this.applySwingOffset(matrices, arm, swingProgress);
                    }
                }
                this.renderItem(player, item, bl2 ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND, !bl2, matrices, vertexConsumers, light);
            }

            matrices.pop();
        }
    }

    private void applyBrushTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack, float equipProgress) {
        this.applyEquipOffset(matrices, arm, equipProgress);
        float f = (float) (mc.player.getItemUseTimeLeft() % 10);
        float g = f - tickDelta + 1.0F;
        float h = 1.0F - g / 10.0F;
        float n = -15.0F + 75.0F * MathHelper.cos(h * 2.0F * (float) Math.PI);
        if (arm != Arm.RIGHT) {
            matrices.translate(0.1, 0.83, 0.35);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n));
            matrices.translate(-0.3, 0.22, 0.35);
        } else {
            matrices.translate(-0.25, 0.22, 0.35);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(0.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(n));
        }
    }

    private void applyEatOrDrinkTransformation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack) {
        float f = (float) mc.player.getItemUseTimeLeft() - tickDelta + 1.0F;
        float g = f / (float) stack.getMaxUseTime(mc.player);
        if (g < 0.8F) {
            float h = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);
            matrices.translate(0.0F, h, 0.0F);
        }

        float h = 1.0F - (float) Math.pow(g, 27.0);
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(h * 0.6F * (float) i, h * -0.5F, h * 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * h * 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(h * 10.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * h * 30.0F));
    }

    private void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate((float) i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float g = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * (45.0F + f * -20.0F)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) i * g * -20.0F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(g * -80.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) i * -45.0F));
    }

    public void renderItem(LivingEntity entity, ItemStack stack, ModelTransformationMode renderMode, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!stack.isEmpty()) {
            HandOverlayModule handOverlay = HandOverlayModule.getInstance();
            if (handOverlay.isEnabled() && handOverlay.isShaderPackCompatMode()) {
                mc.getItemRenderer().renderItem(entity, stack, renderMode, leftHanded, matrices, vertexConsumers, entity.getWorld(), light, OverlayTexture.DEFAULT_UV, entity.getId() + renderMode.ordinal());
                handOverlay.beginCompatOverlay();
                mc.getItemRenderer().renderItem(entity, stack, renderMode, leftHanded, matrices, vertexConsumers, entity.getWorld(), light, OverlayTexture.DEFAULT_UV, entity.getId() + renderMode.ordinal());
                handOverlay.endCompatOverlay();
                return;
            }

            VertexConsumerProvider provider = handOverlay.isEnabled() ? handOverlay.wrap(vertexConsumers) : vertexConsumers;
            mc.getItemRenderer().renderItem(entity, stack, renderMode, leftHanded, matrices, provider, entity.getWorld(), light, OverlayTexture.DEFAULT_UV, entity.getId() + renderMode.ordinal());
        }
    }

    public boolean auraCheck() {
        return !auraOnly.getValue() || Aura.getInstance().getTarget() != null;
    }
}