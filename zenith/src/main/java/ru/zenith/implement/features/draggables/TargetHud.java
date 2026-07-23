package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.item.ItemUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.StopWatch;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.common.util.render.Render2DUtil;
import ru.zenith.common.util.render.ScissorManager;
import ru.zenith.core.Main;
import ru.zenith.implement.features.modules.combat.Aura;
import ru.zenith.implement.features.modules.combat.TriggerBot;

import java.util.List;
import java.util.stream.StreamSupport;

public class TargetHud extends AbstractDraggable {
    private final Animation animation = new DecelerateAnimation().setMs(200).setValue(1);
    private final StopWatch stopWatch = new StopWatch();
    private LivingEntity lastTarget;
    private Item lastItem = Items.AIR;
    private float health;

    public TargetHud() {
        super("Target Hud", 10, 40, 100, 36,true);
    }

    @Override
    public boolean visible() {
        return scaleAnimation.isDirection(Direction.FORWARDS);
    }

    @Override
    public void tick() {
        LivingEntity auraTarget = Aura.getInstance().getTarget();
        LivingEntity triggerBotTarget = TriggerBot.getInstance().getTarget();

        if (auraTarget != null) {
            lastTarget = auraTarget;
            startAnimation();
        } else if (triggerBotTarget != null) {
            lastTarget = triggerBotTarget;
            startAnimation();
            stopWatch.reset();
        } else if (PlayerIntersectionUtil.isChat(mc.currentScreen)) {
            lastTarget = mc.player;
            startAnimation();
        } else if (stopWatch.finished(500)) stopAnimation();
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (lastTarget != null) {
            MatrixStack matrix = context.getMatrices();
//            drawUsingItem(context, matrix);
//            drawArmor(context, matrix);
            drawMain(context, matrix);
            drawFace(context);
        }
    }

    private void drawMain(DrawContext context, MatrixStack matrix) {
        FontRenderer font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        float hp = PlayerIntersectionUtil.getHealth(lastTarget);
        float widthHp = 61;
        String stringHp = PlayerIntersectionUtil.getHealthString(hp);
        health = MathHelper.clamp(MathUtil.interpolateSmooth(1, health, Math.round(hp / lastTarget.getMaxHealth() * widthHp)), 2, widthHp);

        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight()).round(8).color(ColorUtil.getClientColor()).build());

        if (font.getStringWidth(lastTarget.getName().getString()) > 60) {
            ScissorManager scissorManager = Main.getInstance().getScissorManager();
            scissorManager.push(matrix.peek().getPositionMatrix(), getX(), getY(), getWidth() - 1, getHeight());
            font.drawGradientString(matrix, lastTarget.getName().getString(), getX() + 35, getY() + 6, ColorUtil.getText(), ColorUtil.getText(0.6F));
            scissorManager.pop();
        } else font.drawString(matrix, lastTarget.getName().getString(), getX() + 35, getY() + 6, ColorUtil.getText());

        rectangle.render(ShapeProperties.create(matrix, getX() + 34, getY() + 22F, widthHp, 8F)
                .round(4).color(ColorUtil.replAlpha(ColorUtil.getClientColor(), 100)).build());
        rectangle.render(ShapeProperties.create(matrix, getX() + 34, getY() + 22F, health, 8F)
                .round(4).color(ColorUtil.multDark(ColorUtil.getMainGuiColor(), 0.5F), ColorUtil.multDark(ColorUtil.getMainGuiColor(), 0.5F), ColorUtil.getMainGuiColor(), ColorUtil.getMainGuiColor()  ).build());

        float width = Fonts.getSize(14, Fonts.Type.DEFAULT).getStringWidth(stringHp);
        Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(matrix, "HP: " + stringHp, getX() + 35.5f, getY() + 15.5F, ColorUtil.getText());
    }

    private void drawArmor(DrawContext context, MatrixStack matrix) {
        List<ItemStack> items = StreamSupport.stream(lastTarget.getEquippedItems().spliterator(), false).filter(s -> !s.isEmpty()).toList();

        if (!items.isEmpty()) {
            float x = getX() + getWidth() / 2F - items.size() * 5.5F;
            float y = getY() - 13;
            float itemX = -10.5F;

            matrix.push();
            matrix.translate(x, y, -200);
            blur.render(ShapeProperties.create(matrix, 0, 0, items.size() * 11, 11).round(2.5F).softness(1)
                    .thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

            for (ItemStack stack : items) {
                Render2DUtil.defaultDrawStack(context, stack, itemX += 11, 0.5F, false, true, 0.5F);
            }
            matrix.pop();
        }
    }

    private void drawUsingItem(DrawContext context, MatrixStack matrix) {
        animation.setDirection(lastTarget.isUsingItem() ? Direction.FORWARDS : Direction.BACKWARDS);
        if (!lastTarget.getActiveItem().isEmpty() && lastTarget.getActiveItem().getCount() != 0) {
            lastItem = lastTarget.getActiveItem().getItem();
        }

        if (!animation.isFinished(Direction.BACKWARDS) && !lastItem.equals(Items.AIR)) {
            int size = 24;
            float anim = animation.getOutput().floatValue();
            float progress = (lastTarget.getItemUseTime() + tickCounter.getTickDelta(false)) / ItemUtil.maxUseTick(lastItem) * 360;

            float x = getX() - (size + 5) * anim;
            float y = getY() + 6;

            ScissorManager scissorManager = Main.getInstance().getScissorManager();
            scissorManager.push(matrix.peek().getPositionMatrix(), getX() - 50, getY(), 50, getHeight());
            MathUtil.setAlpha(anim, () -> {
                blur.render(ShapeProperties.create(matrix, x, y, size, size).round(12).softness(1)
                        .thickness(2).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.7F)).build());

                arc.render(ShapeProperties.create(matrix, x, y, size, size).round(0.4F).thickness(0.2f).end(progress)
                        .color(ColorUtil.fade(0), ColorUtil.fade(200), ColorUtil.fade(0), ColorUtil.fade(200)).build());

                Render2DUtil.defaultDrawStack(context, lastItem.getDefaultStack(), x + 3, y + 3, false, false, 1);
            });
            scissorManager.pop();
        }
    }

    private void drawFace(DrawContext context) {
        EntityRenderer<? super LivingEntity, ?> baseRenderer = mc.getEntityRenderDispatcher().getRenderer(lastTarget);
        if (!(baseRenderer instanceof LivingEntityRenderer<?, ?, ?>)) {
            return;
        }

        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?> renderer = (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) baseRenderer;
        LivingEntityRenderState state = renderer.getAndUpdateRenderState(lastTarget, tickCounter.getTickDelta(false));
        Identifier textureLocation = renderer.getTexture(state);
        AbstractTexture abstractTexture = mc.getTextureManager().getTexture(textureLocation);

       /* MinecraftTextureHelper minecraftTextureHelper = MinecraftTextureHelper.INSTANCE;
        RenderPhase.Texture texture = minecraftTextureHelper.getOrCreateTexture(abstractTexture);
        MinecraftTextureHelper.UV uv = minecraftTextureHelper.getFaceUV(currentTarget.getClass()); */

        Render2DUtil.drawTexture(context, textureLocation, getX() + 3, getY() + 4F, 28, 6, 8, 8, 64, ColorUtil.getRect(1), ColorUtil.multRed(-1, 1 + lastTarget.hurtTime / 4F));
    }
}