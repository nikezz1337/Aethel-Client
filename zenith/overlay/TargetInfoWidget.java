package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import sweetie.evaware.api.utils.animation.AnimationUtil;
import sweetie.evaware.api.utils.animation.Easing;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.client.features.modules.combat.AuraModule;
import sweetie.evaware.client.features.modules.render.InterfaceModule;
import sweetie.evaware.client.ui.widget.Widget;

import java.awt.*;
import java.time.Duration;

public class TargetInfoWidget extends Widget {
    @Override
    public String getName() {
        return "Target info";
    }

    public TargetInfoWidget() {
        super(30f, 30f);
    }

    private final AnimationUtil showAnimation = new AnimationUtil();
    private final AnimationUtil healthBackAnim = new AnimationUtil();
    private final AnimationUtil healthAnim = new AnimationUtil();
    private final AnimationUtil absorptionAnim = new AnimationUtil();
    private final AnimationUtil absorptionXAnim = new AnimationUtil();

    private float healthAnimation = 0f;
    private LivingEntity target;
    private LivingEntity lastTarget;

    // Режим можно переключать через настройку в InterfaceModule
    // Для простоты сделаем оба режима и будем переключать по условию
    private boolean useNewMode = true; // true = New, false = Old

    @Override
    public void render(MatrixStack matrixStack) {
        update();
        LivingEntity pretendTarget = getTarget();

        if (pretendTarget != null) {
            target = pretendTarget;
        }

        if (showAnimation.getValue() <= 0.0 || target == null) return;

        if (useNewMode) {
            renderNewMode(matrixStack);
        } else {
            renderOldMode(matrixStack);
        }
    }

    private void renderNewMode(MatrixStack ms) {
        if (lastTarget != null || showAnimation.getValue() > 0) {
            LivingEntity renderTarget = lastTarget != null ? lastTarget : mc.player;
            if (renderTarget == null) return;

            float x = getDraggable().getX();
            float y = getDraggable().getY();
            float anim = (float) showAnimation.getValue();

            float padding = scaled(3.5f);
            float innerPadding = scaled(3.0f);
            float avatarSize = scaled(22.0f);
            float nameSize = scaled(6.25f);
            float hpSize = scaled(5.0f);
            float spacing = scaled(2.0f);
            float healthBarHeight = scaled(3.5f);

            String nameText = renderTarget.getName().getString();
            float nameWidth = getMediumFont().getWidth(nameText, nameSize);
            float nameHeight = getMediumFont().getHeight(nameSize);

            float multiplier = (float) Math.pow(10, 1);
            float currentHealth = renderTarget.getHealth();
            float maxHealth = Math.max(renderTarget.getMaxHealth(), 1.0f);
            float absorptionAmount = renderTarget.getAbsorptionAmount();
            float finalHealth = Math.round(currentHealth * multiplier) / multiplier;
            float finalAbsorption = Math.round(absorptionAmount * multiplier) / multiplier;

            String hpText = "HP: " + finalHealth;
            float hpWidth = getMediumFont().getWidth(hpText, hpSize);
            float hpHeight = getMediumFont().getHeight(hpSize);

            boolean hasAbsorption = absorptionAmount > 0;
            String absorptionText = hasAbsorption ? "" + finalAbsorption : "";
            float absorptionTextWidth = hasAbsorption ? getMediumFont().getWidth(absorptionText, hpSize) : 0.0f;

            float combinedHpWidth = hasAbsorption ? hpWidth + absorptionTextWidth + scaled(12.0f) : hpWidth;
            float contentWidth = Math.max(nameWidth, combinedHpWidth);
            float healthBarWidth = Math.max(contentWidth, scaled(55.0f));

            float totalWidth = padding + avatarSize + innerPadding + healthBarWidth + padding;
            float totalHeight = padding + nameHeight + spacing + hpHeight + spacing + healthBarHeight + padding;

            getDraggable().setWidth(totalWidth);
            getDraggable().setHeight(totalHeight);

            ms.push();
            float centX = x + totalWidth / 2f;
            float centY = y + totalHeight / 2f;
            ms.translate(centX, centY, 0);
            ms.scale(anim, anim, 1);
            ms.translate(-centX, -centY, 0);

            float healthRatio = MathHelper.clamp(currentHealth / maxHealth, 0.0f, 1.0f);
            float absorptionRatio = MathHelper.clamp(absorptionAmount / maxHealth, 0.0f, 1.0f);

            healthBackAnim.update();
            healthAnim.update();
            absorptionAnim.update();
            absorptionXAnim.update();

            healthBackAnim.run(healthBarWidth * healthRatio, 1750, Easing.CUBIC_OUT);
            healthAnim.run(healthBarWidth * healthRatio, 450, Easing.CUBIC_OUT);
            absorptionAnim.run(healthBarWidth * MathHelper.clamp(healthRatio + absorptionRatio, 0.0f, 1.0f), 450, Easing.CUBIC_OUT);
            absorptionXAnim.run(hasAbsorption ? 1 : 0, 450, Easing.CUBIC_OUT);

            int bgAlpha = (int) (220 * MathHelper.clamp(anim, 0.0f, 1.0f));
            RenderUtil.BLUR_RECT.draw(ms, x, y, totalWidth, totalHeight, scaled(3.0f), new Color(0, 0, 0, bgAlpha));

            float avatarX = x + padding;
            float avatarY = y + (totalHeight - avatarSize) / 2.0f;
            drawRoundFace(ms, avatarX, avatarY, (int) avatarSize, 1.0f, scaled(2.0f), renderTarget);

            float textX = avatarX + avatarSize + innerPadding;
            float nameY = y + padding;
            getMediumFont().drawText(ms, nameText, textX, nameY, nameSize, Color.WHITE);

            float hpY = nameY + nameHeight + spacing;
            getMediumFont().drawText(ms, hpText, textX, hpY, hpSize, Color.WHITE);

            if (hasAbsorption && absorptionXAnim.getValue() > 0) {
                float absorptionDrawX = x + totalWidth - padding - absorptionTextWidth;
                int alpha = (int) (absorptionXAnim.getValue() * 255);
                getMediumFont().drawText(ms, absorptionText, absorptionDrawX, hpY, hpSize, new Color(255, 217, 13, alpha));
            }

            float healthBarX = textX;
            float healthBarY = hpY + hpHeight + spacing;
            RenderUtil.RECT.draw(ms, healthBarX, healthBarY, healthBarWidth, healthBarHeight, scaled(1.0f), new Color(0, 0, 0, 150));

            float healthFill = MathHelper.clamp((float) healthAnim.getValue(), 0.0f, healthBarWidth);
            if (healthFill > 0) {
                Color themeColor = UIColors.primary();
                Color colorStart = ColorUtil.setAlpha(themeColor, 210);
                Color colorEnd = ColorUtil.setAlpha(Color.WHITE, 255);
                RenderUtil.GRADIENT_RECT.draw(ms, healthBarX, healthBarY, healthFill, healthBarHeight, scaled(1.0f), 
                    colorStart, colorStart, colorEnd, colorEnd);
            }

            if (hasAbsorption && absorptionXAnim.getValue() > 0) {
                float absorptionOverlay = healthBarWidth * MathHelper.clamp(absorptionAmount / maxHealth, 0.0f, 1.0f);
                if (absorptionOverlay > 0) {
                    int alphaStart = (int) (absorptionXAnim.getValue() * 200);
                    int alphaEnd = (int) (absorptionXAnim.getValue() * 255);
                    Color goldStart = new Color(57, 34, 4, alphaStart);
                    Color goldEnd = new Color(255, 217, 13, alphaEnd);
                    RenderUtil.GRADIENT_RECT.draw(ms, healthBarX, healthBarY, Math.min(absorptionOverlay, healthBarWidth), 
                        healthBarHeight, scaled(1.0f), goldStart, goldStart, goldEnd, goldEnd);
                }
            }

            ms.pop();
        }

        if (target != null) {
            lastTarget = target;
        }
    }

    private void drawRoundFace(MatrixStack ms, float x, float y, int size, float alpha, float radius, LivingEntity entity) {
        if (entity instanceof PlayerEntity player) {
            Color headColor = ColorUtil.setAlpha(Color.WHITE, (int)(alpha * 255));
            RenderUtil.TEXTURE_RECT.drawHead(ms, player, x, y, size, size, radius, 0f, headColor);
        } else {
            float fontSize = size * 0.8f;
            getSemiBoldFont().drawCenteredText(ms, "?", x + size / 2f, y + size / 2f - fontSize / 2f, fontSize, Color.WHITE);
        }
    }

    private void renderOldMode(MatrixStack matrixStack) {
        healthAnimation = MathHelper.clamp(MathUtil.interpolate(healthAnimation, target.getHealth() / target.getMaxHealth(), 0.3f), 0f, 1f);
        float x = getDraggable().getX();
        float y = getDraggable().getY();

        float anim = (float) showAnimation.getValue();

        float[] headProperties = headProperties(x, y);
        float headX = headProperties[0];
        float headY = headProperties[1];
        float headSize = headProperties[2];

        float bigFontSize = headSize * 0.35f;
        float smallFontSize = (headSize * 0.4f) * 0.7f;

        String targetName = target.getName().getString();
        String healthText = String.format("%.1f", target.getHealth() + target.getAbsorptionAmount()) + "HP";
        float healthTextWidth = getMediumFont().getWidth(healthText, smallFontSize);

        float offset = getGap() * 3f;
        float margin = getGap() * 2f;
        float width = headSize * 3.7f + margin * 2f;
        float height = headSize + getGap() * 2f;
        float backgroundRound = offset * 0.7f;

        int fullAlpha = (int) (anim * 255f);

        float[] healthBarProperties = healthBarProperties();
        float healthBarHeight = healthBarProperties[0];
        float healthBarRound = healthBarProperties[1];
        float healthBarY = y + height - healthBarHeight - margin;
        float healthBarX = x + headSize + margin;
        float healthBarWidth = width - margin * 2.5f - headSize - healthTextWidth;

        float diffHealth = Math.abs(smallFontSize - healthBarHeight) / 2f;

        float nameDiffToHealthBar = Math.abs((y + margin) - (healthBarY - margin / 2f));
        float nameY = y + margin + nameDiffToHealthBar / 2f - bigFontSize / 2f;

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, width, height, backgroundRound, UIColors.widgetBlur(fullAlpha));

        Color textColor = UIColors.textColor(fullAlpha);
        getMediumFont().drawWrap(matrixStack, targetName, healthBarX, nameY, width - headSize - margin, bigFontSize, textColor, scaled(9f), Duration.ofMillis(2500), Duration.ofMillis(1700));
        getMediumFont().drawText(matrixStack, healthText, x + width - healthTextWidth - margin, healthBarY - diffHealth, smallFontSize, textColor);

        RenderUtil.RECT.draw(matrixStack, healthBarX, healthBarY, healthBarWidth, healthBarHeight, healthBarRound, UIColors.backgroundBlur(fullAlpha));

        Color color1 = UIColors.gradient(0, fullAlpha);
        Color color2 = UIColors.gradient(90, fullAlpha);
        RenderUtil.GRADIENT_RECT.draw(matrixStack, healthBarX, healthBarY, healthBarWidth * healthAnimation, healthBarHeight, healthBarRound, color1, color2, color1, color2);

        if (target instanceof PlayerEntity player) {
            Color headColor = ColorUtil.setAlpha(Color.WHITE, fullAlpha);
            RenderUtil.TEXTURE_RECT.drawHead(matrixStack, player, headX, headY, headSize, headSize, getGap() / 2f, 0f, headColor);
        } else {
            float headFontSize = headSize * 0.8f;
            getSemiBoldFont().drawCenteredText(matrixStack, "?", headX + headSize / 2f, headY + headSize / 2f - headFontSize / 2f, headFontSize, UIColors.textColor(fullAlpha));
        }

        getDraggable().setWidth(width);
        getDraggable().setHeight(height);
    }

    private float[] healthBarProperties() {
        float height = scaled(5f);
        float round = height * 0.3f;

        return new float[]{height, round};
    }

    private float[] headProperties(float xPos, float yPos) {
        float x = xPos + getGap();
        float y = yPos + getGap();
        float size = scaled(25f);
        return new float[]{x, y, size};
    }

    private void update() {
        showAnimation.update();
        showAnimation.run(getTarget() != null ? 1.0 : 0.0, getDuration(), getEasing());
    }

    private LivingEntity getTarget() {
        AuraModule aura = AuraModule.getInstance();

        if (aura.isEnabled() && aura.target != null) {
            return aura.target;
        }

        if (mc.currentScreen instanceof ChatScreen) {
            return mc.player;
        }

        return null;
    }
}
