package dev.ethereal.client.ui.widget.overlay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleManager;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.client.ui.widget.Widget;

import java.awt.Color;
import java.util.*;

public class ArrayListWidget extends Widget {
    private final Map<String, Float> animMap = new HashMap<>();

    public ArrayListWidget() { super(2f, 30f); }

    @Override
    public String getName() { return "ArrayList"; }

    @Override
    public void render(MatrixStack ms) {
        float x = getDraggable().getX();
        float y = getDraggable().getY();
        float width = getDraggable().getWidth();
        boolean isRightSide = x + (width / 2f) > MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f;

        float fS = scaled(5.5f), p = scaled(2.5f);

        List<Module> enabled = ModuleManager.getInstance().getModules().stream()
                .filter(m -> m.isEnabled() && m.getCategory() != Category.RENDER)
                .sorted((m1, m2) -> Float.compare(
                        getMediumFont().getWidth(m2.getName(), fS),
                        getMediumFont().getWidth(m1.getName(), fS)))
                .toList();

        Set<String> enabledNames = new HashSet<>();
        enabled.forEach(m -> enabledNames.add(m.getName()));

        enabled.forEach(m -> {
            float cur = animMap.getOrDefault(m.getName(), 0f);
            animMap.put(m.getName(), cur + (1f - cur) * 0.25f);
        });
        animMap.forEach((k, v) -> {
            if (!enabledNames.contains(k)) animMap.put(k, v - v * 0.2f);
        });
        animMap.entrySet().removeIf(e -> e.getValue() < 0.01f);

        List<String> allKeys = new ArrayList<>(animMap.keySet());
        allKeys.sort((k1, k2) -> Float.compare(
                getMediumFont().getWidth(k2, fS),
                getMediumFont().getWidth(k1, fS)));

        float currentY = y;
        float maxW = 0;

        for (String name : allKeys) {
            float anim = animMap.getOrDefault(name, 0f);
            if (anim <= 0.01f) continue;

            float textW = getMediumFont().getWidth(name, fS);
            float rowH = (fS + p * 2f) * anim;
            float rectW = textW + p * 2f;
            float rectX = isRightSide ? (x + width - rectW) : x;

            int alpha = (int)(245 * anim);
            Color blockColor = new Color(8, 8, 8, (int)(200 * anim));

            RenderUtil.BLUR_RECT.draw(ms, rectX, currentY, rectW, rowH, 0f, blockColor);
            getMediumFont().drawGradientText(ms, name, rectX + p, currentY + rowH / 2f - fS / 2f, fS,
                    UIColors.primary(alpha), UIColors.secondary(alpha), textW);

            if (rectW > maxW) maxW = rectW;
            currentY += rowH;
        }

        getDraggable().setWidth(maxW);
        getDraggable().setHeight(enabled.isEmpty() ? scaled(10) : (currentY - y));
    }
}
