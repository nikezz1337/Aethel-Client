package antileak.base.client.modules.impl.render.base.implement;

import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import antileak.base.elysium;
import antileak.base.api.events.implement.EventRender;
import antileak.base.api.utils.animation.AnimationUtils;
import antileak.base.api.utils.animation.Easings;
import antileak.base.api.utils.color.ColorUtils;
import antileak.base.api.utils.draggable.Draggable;
import antileak.base.api.utils.render.RenderUtils;
import antileak.base.api.utils.render.fonts.msdf.Font;
import antileak.base.api.utils.render.fonts.msdf.Fonts;
import antileak.base.api.utils.scissor.ScissorUtils;
import antileak.base.client.modules.impl.render.base.InterfaceProcessing;

import java.util.*;

public class Potions extends InterfaceProcessing {
    private static final float BASE_MIN_WIDTH   = 70f;
    private static final float EXTRA_WIDTH      = 18f;
    private static final float ROW_RIGHT_MARGIN = 18f;

    private static final int EXPIRING_TICKS = 5 * 20;

    private static final float PULSE_SPEED_EXPIRING = 3.8f;
    private static final float PULSE_SPEED_BAD      = 1.6f;

    private static final class PotionSnapshot {
        RegistryEntry<StatusEffect> entry;
        String baseName;
        int amplifier, duration;
        boolean infinite;
    }

    private Font issue(int size) { return Fonts.getFont("suisse", size); }

    private final Map<StatusEffect, AnimationUtils> animations  = new LinkedHashMap<>();
    private final Map<StatusEffect, PotionSnapshot> snapshots   = new HashMap<>();
    private final Map<StatusEffect, Integer>        maxDurations = new HashMap<>();
    private final Set<StatusEffect>                 renderOrderSeen = new HashSet<>();
    private final AnimationUtils widthAnimation = new AnimationUtils(70, 10.5f, Easings.QUAD_OUT);

    public Potions(Draggable draggable) { super(draggable); }

    private antileak.base.api.utils.render.fonts.ttf.MCFontRenderer sf_regular(int size) {
        return antileak.base.api.utils.render.fonts.ttf.Fonts.getFont("sf_regular.ttf", size);
    }
    private Font icon(int size) { return Fonts.getFont("icon", size); }
    private antileak.base.api.utils.render.fonts.ttf.MCFontRenderer myfont(int size) {
        return antileak.base.api.utils.render.fonts.ttf.Fonts.getFont("myfont.ttf", size);
    }

    private AnimationUtils getAnimation(StatusEffect effect) {
        return animations.computeIfAbsent(effect, e -> new AnimationUtils(0, 10.5f, Easings.QUAD_OUT));
    }

    private static String getLevelSuffix(int level) {
        int n = Math.max(1, level);
        return switch (n) {
            case 1  -> "I";
            case 2  -> "II";
            case 3  -> "III";
            case 4  -> "IV";
            case 5  -> "V";
            case 6  -> "VI";
            case 7  -> "VII";
            case 8  -> "VIII";
            case 9  -> "IX";
            case 10 -> "X";
            default -> "X".repeat(n / 10) + getLevelSuffix(n % 10 == 0 ? 10 : n % 10);
        };
    }

    private static String formatDuration(StatusEffectInstance effect) {
        return formatDuration(effect.getDuration(), effect.isInfinite());
    }

    private static String formatDuration(int duration, boolean infinite) {
        if (infinite) return "inf";
        int seconds = Math.max(0, duration / 20);
        int secs = seconds % 60;
        return (seconds / 60) + ":" + (secs < 10 ? "0" + secs : String.valueOf(secs));
    }

    private void updateSnapshot(StatusEffectInstance effect) {
        StatusEffect type = effect.getEffectType().value();
        PotionSnapshot s = snapshots.computeIfAbsent(type, e -> new PotionSnapshot());
        s.entry     = effect.getEffectType();
        s.baseName  = I18n.translate(effect.getTranslationKey());
        s.amplifier = effect.getAmplifier() + 1;
        s.duration  = effect.getDuration();
        s.infinite  = effect.isInfinite();
    }

    private List<StatusEffect> buildRenderOrder(Collection<StatusEffectInstance> effects, Set<StatusEffect> active) {
        List<StatusEffect> order = new ArrayList<>();
        renderOrderSeen.clear();
        for (StatusEffectInstance effect : effects) {
            StatusEffect type = effect.getEffectType().value();
            if (renderOrderSeen.add(type)) order.add(type);
        }
        for (StatusEffect type : animations.keySet()) if (!active.contains(type)) order.add(type);
        return order;
    }

    private void drawEffectIcon(EventRender.Default eventRender, RegistryEntry<StatusEffect> effect,
                                float x, float y, int size, int alpha) {
        Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(effect);
        RenderUtils.drawSprite(eventRender.getContext().getMatrices(), sprite, x, y, size, ColorUtils.rgba(255, 255, 255, alpha));
    }

    private void drawEffectIconTinted(EventRender.Default eventRender, RegistryEntry<StatusEffect> effect,
                                      float x, float y, int size, int tintColor) {
        Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(effect);
        RenderUtils.drawSprite(eventRender.getContext().getMatrices(), sprite, x, y, size, tintColor);
    }

    private static boolean isBadEffect(StatusEffect effect) {
        return effect.getCategory() == StatusEffectCategory.HARMFUL;
    }

    private static float getPulse(float speedHz, float phaseOffset) {
        float t = (System.currentTimeMillis() / 1000.0f) * speedHz * (float)(Math.PI * 2.0);
        float raw = (float) Math.sin(t + phaseOffset);
        float normalized = (raw + 1.0f) * 0.5f;
        return normalized * normalized;
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        DefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    public void DefaultStyle(EventRender.Default eventRender) {
        float x = draggable.getX(), y = draggable.getY();
        int colorTheme;
        if (!elysium.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            colorTheme = elysium.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        } else {
            colorTheme = ColorUtils.getThemeColor();
        }

        float targetWidth = BASE_MIN_WIDTH, targetHeight = 16;
        int visibleCount = 0;

        Collection<StatusEffectInstance> effects = mc != null && mc.player != null
                ? mc.player.getStatusEffects() : List.of();

        Set<StatusEffect> active = new HashSet<>();
        for (StatusEffectInstance effect : effects) {
            StatusEffect type = effect.getEffectType().value();
            active.add(type);
            getAnimation(type).update(1);
            updateSnapshot(effect);
            int duration = effect.getDuration();
            Integer prevMax = maxDurations.get(type);
            if (prevMax == null || duration > prevMax) maxDurations.put(type, duration);
        }
        for (Map.Entry<StatusEffect, AnimationUtils> e : animations.entrySet())
            if (!active.contains(e.getKey())) e.getValue().update(0);

        List<StatusEffect> renderOrder = buildRenderOrder(effects, active);

        for (StatusEffect type : renderOrder) {
            float animValue = getAnimation(type).getValue();
            PotionSnapshot snapshot = snapshots.get(type);
            if (animValue > 0.01f && snapshot != null) {
                visibleCount++;
                String baseName = snapshot.baseName != null ? snapshot.baseName : I18n.translate(type.getTranslationKey());
                float nameWidth = issue(12).getWidth(baseName);
                if (!getLevelSuffix(snapshot.amplifier).isEmpty())
                    nameWidth += issue(12).getWidth(" " + getLevelSuffix(snapshot.amplifier));
                float rowWidth = nameWidth + issue(10).getWidth(formatDuration(snapshot.duration, snapshot.infinite)) + ROW_RIGHT_MARGIN;
                if (rowWidth > targetWidth) targetWidth = rowWidth;
                targetHeight += 12 * animValue;
            }
        }

        if (visibleCount > 0) targetHeight += 2;
        widthAnimation.update(targetWidth);
        float width  = widthAnimation.getValue() + EXTRA_WIDTH;
        float height = targetHeight;

        RenderUtils.drawDefaultHudElementRects(eventRender.getContext().getMatrices(), x, y, width, height, colorTheme, isUnusualRectType());
        issue(14).draw(eventRender.getContext().getMatrices(), "Potions", x + 5, y + 6f, -1);
        myfont(15).drawString("e", x + width - 13.5f, y + 7f, colorTheme);

        float offsetY    = 18;
        int   effectIndex = 0;

        for (StatusEffect type : renderOrder) {
            float animValue  = getAnimation(type).getValue();
            PotionSnapshot snapshot = snapshots.get(type);
            if (animValue <= 0.01f || snapshot == null) { effectIndex++; continue; }

            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(x, y, width, height);

            int alpha = (int)(255 * animValue);

            boolean bad      = snapshot.entry != null && isBadEffect(snapshot.entry.value());
            boolean expiring = !snapshot.infinite && snapshot.duration > 0 && snapshot.duration <= EXPIRING_TICKS;

            int textColor;
            int iconColor;

            if (expiring) {
                float pulse = getPulse(PULSE_SPEED_EXPIRING, effectIndex * 1.1f);
                float minBrightness = 0.08f;
                float brightness    = minBrightness + (1.0f - minBrightness) * pulse;
                int r = 255;
                int g = (int)(20  * brightness);
                int b = (int)(20  * brightness);
                int a = (int)(MathHelper.lerp(0.55f + 0.45f * pulse, 0f, alpha));
                textColor = ColorUtils.rgba(r, g, b, a);
                iconColor = textColor;
            } else if (bad) {
                float pulse = getPulse(PULSE_SPEED_BAD, effectIndex * 1.1f);
                float base  = 0.55f;
                float t     = base + (1.0f - base) * pulse;
                int r = 255;
                int g = (int)(30  * t);
                int b = (int)(30  * t);
                textColor = ColorUtils.rgba(r, g, b, alpha);
                iconColor = textColor;
            } else {
                textColor = ColorUtils.rgba(255, 255, 255, alpha);
                iconColor = ColorUtils.rgba(255, 255, 255, alpha);
            }

            if (snapshot.entry != null) {
                if (bad || expiring) {
                    drawEffectIconTinted(eventRender, snapshot.entry, x + 3, y + offsetY - 1, 9, iconColor);
                } else {
                    drawEffectIcon(eventRender, snapshot.entry, x + 3, y + offsetY - 1, 9, alpha);
                }
            }

            String baseName    = snapshot.baseName != null ? snapshot.baseName : I18n.translate(type.getTranslationKey());
            String levelSuffix = getLevelSuffix(snapshot.amplifier);
            float  textX       = x + 5 + 7 + 1;
            float  textY       = y + 2 + offsetY;

            issue(12).draw(eventRender.getContext().getMatrices(), baseName, textX, textY, textColor);
            if (!levelSuffix.isEmpty()) {
                float baseWidth = issue(12).getStringWidth(baseName);
                issue(12).draw(eventRender.getContext().getMatrices(), " " + levelSuffix, textX + baseWidth, textY + 0.2f,
                        ColorUtils.rgba(255, 255, 255, alpha));
            }

            String time        = formatDuration(snapshot.duration, snapshot.infinite);
            float  timeBoxWidth = Math.max(issue(10).getStringWidth(time) + 4, 12f);
            float  timeBoxX    = x + width - timeBoxWidth - 5;

            issue(12).draw(eventRender.getContext().getMatrices(), time,
                    timeBoxX + timeBoxWidth / 2 + 0.3f - 6, textY, textColor);

            offsetY += 12 * animValue;
            effectIndex++;
            ScissorUtils.pop();
            ScissorUtils.unset();
        }

        animations.entrySet().removeIf(e -> !active.contains(e.getKey()) && e.getValue().getValue() <= 0.01f);
        snapshots.keySet().removeIf(type -> !animations.containsKey(type));
        maxDurations.keySet().removeIf(type -> !animations.containsKey(type));

        draggable.setWidth(width);
        draggable.setHeight(height);
    }
}