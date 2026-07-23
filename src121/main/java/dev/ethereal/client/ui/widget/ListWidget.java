package dev.ethereal.client.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;

import java.awt.Color;
import java.util.*;
import java.util.List;

public abstract class ListWidget extends Widget {
    protected ListWidget(float x, float y) { super(x, y); }

    private final Map<String, Float> anim = new HashMap<>();
    private final Map<String, Integer> order = new HashMap<>();
    private final Map<String, Row> cache = new HashMap<>();
    private int orderCounter = 0;

    protected abstract String getIcon();

    protected abstract List<Row> collectRows();

    @Getter
    @RequiredArgsConstructor
    protected static class Row {
        private final String key;
        private final String label;
        private final String value;
        private final Color valueColor;
        private final Identifier icon;

        public Row(String key, String label, String value, Color valueColor) {
            this(key, label, value, valueColor, null);
        }
    }

    @Override
    public void render(MatrixStack ms) {
        List<Row> rows = collectRows();

        Set<String> present = new HashSet<>();
        for (Row r : rows) {
            present.add(r.getKey());
            order.putIfAbsent(r.getKey(), orderCounter++);
            cache.put(r.getKey(), r);
            float cur = anim.getOrDefault(r.getKey(), 0f);
            anim.put(r.getKey(), cur + (1f - cur) * 0.25f);
        }

        Iterator<Map.Entry<String, Float>> it = anim.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Float> e = it.next();
            if (!present.contains(e.getKey())) {
                float nv = e.getValue() * 0.8f;
                if (nv < 0.01f) {
                    order.remove(e.getKey());
                    cache.remove(e.getKey());
                    it.remove();
                } else {
                    e.setValue(nv);
                }
            }
        }

        float p = scaled(3f);
        float gap = scaled(4f);
        float iconGap = scaled(2f);
        float fS = scaled(5.5f);
        float headerFS = scaled(5.5f);
        float headerIconS = scaled(8f);
        float leftIconS = scaled(6f);
        float headerH = scaled(12f);
        float rowH = scaled(9.5f);
        float round = scaled(2.5f);

        String title = getName();
        String iconChar = getIcon();
        float titleW = getMediumFont().getWidth(title, headerFS);
        float headerIconW = Fonts.ICONS2.getWidth(iconChar, headerIconS);

        List<String> keys = new ArrayList<>(anim.keySet());
        keys.sort(Comparator.comparingInt(k -> order.getOrDefault(k, 0)));

        float valueColW = headerIconW;
        float leftMax = titleW;
        for (String k : keys) {
            Row r = cache.get(k);
            if (r == null) continue;
            float leftIconW = r.getIcon() != null ? leftIconS + iconGap : 0f;
            float labelPartW = leftIconW + getMediumFont().getWidth(r.getLabel(), fS);
            if (labelPartW > leftMax) leftMax = labelPartW;
            if (!r.getValue().isEmpty()) {
                float vW = getMediumFont().getWidth(r.getValue(), fS);
                if (vW > valueColW) valueColW = vW;
            }
        }
        float panelW = p + leftMax + gap + valueColW + p;

        float x = getDraggable().getX(), y = getDraggable().getY(), dWidth = getDraggable().getWidth();
        boolean isRightSide = x + (dWidth / 2f) > mc.getWindow().getScaledWidth() / 2f;
        float panelX = isRightSide ? (x + dWidth - panelW) : x;

        float bodyH = 0f;
        for (String k : keys) {
            if (cache.get(k) == null) continue;
            float a = anim.getOrDefault(k, 0f);
            if (a <= 0.01f) continue;
            bodyH += rowH * a;
        }
        float totalH = headerH + bodyH;

        float colCenterX = panelX + panelW - p - valueColW / 2f;

        RenderUtil.BLUR_RECT.draw(ms, panelX, y, panelW, totalH, round, new Color(8, 8, 8, 200));

        getMediumFont().drawText(ms, title, panelX + p, y + headerH / 2f - headerFS / 2f, headerFS, Color.WHITE, 0f);
        Fonts.ICONS2.drawGradientText(ms, iconChar, colCenterX - headerIconW / 2f,
                y + headerH / 2f - headerIconS / 2f, headerIconS, UIColors.primary(), UIColors.secondary(), 1.1f);

        if (bodyH > 0.5f) {
            RenderUtil.drawRect(ms, panelX + p, y + headerH, panelW - p * 2f, scaled(0.5f), new Color(255, 255, 255, 22));
        }

        float currentY = y + headerH;
        for (String k : keys) {
            Row r = cache.get(k);
            if (r == null) continue;
            float a = anim.getOrDefault(k, 0f);
            if (a <= 0.01f) continue;

            float rh = rowH * a;
            int alpha = (int) (255 * a);
            float textY = currentY + rh / 2f - fS / 2f;

            float labelX = panelX + p;
            if (r.getIcon() != null) {
                RenderSystem.setShaderColor(1f, 1f, 1f, a);
                RenderUtil.TEXTURE_RECT.draw(ms, labelX, currentY + rh / 2f - leftIconS / 2f, leftIconS, leftIconS, 0f,
                        new Color(255, 255, 255, alpha), 0f, 0f, 1f, 1f, mc.getTextureManager().getTexture(r.getIcon()).getGlId());
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                labelX += leftIconS + iconGap;
            }

            getMediumFont().drawText(ms, r.getLabel(), labelX, textY, fS, new Color(255, 255, 255, alpha), 0f);

            if (!r.getValue().isEmpty()) {
                float valueW = getMediumFont().getWidth(r.getValue(), fS);
                getMediumFont().drawText(ms, r.getValue(), colCenterX - valueW / 2f, textY, fS,
                        ColorUtil.setAlpha(r.getValueColor(), alpha), 0f);
            }

            currentY += rh;
        }

        getDraggable().setWidth(panelW);
        getDraggable().setHeight(totalH);
    }
}
