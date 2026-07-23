package antileak.base.client.modules.impl.render.base.implement;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
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
import antileak.base.client.modules.impl.misc.NameProtect;
import antileak.base.client.modules.impl.misc.ScoreboardHP;
import antileak.base.client.modules.impl.render.base.InterfaceProcessing;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TargetHud extends InterfaceProcessing {
    private static final float NAME_SCROLL_OVERFLOW_THRESHOLD = 12.0f;

    private final AnimationUtils alphaAnimation = new AnimationUtils(0.0f, 9.0f, Easings.QUAD_OUT);
    private final AnimationUtils unusualRevealAnimation = new AnimationUtils(0.0f, 4.6f, Easings.QUAD_OUT);
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
    private Framebuffer burnBuffer;
    private int burnBufferWidth = -1;
    private int burnBufferHeight = -1;
    private String lastNameScrollText = "";
    private long nameScrollStartNs = System.nanoTime();
    private final List<ItemStack> armorScratch = new ObjectArrayList<>(4);
    private Font suisse12;
    private Font suisse13;
    private Font suisse14;
    private Font suisse19;
    private Font energy13;

    public TargetHud(Draggable draggable) {
        super(draggable);
    }

    private Font issue(int size) {
        return switch (size) {
            case 12 -> suisse12 != null ? suisse12 : (suisse12 = Fonts.getFont("suisse", 12));
            case 13 -> suisse13 != null ? suisse13 : (suisse13 = Fonts.getFont("suisse", 13));
            case 14 -> suisse14 != null ? suisse14 : (suisse14 = Fonts.getFont("suisse", 14));
            case 19 -> suisse19 != null ? suisse19 : (suisse19 = Fonts.getFont("suisse", 19));
            default -> Fonts.getFont("suisse", size);
        };
    }

    private Font energy(int size) {
        if (size == 13) {
            return energy13 != null ? energy13 : (energy13 = Fonts.getFont("energy", 13));
        }
        return Fonts.getFont("energy", size);
    }

    public boolean isHeadParticlesEnabled() { return headParticlesEnabled; }

    public void setHeadParticlesEnabled(boolean headParticlesEnabled) {
        this.headParticlesEnabled = headParticlesEnabled;
        if (!headParticlesEnabled) headParticles.clear();
    }

    public boolean isHealthBarStyleEnabled() { return healthBarStyleEnabled; }

    public void setHealthBarStyleEnabled(boolean healthBarStyleEnabled) {
        this.healthBarStyleEnabled = healthBarStyleEnabled;
    }

    private int interpolateColor(int color1, int color2, float ratio) {
        ratio = MathHelper.clamp(ratio, 0.0f, 1.0f);
        int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF, a1 = (color1 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF, a2 = (color2 >> 24) & 0xFF;
        return ((int)(a1 + (a2 - a1) * ratio) << 24) | ((int)(r1 + (r2 - r1) * ratio) << 16)
                | ((int)(g1 + (g2 - g1) * ratio) << 8) | (int)(b1 + (b2 - b1) * ratio);
    }

    private void ensureBurnBuffer() {
        if (mc == null || mc.getWindow() == null) return;
        int w = mc.getWindow().getFramebufferWidth();
        int h = mc.getWindow().getFramebufferHeight();
        if (burnBuffer == null || burnBufferWidth != w || burnBufferHeight != h) {
            if (burnBuffer != null) burnBuffer.delete();
            burnBuffer = new SimpleFramebuffer(w, h, true);
            burnBufferWidth = w;
            burnBufferHeight = h;
        }
    }

    private boolean beginBurnPassIfNeeded(boolean unusualAnimation) {
        if (!unusualAnimation) return false;
        ensureBurnBuffer();
        if (burnBuffer == null) return false;
        burnBuffer.setClearColor(0f, 0f, 0f, 0f);
        burnBuffer.clear();
        burnBuffer.beginWrite(false);
        return true;
    }

    private static final class HeadParticle {
        float x, y, vx, vy, size, age, maxAge;
    }

    private void updateAndRenderHeadParticles(MatrixStack matrices, LivingEntity target, float headX, float headY, float headSize, float alpha, int themeColor) {
        if (target == null || alpha <= 0.02f) {
            headParticles.clear();
            particleTarget = target;
            lastTargetHurtTime = 0;
            return;
        }
        long now = System.nanoTime();
        float deltaTicks = MathHelper.clamp((now - lastParticleUpdateNs) / 1_000_000_000.0f * 60.0f, 0.2f, 3.0f);
        lastParticleUpdateNs = now;
        if (particleTarget != target) {
            headParticles.clear();
            particleTarget = target;
            lastTargetHurtTime = Math.max(0, target.hurtTime);
        }
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
                p.x = centerX + MathHelper.cos(angle) * radius;
                p.y = centerY + MathHelper.sin(angle) * radius;
                p.vx = MathHelper.cos(spreadAngle) * speed + (p.x - centerX) * 0.025f;
                p.vy = MathHelper.sin(spreadAngle) * speed + (p.y - centerY) * 0.025f;
                p.size = 3.8f + random.nextFloat() * 1.4f;
                p.age = 0.0f;
                p.maxAge = 74.0f + random.nextFloat() * 42.0f;
                headParticles.add(p);
            }
        }
        float velocityDrag = (float)Math.pow(0.975f, deltaTicks);
        for (int i = headParticles.size() - 1; i >= 0; i--) {
            HeadParticle p = headParticles.get(i);
            p.age += deltaTicks;
            if (p.age >= p.maxAge) { headParticles.remove(i); continue; }
            p.x += p.vx * deltaTicks;
            p.y += p.vy * deltaTicks;
            p.vx *= velocityDrag;
            p.vy *= velocityDrag;
            p.vy += 0.0012f * deltaTicks;
            float life = 1.0f - (p.age / p.maxAge);
            float smoothLife = life * life * (3.0f - 2.0f * life);
            float particleAlpha = alpha * smoothLife;
            if (particleAlpha <= 0.02f) continue;
            RenderUtils.drawRoundedRect(matrices, p.x - p.size * 0.5f, p.y - p.size * 0.5f, p.size, p.size, p.size * 0.45f,
                    ColorUtils.applyAlpha(themeColor, particleAlpha * 0.58f));
        }
    }

    private void drawTargetHudItem(EventRender.Default eventRender, MatrixStack matrices, ItemStack stack,
                                   float slotX, float slotY, float itemScale) {
        if (stack.isEmpty()) return;
        if (itemScale < 0.05f) return;
        matrices.push();
        matrices.translate(slotX + 4.0f, slotY + 4.0f, 0);
        matrices.scale(itemScale, itemScale, 1f);
        matrices.translate(-4, -4, 0);
        eventRender.getContext().drawItem(stack, 0, 0);
        matrices.pop();
    }

    private void safeDrawStringWithFade(MatrixStack matrices, Font font, String text, float x, float y, float maxWidth, int color) {
        if (font == null || text == null || text.isEmpty() || maxWidth < 2.0f || (color >> 24 & 0xFF) < 5) return;
        try { font.drawStringWithFade(matrices, text, x, y, maxWidth, color); } catch (Exception ignored) {}
    }

    private void safeDraw(MatrixStack matrices, Font font, String text, float x, float y, int color) {
        if (font == null || text == null || text.isEmpty() || (color >> 24 & 0xFF) < 5) return;
        try { font.draw(matrices, text, x, y, color); } catch (Exception ignored) {}
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
        phase -= (float)Math.floor(phase);
        float scrollOffset = getPingPongOffset(overflow, phase);
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(x + 1, y - 2.0f, maxWidth, font.getHeight() + 4.0f);
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

    @Override
    public void onRender(EventRender.Default eventRender) {
        DefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    public void DefaultStyle(EventRender.Default eventRender) {
        if (mc.player == null) {
            headParticles.clear();
            lastTargetHurtTime = 0;
            draggable.setWidth(0);
            draggable.setHeight(0);
            return;
        }

        Aura aura = ModuleClass.aura;
        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        LivingEntity auraTarget = aura != null ? aura.getTarget() : null;
        boolean showTargetHud = chatOpen || auraTarget != null;

        alphaAnimation.setSpeed(showTargetHud ? 9.0f : 5.0f);
        alphaAnimation.update(showTargetHud ? 1.0f : 0.0f);
        float alpha = MathHelper.clamp(alphaAnimation.getValue(), 0.0f, 1.0f);
        float renderProgress = alpha;

        if (showTargetHud) lastTarget = chatOpen ? mc.player : auraTarget;

        LivingEntity target = showTargetHud ? (chatOpen ? mc.player : auraTarget) : lastTarget;
        if (target == null || renderProgress <= 0.02f) {
            headParticles.clear();
            lastTargetHurtTime = 0;
            draggable.setWidth(0);
            draggable.setHeight(0);
            goldenAlphaAnimation.setValue(0.0f);
            ABValueAnimation.setValue(0.0f);
            goldenHpAnimation.setValue(0.0f);
            goldenHpTrailAnimation.setValue(0.0f);
            return;
        }

        float currentAbsorption = target.getAbsorptionAmount();
        boolean hasAbsorption = currentAbsorption > 0;

        goldenAlphaAnimation.setSpeed(hasAbsorption ? 9.0f : 5.0f);
        goldenAlphaAnimation.update(hasAbsorption ? 1.0f : 0.0f);
        float goldenAlpha = MathHelper.clamp(goldenAlphaAnimation.getValue(), 0.0f, 1.0f);

        float x = draggable.getX(), y = draggable.getY();
        var theme = elysium.INSTANCE.themeStorage.getThemes().getTheme();
        int colorTheme = !theme.getName().equals("Rainbow") ? theme.color[0] : ColorUtils.getThemeColor();
        int colorTheme2 = !theme.getName().equals("Rainbow") ? ColorUtils.darken(theme.color[0], 0.4f) : ColorUtils.getThemeColor();

        if (showTargetHud) {
            cachedBarThemeColor = colorTheme;
            cachedBarThemeColor2 = colorTheme2;
        }
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
        float displayedHpValue = showTargetHud ? targetHealthForAnim + targetABForAnim : animatedHealthValue + animatedABValue;
        String hpText = String.format("%.1f", displayedHpValue);

        float padding = 3.75f, headSize = 21.0f, rightPad = 4.5f;
        float width = 84.5f, height = 29.0f;
        float headX = x + 4.5f, headY = y + 4.5f;
        float textX = x + 29;
        float topItemY = y + 3.75f, topItemScale = 0.49f * alpha;
        float textMaxWidth = Math.max(7.5f, width - (textX - x) - 21.5f);

        MatrixStack matrices = eventRender.getContext().getMatrices();
        float drawAlpha = alpha, hpBarAlpha = drawAlpha;
        boolean drawSquares = isUnusualRectType();
        int drawAlphaInt = (int)(255.0f * drawAlpha);

        if (drawAlphaInt < 5) {
            draggable.setWidth(width);
            draggable.setHeight(height);
            return;
        }

        matrices.push();

        RenderUtils.drawDefaultHudPanel(matrices, x, y, width, height, 6, 6,
                ColorUtils.applyAlpha(ColorUtils.rgba(50, 50, 50, 255), drawAlpha),
                ColorUtils.applyAlpha(ColorUtils.darken(colorTheme, 0.15f), drawAlpha),
                ColorUtils.applyAlpha(ColorUtils.darken(colorTheme, 0.05f), drawAlpha));
        if (drawSquares && drawAlpha > 0.06f) {
            RenderUtils.drawHudSquarePattern(matrices, x + 6.0f, y, width, height, ColorUtils.applyAlpha(barThemeColor, drawAlpha));
        }

        if (headParticlesEnabled) {
            updateAndRenderHeadParticles(matrices, target, headX, headY, headSize, drawAlpha, barThemeColor);
        } else {
            headParticles.clear();
        }

        if (target instanceof PlayerEntity playerEntity) {
            RenderUtils.drawPlayerHead(matrices, playerEntity.getUuid(), headX, headY - 0.5f, headSize, 5f, drawAlpha, 0.0f);
        } else {
            Font font19 = issue(19);
            if (font19 != null) {
                try { font19.drawCenteredString(matrices, "?", (headX + headSize / 2.0f) - 1, headY + 6.4f, ColorUtils.rgba(220, 220, 220, drawAlphaInt)); } catch (Exception ignored) {}
            }
        }

        drawScrollingName(matrices, issue(13), name, textX + 0.5f, y + 5.5f, textMaxWidth, ColorUtils.rgba(255, 255, 255, drawAlphaInt));
        int textThemeColor = ColorUtils.setAlphaColor(colorTheme, drawAlphaInt);
        safeDraw(matrices, energy(13), "v", textX, y + 15f, textThemeColor);
        safeDrawStringWithFade(matrices, issue(12), hpText, textX + 8.25f, y + 14.85f,
                Math.max(7.5f, textMaxWidth - 8.25f), textThemeColor);

        ItemStack topMainHand = target.getMainHandStack();
        ItemStack topOffHand = target.getOffHandStack();
        if (topItemScale > 0.1f) {
            if (!topMainHand.isEmpty()) drawTargetHudItem(eventRender, matrices, topMainHand, x + width - 18.7f, topItemY - 3.5f, topItemScale);
            if (!topOffHand.isEmpty()) drawTargetHudItem(eventRender, matrices, topOffHand, x + width - 12.2f, topItemY - 3.5f, topItemScale);
        }

        float barX = textX, barY = y + height - 6.9f;
        float barW = Math.max(14.2f, width - rightPad - (barX - x) - 1), barH = 3.5f;
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

        if (hpBarAlpha > 0.025f && barW > 2.0f) {
            RenderUtils.drawGradientRect(matrices, barX, barY, barW + 2.25f, barH, 1, barBgLeft, barBgRight, true);
            if (!hidingHud) {
                float trailProgressDraw = MathHelper.lerp(0.58f, hpTrailProgressAnimated, hpProgressAnimated);
                float trailW = barW * trailProgressDraw;
                if (trailW > 1.15f) RenderUtils.drawGradientRect(matrices, barX, barY, trailW + 2.25f, barH, 1, barTrailLeft, barTrailRight, true);
            }
            float filledW = barW * hpProgressAnimated;
            if (filledW > 1.15f) RenderUtils.drawGradientRect(matrices, barX, barY, filledW + 2.25f, barH, 1, barFillLeft, barFillRight, true);
            if (goldenAlpha > 0.01f) {
                float goldenBarAlpha = goldenAlpha * hpBarAlpha;
                int goldenBaseLeft = ColorUtils.rgba(147, 108, 16, 255);
                int goldenBaseRight = ColorUtils.rgba(236, 183, 39, 255);
                int goldenTrailLeft = ColorUtils.applyAlpha(ColorUtils.darken(goldenBaseLeft, 0.7f), goldenBarAlpha * 0.5f);
                int goldenTrailRight = ColorUtils.applyAlpha(ColorUtils.darken(goldenBaseRight, 0.7f), goldenBarAlpha * 0.5f);
                int goldenFillLeft = ColorUtils.applyAlpha(ColorUtils.darken(goldenBaseLeft, 0.7f), goldenBarAlpha);
                int goldenFillRight = ColorUtils.applyAlpha(goldenBaseRight, goldenBarAlpha);
                if (!hidingHud && hasAbsorption) {
                    float goldenTrailProgressDraw = MathHelper.lerp(0.58f, goldenTrailProgressAnimated, goldenProgressAnimated);
                    float goldenTrailW = barW * goldenTrailProgressDraw;
                    if (goldenTrailW > 1.15f) RenderUtils.drawGradientRect(matrices, barX, barY, goldenTrailW + 2.25f, barH, 1, goldenTrailLeft, goldenTrailRight, true);
                }
                float goldenFilledW = barW * goldenProgressAnimated;
                if (goldenFilledW > 1.15f) RenderUtils.drawGradientRect(matrices, barX, barY, goldenFilledW + 2.25f, barH, 1, goldenFillLeft, goldenFillRight, true);
            }
        }

        matrices.pop();
        draggable.setWidth(width);
        draggable.setHeight(height);
    }
}