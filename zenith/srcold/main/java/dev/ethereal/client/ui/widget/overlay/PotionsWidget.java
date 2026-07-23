package dev.ethereal.client.ui.widget.overlay;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.other.TextUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.widget.ContainerWidget;
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
            if (!orderMap.containsKey(k)) orderMap.put(k, orderCounter++);
            animMap.put(k, animMap.getOrDefault(k, 0f) + (1f - animMap.getOrDefault(k, 0f)) * 0.25f);
        });

        List<String> toRemove = new ArrayList<>();
        animMap.forEach((k, v) -> {
            if (mc.player.getActiveStatusEffects().values().stream().noneMatch(ef -> ef.getTranslationKey().equals(k))) {
                float nv = v - v * 0.2f;
                if (nv < 0.01f) toRemove.add(k);
                else animMap.put(k, nv);
            }
        });
        toRemove.forEach(k -> { animMap.remove(k); orderMap.remove(k); });

        float headerH = scaled(12.7f), rowH0 = scaled(8.5f), p = scaled(2.5f), fS = scaled(5.5f), iconS = scaled(8f);
        float effectIconS = scaled(6f);
        float durBoxW = scaled(18f);

        String title = "Potions";
        String iconChar = "j";
        float titleW = getMediumFont().getWidth(title, fS);
        float iconW = Fonts.ICONS2.getWidth(iconChar, iconS);

        Map<String, Float> widthMap = new HashMap<>();
        float maxW = 0;
        for (StatusEffectInstance e : effects) {
            String name = Language.getInstance().get(e.getTranslationKey()) + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
            float w = p + effectIconS + p + getMediumFont().getWidth(name, fS) + p + scaled(1.5f) + durBoxW + p;
            widthMap.put(e.getTranslationKey(), w);
            if (w > maxW) maxW = w;
        }

        float headerMinW = titleW + iconW + p * 4;
        float headerW = Math.max(headerMinW, maxW);

        float x = getDraggable().getX(), y = getDraggable().getY(), dWidth = getDraggable().getWidth();
        boolean isRightSide = x + (dWidth / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;
        float headerX = isRightSide ? (x + dWidth - headerW) : x;
        float currentY = y;

        RenderUtil.BLUR_RECT.draw(ms, headerX, currentY, headerW, headerH, 2.5f, new Color(8, 8, 8, 200));
        getMediumFont().drawText(ms, title, headerX + p, currentY + headerH / 2f - fS / 2f, fS, Color.WHITE, 0f);
        Fonts.ICONS2.drawGradientText(ms, iconChar, headerX + headerW - p - iconW, currentY + headerH / 2f - iconS / 2f, iconS,
                UIColors.primary(), UIColors.secondary(), 1.1f);

        currentY += headerH + scaled(1f);

        List<String> allKeys = new ArrayList<>();
        effects.forEach(e -> { if (!allKeys.contains(e.getTranslationKey())) allKeys.add(e.getTranslationKey()); });
        animMap.keySet().forEach(k -> { if (!allKeys.contains(k)) allKeys.add(k); });
        allKeys.sort((k1, k2) -> Integer.compare(orderMap.getOrDefault(k1, 0), orderMap.getOrDefault(k2, 0)));

        for (String k : allKeys) {
            Float animValue = animMap.get(k);
            if (animValue == null || animValue <= 0.01f) continue;
            float anim = animValue;

            StatusEffectInstance e = effects.stream().filter(ef -> ef.getTranslationKey().equals(k)).findFirst().orElse(null);
            if (e == null) continue;

            float itemW = widthMap.getOrDefault(k, headerMinW);
            float xOffset = isRightSide ? (25f * (1f - anim)) : (-25f * (1f - anim));
            float itemX = (isRightSide ? (x + dWidth - itemW) : x) + xOffset;

            float rowH = rowH0 * anim + 2.5f;
            int alpha = (int)(245 * anim);
            float gap = scaled(1.5f);
            float nameBlockW = itemW - gap - durBoxW - p;
            Color blockColor = new Color(8, 8, 8, (int)(200 * anim));

            ms.push();

            // Блок имени + иконка
            RenderUtil.BLUR_RECT.draw(ms, itemX, currentY, nameBlockW, rowH, 2.5f, blockColor);

            String id = e.getEffectType().getKey().get().getValue().getPath();
            Identifier tex = Identifier.of("minecraft", "textures/mob_effect/" + id + ".png");
            RenderSystem.setShaderColor(1f, 1f, 1f, anim);
            RenderUtil.TEXTURE_RECT.draw(ms, itemX + p, currentY + rowH / 2f - effectIconS / 2f, effectIconS, effectIconS, 0f,
                    new Color(255, 255, 255, alpha), 0f, 0f, 1f, 1f, mc.getTextureManager().getTexture(tex).getGlId());
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            String name = Language.getInstance().get(k) + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : "");
            getMediumFont().drawText(ms, name, itemX + p + effectIconS + p, currentY + rowH / 2f - fS / 2f, fS, new Color(255, 255, 255, alpha), 0f);

            // Блок длительности
            float kBoxX = itemX + nameBlockW + gap;
            RenderUtil.BLUR_RECT.draw(ms, kBoxX, currentY, durBoxW, rowH, 2.5f, blockColor);

            String dur = TextUtil.getDurationText(e.getDuration());
            float dW = getMediumFont().getWidth(dur, fS);
            getMediumFont().drawText(ms, dur, kBoxX + durBoxW / 2f - dW / 2f, currentY + rowH / 2f - fS / 2f, fS, new Color(255, 255, 255, alpha), 0f);

            ms.pop();
            currentY += rowH + scaled(1f);
        }

        getDraggable().setWidth(headerW);
        getDraggable().setHeight(currentY - y);
    }
}
