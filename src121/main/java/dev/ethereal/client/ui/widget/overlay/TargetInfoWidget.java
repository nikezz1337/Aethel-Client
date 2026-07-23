package dev.ethereal.client.ui.widget.overlay;

import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.client.features.modules.combat.Aura;
import dev.ethereal.client.features.modules.other.HealthResolverModule;
import dev.ethereal.client.ui.widget.Widget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class TargetInfoWidget extends Widget {
    @Override
    public String getName() {
        return "Target info";
    }

    public TargetInfoWidget() {
        super(30f, 30f);
    }

    private final AnimationUtil showAnimation = new AnimationUtil();
    private float healthAnimation = 0f;
    private float animatedHealth = 0f;
    private float animatedAbsorption = 0f;
    private LivingEntity target;

    @Override
    public void render(Render2DEvent event) {
        DrawContext context = event.context();
        MatrixStack matrixStack = event.matrixStack();
        update();

        LivingEntity pretendTarget = getTarget();
        if (pretendTarget != null && pretendTarget != target) {
            animatedHealth = 0f;
            animatedAbsorption = 0f;
        }
        if (pretendTarget != null) target = pretendTarget;
        if (showAnimation.getValue() <= 0.0 || target == null) return;

        healthAnimation = MathHelper.clamp(MathUtil.interpolate(healthAnimation, target.getHealth() / target.getMaxHealth(), 0.3f), 0f, 1f);

        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float anim = (float) showAnimation.getValue();

        float padding      = scaled(3.5f);
        float innerPad     = scaled(3.5f);
        float avatarSize   = scaled(16f);
        float nameSize     = scaled(6.5f);
        float hpSize       = scaled(5.5f);
        float spacing      = scaled(2f);
        float hpBarHeight  = scaled(3f);
        float hpBarPad     = scaled(2f);
        float itemIconSize = scaled(7f);
        float itemGap      = scaled(1.5f);

        String nameText  = target.getName().getString();
        float nameWidth  = getMediumFont().getWidth(nameText, nameSize);
        float nameHeight = getMediumFont().getHeight(nameSize);

        float currentHealth    = target.getHealth();
        float maxHealth        = Math.max(target.getMaxHealth(), 1.0f);
        float absorptionAmount = PlayerUtil.isFT() ? 0 : target.getAbsorptionAmount();
        float[] resolvedHealth = HealthResolverModule.getInstance().getHealthFromScoreboard(target);
        currentHealth = resolvedHealth[0];
        maxHealth = Math.max(resolvedHealth[1], 1.0f);

        // Парс HP через скорборд на FT/RW серверах
        // Плавная анимация HP как FPS в WatermarkWidget
        if (animatedHealth == 0f) animatedHealth = currentHealth;
        animatedHealth = MathHelper.lerp(0.1f, animatedHealth, currentHealth);

        // Плавная анимация абсорбции
        if (animatedAbsorption == 0f) animatedAbsorption = absorptionAmount;
        animatedAbsorption = MathHelper.lerp(0.1f, animatedAbsorption, absorptionAmount);

        String hpText  = String.format("HP: %.1f", animatedHealth);
        float hpWidth  = getMediumFont().getWidth(hpText, hpSize);
        float hpHeight = getMediumFont().getHeight(hpSize);

        boolean hasAbsorption = animatedAbsorption > 0.01f;
        String absorptionText = hasAbsorption ? String.format("%.1f", animatedAbsorption) : "";
        float absorptionTextWidth = hasAbsorption ? getMediumFont().getWidth(absorptionText, hpSize) : 0f;
        float combinedHpWidth = hasAbsorption ? hpWidth + absorptionTextWidth + scaled(12f) : hpWidth;

        float contentWidth   = Math.max(nameWidth, combinedHpWidth);
        float healthBarWidth = Math.max(contentWidth, scaled(55f));

        float totalWidth  = padding + avatarSize + innerPad + healthBarWidth + padding;
        float totalHeight = padding + nameHeight + spacing + hpHeight + spacing + hpBarHeight + padding-6;

        // --- собираем предметы таргета (броня + оффхенд + мейнхенд) ---
        ItemStack mainHand  = target.getEquippedStack(EquipmentSlot.MAINHAND);
        ItemStack offHand   = target.getEquippedStack(EquipmentSlot.OFFHAND);
        ItemStack helmet    = target.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chestplate= target.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack leggings  = target.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack boots     = target.getEquippedStack(EquipmentSlot.FEET);

        // Иконки в правом верхнем углу: mainhand, offhand, helmet, chest, legs, boots
        ItemStack[] slots = { mainHand, offHand, helmet, chestplate, leggings, boots };
        int nonEmptyCount = 0;
        for (ItemStack s : slots) if (!s.isEmpty()) nonEmptyCount++;

        float iconsRowWidth = nonEmptyCount > 0 ? nonEmptyCount * (itemIconSize + itemGap) - itemGap : 0f;
        // расширяем виджет если иконки шире
        if (iconsRowWidth > totalWidth) totalWidth = iconsRowWidth + padding * 2f;

        float iconsY = y - itemIconSize - itemGap;

        int bgAlpha = (int)(200 * anim);
        Color bgColor = new Color(18, 18, 18, bgAlpha);
        float round = scaled(3f);

        // --- фон основного блока ---
        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, totalWidth, totalHeight, round + 1, bgColor);

        // --- аватар ---
        float avatarX = x + padding;
        float avatarY = y + (totalHeight - avatarSize) / 2f;

        if (target instanceof PlayerEntity player) {
            float headRadius = avatarSize * 0.15f - 2;
            Color headColor = ColorUtil.setAlpha(Color.WHITE, (int)(255 * anim));
            RenderUtil.TEXTURE_RECT.drawRoundedHead(matrixStack, player, avatarX, avatarY, avatarSize, avatarSize, headRadius, headColor);
        } else {
            float headRadius = avatarSize * 0.15f - 2;
            Color bgc = new Color(60, 60, 60, (int)(255 * anim));
            RenderUtil.BLUR_RECT.draw(matrixStack, avatarX, avatarY, avatarSize, avatarSize, headRadius, bgc);
            getSemiBoldFont().drawCenteredText(matrixStack, "?", avatarX + avatarSize / 2f, avatarY + avatarSize / 2f - avatarSize * 0.4f, avatarSize * 0.8f, UIColors.textColor((int)(255 * anim)));
        }

        float textX = avatarX + avatarSize + innerPad;

        // --- ник белым ---
        float nameY = y + padding;
        getMediumFont().drawText(matrixStack, nameText, textX, nameY, nameSize, new Color(255, 255, 255, (int)(255 * anim)));

        // --- HP текст ---
        float hpY = nameY + nameHeight + spacing;
        Color subColor = new Color(185, 185, 185, (int)(255 * anim));
        getMediumFont().drawText(matrixStack, hpText, textX, hpY, hpSize, subColor);

        if (hasAbsorption && anim > 0.01f) {
            float absorptionDrawX = x + totalWidth - padding - absorptionTextWidth;
            getMediumFont().drawText(matrixStack, absorptionText, absorptionDrawX, hpY, hpSize, new Color(255, 217, 13, (int)(255 * anim)));
        }

        // --- рект для полоски HP под основным блоком ---
        if (anim > 0.01f) {
            float blockGap    = scaled(1.5f);
            float hpBlockH    = hpBarPad * 2f + hpBarHeight;
            float hpBlockY    = y + totalHeight + blockGap;
            float hpBarRound  = scaled(0.5f);
            float blockRound  = scaled(2f);

            // фон блока полоски
            RenderUtil.BLUR_RECT.draw(matrixStack, x, hpBlockY, totalWidth, hpBlockH, blockRound, new Color(18, 18, 18, (int)(200 * anim)));

            // сама полоска внутри блока
            float barX = x + hpBarPad;
            float barY = hpBlockY + hpBarPad;
            float barW = totalWidth - hpBarPad * 2f;

            RenderUtil.BLUR_RECT.draw(matrixStack, barX, barY, barW, hpBarHeight, hpBarRound, new Color(0, 0, 0, (int)(150 * anim)));

            float healthRatio = MathHelper.clamp(animatedHealth / maxHealth, 0f, 1f);
            float healthFill  = barW * healthRatio;
            if (healthFill > 0) {
                Color prim = ColorUtil.setAlpha(UIColors.primary(), (int)(255 * anim));
                Color sec  = ColorUtil.setAlpha(UIColors.secondary(), (int)(255 * anim));
                RenderUtil.GRADIENT_RECT.draw(matrixStack, barX, barY, healthFill, hpBarHeight, hpBarRound, prim, sec, prim, sec);
            }

            if (hasAbsorption) {
                float absorptionOverlay = barW * MathHelper.clamp(animatedAbsorption / maxHealth, 0f, 1f);
                if (absorptionOverlay > 0) {
                    Color goldStart = new Color(57, 34, 4, (int)(200 * anim));
                    Color goldEnd   = new Color(255, 217, 13, (int)(255 * anim));
                    RenderUtil.GRADIENT_RECT.draw(matrixStack, barX, barY, Math.min(absorptionOverlay, barW), hpBarHeight, hpBarRound, goldStart, goldEnd, goldStart, goldEnd);
                }
            }

            getDraggable().setHeight(totalHeight + blockGap + hpBlockH);
        }

        // --- иконки предметов в правом верхнем углу над ректом ---
        if (nonEmptyCount > 0 && pretendTarget != null) {
            float scaleFactor = itemIconSize / 16f;
            float iconX = x + totalWidth - iconsRowWidth - padding;

            for (ItemStack stack : slots) {
                if (stack.isEmpty()) continue;

                matrixStack.push();
                matrixStack.translate(iconX, iconsY, 0f);
                matrixStack.scale(scaleFactor, scaleFactor, 1f);
                context.drawItem(stack, 0, 0);
                matrixStack.pop();

                iconX += itemIconSize + itemGap;
            }
        }

        getDraggable().setWidth(totalWidth);
        if (anim <= 0.01f) getDraggable().setHeight(totalHeight);
    }

    @Override
    public void render(MatrixStack matrixStack) {}

    private void update() {
        showAnimation.update();
        showAnimation.run(getTarget() != null ? 1.0 : 0.0, getDuration(), getEasing());
    }

    private LivingEntity getTarget() {
        Aura aura = Aura.getInstance();
        if (aura.isEnabled() && aura.getTarget() != null) return aura.getTarget();
        if (mc.currentScreen instanceof ChatScreen) return mc.player;
        return null;
    }
}
