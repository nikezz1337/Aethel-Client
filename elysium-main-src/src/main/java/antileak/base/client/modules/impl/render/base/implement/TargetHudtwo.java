package antileak.base.client.modules.impl.render.base.implement;

import antileak.base.client.modules.impl.misc.NameProtect;
import antileak.base.client.modules.impl.misc.ScoreboardHP;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import antileak.base.elysium;
import antileak.base.api.events.implement.EventRender;
import antileak.base.api.storages.implement.helpertstorages.enumvar.ModuleClass;
import antileak.base.api.utils.animation.AnimationUtils;
import antileak.base.api.utils.animation.Easings;
import antileak.base.api.utils.color.ColorUtils;
import antileak.base.api.utils.draggable.Draggable;
import antileak.base.api.utils.render.RenderUtils;
import antileak.base.api.utils.render.fonts.msdf.Font;
import antileak.base.api.utils.render.fonts.msdf.Fonts;
import antileak.base.api.utils.scissor.ScissorUtils;
import antileak.base.client.modules.impl.combat.Aura;
import antileak.base.client.modules.impl.render.base.InterfaceProcessing;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TargetHudtwo extends InterfaceProcessing {
    private static final float NAME_SCROLL_OVERFLOW_THRESHOLD = 12.0f;

    private final AnimationUtils alphaAnimation = new AnimationUtils(0.0f, 9.0f, Easings.QUAD_OUT);
    private final AnimationUtils hpAnimation = new AnimationUtils(1.0f, 9.2f, Easings.QUAD_OUT);
    private final AnimationUtils hpTrailAnimation = new AnimationUtils(1.0f, 7.4f, Easings.QUAD_OUT);
    private final AnimationUtils hpValueAnimation = new AnimationUtils(20.0f, 7.0f, Easings.QUAD_OUT);
    private final AnimationUtils ABValueAnimation = new AnimationUtils(0.0f, 7.0f, Easings.QUAD_OUT);
    private final AnimationUtils goldenHpAnimation = new AnimationUtils(0.0f, 9.2f, Easings.QUAD_OUT);
    private final AnimationUtils goldenHpTrailAnimation = new AnimationUtils(0.0f, 7.4f, Easings.QUAD_OUT);
    private final AnimationUtils goldenAlphaAnimation = new AnimationUtils(0.0f, 9.0f, Easings.QUAD_OUT);
    private final List<HeadParticle> headParticles = new ObjectArrayList<>();

    private LivingEntity lastTarget;
    private float maxAbsorption = 20.0f;
    private boolean headParticlesEnabled = true;
    private boolean healthBarStyleEnabled = false;
    private long lastParticleUpdateNs = System.nanoTime();
    private LivingEntity particleTarget;
    private int lastTargetHurtTime = 0;
    private int cachedBarThemeColor = ColorUtils.rgba(124, 91, 242, 255);
    private int cachedBarThemeColor2 = ColorUtils.rgba(93, 67, 175, 255);
    private final ItemStack[] displayItems = new ItemStack[6];
    private final ItemStack[] armorScratch = new ItemStack[4];
    private String lastNameScrollText = "";
    private long nameScrollStartNs = System.nanoTime();

    public TargetHudtwo(Draggable draggable) {
        super(draggable);
    }

    private Font issue(int size) { return Fonts.getFont("suisse", size); }
    public boolean isHeadParticlesEnabled() { return headParticlesEnabled; }
    public void setHeadParticlesEnabled(boolean v) { this.headParticlesEnabled = v; if (!v) headParticles.clear(); }
    public boolean isHealthBarStyleEnabled() { return healthBarStyleEnabled; }
    public void setHealthBarStyleEnabled(boolean v) { this.healthBarStyleEnabled = v; }

    private void safeDrawStringWithFade(MatrixStack matrices, Font font, String text, float x, float y, float maxWidth, int color) {
        if (font == null || text == null || text.isEmpty() || maxWidth < 2.0f || (color >> 24 & 0xFF) < 5) return;
        try { font.drawStringWithFade(matrices, text, x, y, maxWidth, color); } catch (Exception ignored) {}
    }

    private void safeDraw(MatrixStack matrices, Font font, String text, float x, float y, int color) {
        if (font == null || text == null || text.isEmpty() || (color >> 24 & 0xFF) < 5) return;
        try { font.draw(matrices, text, x, y, color); } catch (Exception ignored) {}
    }

    private void safeDrawGradientHorizontal(MatrixStack matrices, Font font, String text, float x, float y, int color1, int color2) {
        if (font == null || text == null || text.isEmpty() || (color1 >> 24 & 0xFF) < 5) return;
        try { font.drawGradientStringHorizontal(matrices, text, x, y, color1, color2); } catch (Exception ignored) {}
    }

    private void drawScrollingName(MatrixStack matrices, Font font, String text, float x, float y, float maxWidth, int color) {
        if (font == null || text == null || text.isEmpty() || maxWidth < 2.0f || (color >> 24 & 0xFF) < 5) return;
        float textWidth = font.getWidth(text);
        float overflow = textWidth - maxWidth;
        if (overflow <= NAME_SCROLL_OVERFLOW_THRESHOLD) {
            safeDraw(matrices, font, text, x, y, color);
            return;
        }
        if (!text.equals(lastNameScrollText)) {
            lastNameScrollText = text;
            nameScrollStartNs = System.nanoTime();
        }
        float phase = ((System.nanoTime() - nameScrollStartNs) / 1_000_000_000.0f) / Math.max(2.4f, overflow / 5.0f);
        phase -= (float) Math.floor(phase);
        float scrollOffset = getPingPongOffset(overflow, phase);
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(x + 1, y - 2.0f, maxWidth , font.getHeight() + 4.0f);
        safeDraw(matrices, font, text, x - scrollOffset + 0.5f, y, color);
        ScissorUtils.pop();
    }

    private float getPingPongOffset(float maxOffset, float phase) {
        if (maxOffset <= 0.0f) return 0.0f;
        float pingPong = phase < 0.5f ? (phase * 2.0f) : (2.0f - phase * 2.0f);
        float eased = pingPong * pingPong * (3.0f - 2.0f * pingPong);
        return maxOffset * eased;
    }

    private String getDisplayName(LivingEntity target) {
        String raw = target.getName().getString();
        if (raw == null || raw.isEmpty()) raw = "Unknown";
        NameProtect np = NameProtect.INSTANCE;
        if (np != null && np.isEnable()) {
            String patched = np.patch(raw);
            if (patched != null) return patched;
        }
        return raw;
    }

    private int interpolateColor(int color1, int color2, float ratio) {
        ratio = MathHelper.clamp(ratio, 0.0f, 1.0f);
        int r1 = (color1 >> 16) & 0xFF; int g1 = (color1 >> 8) & 0xFF; int b1 = color1 & 0xFF; int a1 = (color1 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF; int g2 = (color2 >> 8) & 0xFF; int b2 = color2 & 0xFF; int a2 = (color2 >> 24) & 0xFF;
        return ((int)(a1 + (a2 - a1) * ratio) << 24) | ((int)(r1 + (r2 - r1) * ratio) << 16) | ((int)(g1 + (g2 - g1) * ratio) << 8) | (int)(b1 + (b2 - b1) * ratio);
    }

    private int collectDisplayItems(LivingEntity target) {
        int armorCount = 0;
        for (ItemStack stack : target.getArmorItems()) { if (armorCount < armorScratch.length) armorScratch[armorCount++] = stack; }
        int count = 0;
        for (int i = armorCount - 1; i >= 0; i--) { displayItems[count++] = armorScratch[i]; armorScratch[i] = ItemStack.EMPTY; }
        ItemStack mainHand = target.getMainHandStack();
        if (!mainHand.isEmpty()) displayItems[count++] = mainHand;
        ItemStack offHand = target.getOffHandStack();
        if (!offHand.isEmpty()) displayItems[count++] = offHand;
        return count;
    }

    private static final class HeadParticle {
        float x, y, vx, vy, size, age, maxAge;
    }

    private void updateAndRenderHeadParticles(MatrixStack matrices, LivingEntity target, float headX, float headY, float headSize, float alpha, int themeColor) {
        if (target == null || alpha <= 0.02f) { headParticles.clear(); particleTarget = target; lastTargetHurtTime = 0; return; }
        long now = System.nanoTime();
        float deltaTicks = MathHelper.clamp((now - lastParticleUpdateNs) / 1_000_000_000.0f * 60.0f, 0.2f, 3.0f);
        lastParticleUpdateNs = now;
        if (particleTarget != target) { headParticles.clear(); particleTarget = target; lastTargetHurtTime = Math.max(0, target.hurtTime); }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float centerX = headX + headSize * 0.5f, centerY = headY + headSize * 0.5f;
        int hurtTime = Math.max(0, target.hurtTime);
        boolean spawnBurst = hurtTime > 0 && (hurtTime > lastTargetHurtTime || hurtTime % 3 == 0);
        lastTargetHurtTime = hurtTime;
        if (spawnBurst) {
            int burstCount = 1 + random.nextInt(2);
            for (int n = 0; n < burstCount && headParticles.size() < 14; n++) {
                float angle = (float)(random.nextDouble() * Math.PI * 2.0);
                float radius = random.nextFloat() * headSize * 0.24f;
                float spreadAngle = (float)(random.nextDouble() * Math.PI * 2.0);
                float speed = 0.58f + random.nextFloat() * 0.9f;
                HeadParticle p = new HeadParticle();
                p.x = centerX + MathHelper.cos(angle) * radius; p.y = centerY + MathHelper.sin(angle) * radius;
                p.vx = MathHelper.cos(spreadAngle) * speed + (p.x - centerX) * 0.025f;
                p.vy = MathHelper.sin(spreadAngle) * speed + (p.y - centerY) * 0.025f;
                p.size = 3.8f + random.nextFloat() * 1.4f; p.age = 0.0f; p.maxAge = 74.0f + random.nextFloat() * 42.0f;
                headParticles.add(p);
            }
        }
        for (int i = headParticles.size() - 1; i >= 0; i--) {
            HeadParticle p = headParticles.get(i);
            p.age += deltaTicks;
            if (p.age >= p.maxAge) { headParticles.remove(i); continue; }
            p.x += p.vx * deltaTicks; p.y += p.vy * deltaTicks;
            p.vx *= (float)Math.pow(0.975f, deltaTicks); p.vy *= (float)Math.pow(0.975f, deltaTicks);
            p.vy += 0.0012f * deltaTicks;
            float life = 1.0f - (p.age / p.maxAge);
            float smoothLife = life * life * (3.0f - 2.0f * life);
            float particleAlpha = alpha * smoothLife;
            if (particleAlpha <= 0.02f) continue;
            RenderUtils.drawRoundedRect(matrices, p.x - p.size * 0.5f, p.y - p.size * 0.5f, p.size, p.size, p.size * 0.45f,
                    ColorUtils.applyAlpha(themeColor, particleAlpha * 0.58f));
        }
    }

    private void drawTargetHudItem(EventRender.Default eventRender, MatrixStack matrices, ItemStack stack, float slotX, float slotY, float itemScale) {
        if (stack.isEmpty()) return;
        if (itemScale < 0.05f) return;
        matrices.push();
        matrices.translate(slotX + 4.0f, slotY + 4.0f, 0);
        matrices.scale(itemScale, itemScale, 1f);
        matrices.translate(-4, -4, 0);
        eventRender.getContext().drawItem(stack, 0, 0);
        matrices.pop();
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        render(eventRender);
        super.onRender(eventRender);
    }

    public void render(EventRender.Default eventRender) {
        if (mc.player == null) {
            headParticles.clear(); lastTargetHurtTime = 0;
            draggable.setWidth(0); draggable.setHeight(0); return;
        }

        Aura aura = ModuleClass.aura;
        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        LivingEntity auraTarget = aura != null ? aura.getTarget() : null;
        boolean showTargetHud = chatOpen || auraTarget != null;

        alphaAnimation.setSpeed(showTargetHud ? 9.0f : 5.0f);
        alphaAnimation.update(showTargetHud ? 1.0f : 0.0f);
        float alpha = MathHelper.clamp(alphaAnimation.getValue(), 0.0f, 1.0f);

        if (showTargetHud) lastTarget = chatOpen ? mc.player : auraTarget;

        LivingEntity target = showTargetHud ? (chatOpen ? mc.player : auraTarget) : lastTarget;
        if (target == null || alpha <= 0.01f) {
            headParticles.clear(); lastTargetHurtTime = 0;
            draggable.setWidth(0); draggable.setHeight(0);
            goldenAlphaAnimation.setValue(0.0f); ABValueAnimation.setValue(0.0f);
            goldenHpAnimation.setValue(0.0f); goldenHpTrailAnimation.setValue(0.0f); return;
        }

        float currentAbsorption = target.getAbsorptionAmount();
        boolean hasAbsorption = currentAbsorption > 0;
        goldenAlphaAnimation.setSpeed(hasAbsorption ? 9.0f : 5.0f);
        goldenAlphaAnimation.update(hasAbsorption ? 1.0f : 0.0f);
        float goldenAlpha = MathHelper.clamp(goldenAlphaAnimation.getValue(), 0.0f, 1.0f);

        float x = draggable.getX(), y = draggable.getY();
        int colorTheme = !elysium.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")
                ? elysium.INSTANCE.themeStorage.getThemes().getTheme().color[0]
                : ColorUtils.getThemeColor();
        int colorTheme2 = !elysium.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")
                ? ColorUtils.darken(elysium.INSTANCE.themeStorage.getThemes().getTheme().color[0], 0.4f)
                : ColorUtils.getThemeColor();
        if (showTargetHud) { cachedBarThemeColor = colorTheme; cachedBarThemeColor2 = colorTheme2; }
        int barThemeColor = showTargetHud ? colorTheme : cachedBarThemeColor;
        int barThemeColor2 = showTargetHud ? colorTheme2 : cachedBarThemeColor2;

        float maxHealth = Math.max(1.0f, target.getMaxHealth());
        float maxAB = Math.max(1.0f, maxAbsorption);
        float targetHealthForAnim = showTargetHud ? ScoreboardHP.getHealth(target) : 0.0f;
        float targetABForAnim = showTargetHud ? currentAbsorption : 0.0f;
        hpValueAnimation.update(targetHealthForAnim);
        ABValueAnimation.update(targetABForAnim);
        float animatedHealthValue = MathHelper.clamp(hpValueAnimation.getValue(), 0.0f, maxHealth);
        float animatedABValue = MathHelper.clamp(ABValueAnimation.getValue(), 0.0f, maxAB);

        float healthProgress = MathHelper.clamp(targetHealthForAnim / maxHealth, 0.0f, 1.0f);
        hpAnimation.update(healthProgress);
        float hpProgressAnimated = MathHelper.clamp(hpAnimation.getValue(), 0.0f, 1.0f);

        if (hpProgressAnimated > hpTrailAnimation.getValue()) {
            hpTrailAnimation.setValue(MathHelper.lerp(0.78f, hpTrailAnimation.getValue(), hpProgressAnimated));
        } else {
            hpTrailAnimation.update(hpProgressAnimated);
        }

        float hpTrailProgressAnimated = MathHelper.clamp(hpTrailAnimation.getValue(), 0.0f, 1.0f);
        boolean hidingHud = !showTargetHud;
        if (hidingHud) hpTrailProgressAnimated = hpProgressAnimated;

        float absorptionProgress = MathHelper.clamp(currentAbsorption / maxAB, 0.0f, 1.0f);
        goldenHpAnimation.update(absorptionProgress);
        float goldenProgressAnimated = MathHelper.clamp(goldenHpAnimation.getValue(), 0.0f, 1.0f);
        if (goldenProgressAnimated > goldenHpTrailAnimation.getValue()) {
            goldenHpTrailAnimation.setValue(MathHelper.lerp(0.78f, goldenHpTrailAnimation.getValue(), goldenProgressAnimated));
        } else {
            goldenHpTrailAnimation.update(goldenProgressAnimated);
        }
        float goldenTrailProgressAnimated = MathHelper.clamp(goldenHpTrailAnimation.getValue(), 0.0f, 1.0f);
        if (hidingHud || !hasAbsorption) goldenTrailProgressAnimated = goldenProgressAnimated;

        String name = getDisplayName(target);
        String hpText = "HP: " + String.format("%.1f", animatedHealthValue);
        String abText = " (" + String.format("%.1f", animatedABValue) + ")";

        Font font14 = issue(14);
        Font font13 = issue(13);

        float headSize = 27.5f, gap = 5.0f, padding = 4.0f, rightPad = 6.0f;
        float textX = x + padding + headSize + gap - 5;
        float width = 92.5f;
        float height = 32.0f;
        float headX = x + padding, headY = y + 3.5f;
        float textMaxWidth = width - (textX - x) - rightPad - 4;

        MatrixStack matrices = eventRender.getContext().getMatrices();
        float drawAlpha = alpha, hpBarAlpha = drawAlpha;
        int drawAlphaInt = (int)(255.0f * drawAlpha);

        if (drawAlphaInt < 5) {
            draggable.setWidth(width);
            draggable.setHeight(height);
            return;
        }

        matrices.push();

        RenderUtils.drawDefaultHudPanel1(matrices, x, y, width, height, 4, 4.5f, ColorUtils.applyAlpha(ColorUtils.rgba(50, 50, 50, 255), drawAlpha), ColorUtils.applyAlpha(ColorUtils.darken(colorTheme, 0.15f), drawAlpha), ColorUtils.applyAlpha(ColorUtils.darken(colorTheme, 0.05f), drawAlpha));
        if (isUnusualRectType() && drawAlpha > 0.06f) {
            RenderUtils.drawHudSquarePattern(matrices, x + 8, y, width, height, ColorUtils.applyAlpha(barThemeColor, drawAlpha));
        }

        if (headParticlesEnabled) {
            updateAndRenderHeadParticles(matrices, target, headX - 1.85f, headY - 1.0f, headSize, drawAlpha, barThemeColor);
        } else {
            headParticles.clear();
        }

        if (target instanceof PlayerEntity playerEntity) {
            RenderUtils.drawPlayerHead(matrices, playerEntity.getUuid(), headX - 1.85f, headY - 1, headSize, 3.5f, drawAlpha, 0.0f);
        } else {
            Font font26 = issue(26);
            if (font26 != null) {
                try { font26.drawCenteredString(matrices, "?", headX + headSize / 2.25f, headY + 7.5, ColorUtils.rgba(220, 220, 220, drawAlphaInt)); } catch (Exception ignored) {}
            }
        }

        drawScrollingName(matrices, font14, name, textX + 0.7f, y + 5.5f, textMaxWidth, ColorUtils.rgba(255, 255, 255, drawAlphaInt));
        safeDrawStringWithFade(matrices, font13, hpText, textX + 1, y + 14.5f, textMaxWidth, ColorUtils.rgba(232, 232, 232, drawAlphaInt));

        if (goldenAlpha > 0.01f) {
            float hpTextWidth = (font13 != null) ? font13.getWidth(hpText) : 0f;
            int goldenAlphaInt = (int)(255.0f * goldenAlpha * alpha);
            safeDrawGradientHorizontal(matrices, font13, abText, textX + 1 + hpTextWidth, y + 14.5f,
                    ColorUtils.rgba(236, 183, 39, goldenAlphaInt), ColorUtils.rgba(147, 108, 16, goldenAlphaInt));
        }

        float barX = textX, barY = y + height - 10.45f;
        float barW = Math.max(19.0f, width - rightPad - (barX - x));
        float barH = 3.85f;
        float hpRatio = healthProgress;

        int barBgLeft, barBgRight, barTrailLeft, barTrailRight, barFillLeft, barFillRight;
        if (healthBarStyleEnabled) {
            int redDarkBg = ColorUtils.rgba(70, 5, 10, (int)(115.0f * hpBarAlpha));
            int greenDarkBg = ColorUtils.rgba(0, 70, 0, (int)(115.0f * hpBarAlpha));
            int redBrightBg = ColorUtils.rgba(155, 5, 15, (int)(115.0f * hpBarAlpha));
            int greenBrightBg = ColorUtils.rgba(55, 205, 15, (int)(115.0f * hpBarAlpha));
            barBgLeft = interpolateColor(redDarkBg, greenDarkBg, hpRatio);
            barBgRight = interpolateColor(redBrightBg, greenBrightBg, hpRatio);
            int redDarkTrail = ColorUtils.rgba(70, 5, 10, (int)(200.0f * hpBarAlpha));
            int greenDarkTrail = ColorUtils.rgba(0, 70, 0, (int)(200.0f * hpBarAlpha));
            int redBrightTrail = ColorUtils.rgba(155, 5, 15, (int)(200.0f * hpBarAlpha));
            int greenBrightTrail = ColorUtils.rgba(55, 205, 15, (int)(200.0f * hpBarAlpha));
            barTrailLeft = interpolateColor(redDarkTrail, greenDarkTrail, hpRatio);
            barTrailRight = interpolateColor(redBrightTrail, greenBrightTrail, hpRatio);
            int redDarkFill = ColorUtils.rgba(70, 5, 10, (int)(250.0f * hpBarAlpha));
            int greenDarkFill = ColorUtils.rgba(0, 70, 0, (int)(250.0f * hpBarAlpha));
            int redBrightFill = ColorUtils.rgba(155, 5, 15, (int)(250.0f * hpBarAlpha));
            int greenBrightFill = ColorUtils.rgba(55, 205, 15, (int)(250.0f * hpBarAlpha));
            barFillLeft = interpolateColor(redDarkFill, greenDarkFill, hpRatio);
            barFillRight = interpolateColor(redBrightFill, greenBrightFill, hpRatio);
        } else {
            barBgLeft = ColorUtils.applyAlpha(ColorUtils.darken(barThemeColor2, 0.72f), hpBarAlpha * 0.26f);
            barBgRight = ColorUtils.applyAlpha(ColorUtils.darken(barThemeColor, 0.72f), hpBarAlpha * 0.26f);
            barTrailLeft = ColorUtils.applyAlpha(ColorUtils.darken(barThemeColor2, 0.9f), hpBarAlpha * 0.42f);
            barTrailRight = ColorUtils.applyAlpha(ColorUtils.darken(barThemeColor, 0.9f), hpBarAlpha * 0.42f);
            barFillLeft = ColorUtils.applyAlpha(barThemeColor2, hpBarAlpha);
            barFillRight = ColorUtils.applyAlpha(barThemeColor, hpBarAlpha);
        }

        if (hpBarAlpha > 0.025f) {
            RenderUtils.drawGradientRect(matrices, barX, barY, barW + 3, barH + 4.25f, 1.95f, barBgLeft, barBgRight, true);
            if (!hidingHud) {
                float trailW = barW * MathHelper.lerp(0.58f, hpTrailProgressAnimated, hpProgressAnimated);
                if (trailW > 1.15f) RenderUtils.drawGradientRect(matrices, barX, barY, trailW + 3, barH + 4.25f, 1.95f, barTrailLeft, barTrailRight, true);
            }
            float filledW = barW * hpProgressAnimated;
            if (filledW > 1.15f) RenderUtils.drawGradientRect(matrices, barX, barY, filledW + 3, barH + 4.25f, 1.95f, barFillLeft, barFillRight, true);

            if (goldenAlpha > 0.01f) {
                float goldenBarAlpha = goldenAlpha * hpBarAlpha;
                int goldenBaseLeft = ColorUtils.rgba(147, 108, 16, 255);
                int goldenBaseRight = ColorUtils.rgba(236, 183, 39, 255);
                int goldenTrailLeft = ColorUtils.applyAlpha(ColorUtils.darken(goldenBaseLeft, 0.7f), goldenBarAlpha * 0.5f);
                int goldenTrailRight = ColorUtils.applyAlpha(ColorUtils.darken(goldenBaseRight, 0.7f), goldenBarAlpha * 0.5f);
                int goldenFillLeft = ColorUtils.applyAlpha(ColorUtils.darken(goldenBaseLeft, 0.7f), goldenBarAlpha);
                int goldenFillRight = ColorUtils.applyAlpha(goldenBaseRight, goldenBarAlpha);
                if (!hidingHud && hasAbsorption) {
                    float goldenTrailW = barW * MathHelper.lerp(0.58f, goldenTrailProgressAnimated, goldenProgressAnimated);
                    if (goldenTrailW > 1.15f) RenderUtils.drawGradientRect(matrices, barX, barY, goldenTrailW + 3, barH + 4.25f, 1.95f, goldenTrailLeft, goldenTrailRight, true);
                }
                float goldenFilledW = barW * goldenProgressAnimated;
                if (goldenFilledW > 1.15f) RenderUtils.drawGradientRect(matrices, barX, barY, goldenFilledW + 3, barH + 4.25f, 1.95f, goldenFillLeft, goldenFillRight, true);
            }
        }

        float itemY = y - 11.5f, itemSpacing = 9, itemScale = 0.5f * alpha;
        int totalSlots = collectDisplayItems(target);
        float itemX = x + width - (totalSlots * itemSpacing) - 3f;
        for (int itemIndex = 4; itemIndex < totalSlots; itemIndex++) {
            ItemStack stack = displayItems[itemIndex];
            if (!stack.isEmpty()) drawTargetHudItem(eventRender, matrices, stack, itemX + itemIndex * itemSpacing - 1, itemY, itemScale);
            displayItems[itemIndex] = ItemStack.EMPTY;
        }

        matrices.pop();
        draggable.setWidth(width);
        draggable.setHeight(height);
    }
}