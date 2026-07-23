package sweetie.evaware.client.ui.widget.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.other.TextUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.client.ui.widget.ContainerWidget;
import java.awt.Color;
import java.util.*;

public class PotionsWidget extends ContainerWidget {
    private final Map<String, Float> animMap = new HashMap<>();
    private final Map<String, Integer> orderMap = new HashMap<>();
    private int orderCounter = 0;

    public PotionsWidget() { super(3f, 120f); }
    @Override public String getName() { return "Potions"; }
    @Override protected Map<String, ContainerElement.ColoredString> getCurrentData() { return null; }

    @Override
    public void render(MatrixStack ms) {
        if (mc.player == null) return;

        List<StatusEffectInstance> effects = new ArrayList<>(mc.player.getActiveStatusEffects().values());

        effects.forEach(e -> {
            String k = e.getTranslationKey();
            if (!orderMap.containsKey(k)) {
                orderMap.put(k, orderCounter++);
            }
            animMap.put(k, animMap.getOrDefault(k, 0f) + (1f - animMap.getOrDefault(k, 0f)) * 0.25f);
        });

        List<String> toRemove = new ArrayList<>();
        animMap.forEach((k, v) -> {
            if (mc.player.getActiveStatusEffects().values().stream().noneMatch(ef -> ef.getTranslationKey().equals(k))) {
                float newVal = v - v * 0.2f;
                if (newVal < 0.01f) {
                    toRemove.add(k);
                } else {
                    animMap.put(k, newVal);
                }
            }
        });
        toRemove.forEach(k -> {
            animMap.remove(k);
            orderMap.remove(k);
        });

        float h = scaled(11), p = scaled(3.5f), fS = scaled(6f), iconS = scaled(8f), effectIconS = scaled(7f);
        float arrowW = getMediumFont().getWidth(">", fS);

        Map<String, Float> widthMap = new HashMap<>();
        float maxEffectW = 0;

        for (StatusEffectInstance e : effects) {
            String name = Language.getInstance().get(e.getTranslationKey()) + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
            String dur = TextUtil.getDurationText(e.getDuration());
            float w = p + effectIconS + (p * 0.7f) + arrowW + (p * 0.7f) + getMediumFont().getWidth(name, fS) + p * 3 + getMediumFont().getWidth(dur, fS) + p;
            widthMap.put(e.getTranslationKey(), w);
            if (w > maxEffectW) maxEffectW = w;
        }

        String title = "Potions";
        String iconChar = "F";
        float titleW = getMediumFont().getWidth(title, fS);
        float iconW = Fonts.ICONS.getWidth(iconChar, iconS);
        float headerMinW = titleW + iconW + p * 4;

        float headerW = Math.max(headerMinW, maxEffectW);

        effects.sort((e1, e2) -> {
            int order1 = orderMap.getOrDefault(e1.getTranslationKey(), 0);
            int order2 = orderMap.getOrDefault(e2.getTranslationKey(), 0);
            return Integer.compare(order1, order2);
        });

        float x = getDraggable().getX(), y = getDraggable().getY();
        float dWidth = getDraggable().getWidth();
        boolean isRightSide = x + (dWidth / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        Color bg = new Color(16, 16, 24, 240);
        Color graySeparator = new Color(160, 160, 160, 180);

        float headerX = isRightSide ? (x + dWidth - headerW) : x;
        RenderUtil.RECT.draw(ms, headerX, y, headerW, h, 3f, bg);
        getMediumFont().drawText(ms, title, headerX + p, y + h/2f - fS/2f, fS, Color.WHITE, 0f);
        Fonts.ICONS.drawGradientText(ms, iconChar, headerX + headerW - p - iconW, y + h/2f - iconS/2f, iconS,
                UIColors.primary(), UIColors.secondary(), 1.1f);

        float currentY = y + h + 1.5f;

        for (StatusEffectInstance e : effects) {
            String k = e.getTranslationKey();
            float anim = animMap.getOrDefault(k, 0f);
            if (anim <= 0.05f) continue;

            float itemW = widthMap.getOrDefault(k, headerMinW);
            float itemX = isRightSide ? (x + dWidth - itemW) : x;

            float rowH = h * anim;
            int alpha = (int)(255 * anim);
            String id = e.getEffectType().getKey().get().getValue().getPath();

            Color dynamicBg = new Color(16, 16, 24, (int)(245 * anim));
            Color dynamicText = new Color(180, 180, 180, alpha);
            Color dynamicArrow = new Color(120, 120, 120, alpha);
            Color dynamicKeyBg = new Color(50, 50, 50, (int)(220 * anim));

            RenderUtil.BLUR_RECT.draw(ms, itemX, currentY, itemW, rowH, 3f, dynamicBg);

            float tY = currentY + (rowH / 2f) - fS/2f;

            Identifier tex = Identifier.of("minecraft", "textures/mob_effect/" + id + ".png");
            RenderSystem.setShaderColor(1f, 1f, 1f, anim);
            RenderUtil.TEXTURE_RECT.draw(ms, itemX + p, currentY + rowH/2f - effectIconS/2f, effectIconS, effectIconS, 0f, new Color(255, 255, 255, alpha), 0f, 0f, 1f, 1f, mc.getTextureManager().getTexture(tex).getGlId());
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            getMediumFont().drawText(ms, ">", itemX + p + effectIconS + (p * 0.5f), tY, fS, dynamicArrow);

            String name = Language.getInstance().get(k) + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
            getMediumFont().drawText(ms, name, itemX + p + effectIconS + p + arrowW, tY, fS, dynamicText, 0f);

            String dur = TextUtil.getDurationText(e.getDuration());
            float dW = getMediumFont().getWidth(dur, fS);
            float kRectW = dW + scaled(4);
            float kRectH = (fS + scaled(2)) * anim;
            float kRectX = itemX + itemW - p - kRectW;
            float kRectY = currentY + (rowH / 2f) - (kRectH / 2f);

            if (anim > 0.1f) {
                RenderUtil.RECT.draw(ms, kRectX, kRectY, kRectW, kRectH, 2f, dynamicKeyBg);
                getMediumFont().drawText(ms, dur, kRectX + (kRectW / 2f) - (dW / 2f), tY, fS, Color.WHITE, 0f);
            }

            currentY += rowH + 1.5f;
        }

        getDraggable().setWidth(headerW);
        getDraggable().setHeight(currentY - y);
    }
}
