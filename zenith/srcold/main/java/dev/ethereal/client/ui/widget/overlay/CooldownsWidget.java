package dev.ethereal.client.ui.widget.overlay;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.other.TextUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.widget.ContainerWidget;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CooldownsWidget extends ContainerWidget {
    private final Map<String, Float> animMap = new HashMap<>();
    private final Map<String, Integer> orderMap = new HashMap<>();
    private int orderCounter = 0;

    public CooldownsWidget() { super(100f, 100f); }
    @Override public String getName() { return "Cooldowns"; }
    @Override protected Map<String, ContainerElement.ColoredString> getCurrentData() { return null; }

    @Override
    public void render(MatrixStack ms) {
        if (mc.player == null) return;

        Map<String, ContainerElement.ColoredString> data = getCurrentCooldowns();

        data.keySet().forEach(k -> {
            if (!orderMap.containsKey(k)) orderMap.put(k, orderCounter++);
            animMap.put(k, animMap.getOrDefault(k, 0f) + (1f - animMap.getOrDefault(k, 0f)) * 0.25f);
        });

        List<String> toRemove = new ArrayList<>();
        animMap.forEach((k, v) -> {
            if (!data.containsKey(k)) {
                float nv = v - v * 0.2f;
                if (nv < 0.01f) toRemove.add(k);
                else animMap.put(k, nv);
            }
        });
        toRemove.forEach(k -> { animMap.remove(k); orderMap.remove(k); });

        float headerH = scaled(12.7f), rowH0 = scaled(8.5f), p = scaled(2.5f), fS = scaled(5.5f), iconS = scaled(8f);
        float timeBoxW = scaled(20f);

        Map<String, Float> widthMap = new HashMap<>();
        float maxW = 0;
        for (Map.Entry<String, ContainerElement.ColoredString> entry : data.entrySet()) {
            float w = p + getMediumFont().getWidth(entry.getKey(), fS) + p + scaled(1.5f) + timeBoxW + p;
            widthMap.put(entry.getKey(), w);
            if (w > maxW) maxW = w;
        }

        String title = "Cooldowns";
        String iconChar = "i";
        float titleW = getMediumFont().getWidth(title, fS);
        float iconW = Fonts.ICONS2.getWidth(iconChar, iconS);
        float headerMinW = titleW + iconW + p * 4;
        float headerW = Math.max(headerMinW, maxW);

        float x = getDraggable().getX(), y = getDraggable().getY(), dWidth = getDraggable().getWidth();
        boolean isRightSide = x + (dWidth / 2f) > mc.getWindow().getScaledWidth() / 2f;
        float headerX = isRightSide ? (x + dWidth - headerW) : x;
        float currentY = y;

        RenderUtil.BLUR_RECT.draw(ms, headerX, currentY, headerW, headerH, 2.5f, new Color(8, 8, 8, 200));
        getMediumFont().drawText(ms, title, headerX + p, currentY + headerH / 2f - fS / 2f, fS, Color.WHITE, 0f);
        Fonts.ICONS2.drawGradientText(ms, iconChar, headerX + headerW - p - iconW - 1, currentY + headerH / 2f - iconS / 2f, iconS,
                UIColors.primary(), UIColors.secondary(), 1.1f);

        currentY += headerH + scaled(1f);

        List<String> sortedKeys = new ArrayList<>(data.keySet());
        sortedKeys.sort((a, b) -> Integer.compare(orderMap.getOrDefault(a, 0), orderMap.getOrDefault(b, 0)));

        for (String key : sortedKeys) {
            float anim = animMap.getOrDefault(key, 0f);
            if (anim <= 0.05f) continue;

            float itemW = widthMap.getOrDefault(key, headerW);
            float itemX = isRightSide ? (x + dWidth - itemW) : x;
            float rowH = rowH0 * anim + 2.5f;
            int alpha = (int)(245 * anim);
            float gap = scaled(1.5f);
            float nameBlockW = itemW - gap - timeBoxW - p;
            Color blockColor = new Color(8, 8, 8, (int)(200 * anim));

            ms.push();

            RenderUtil.BLUR_RECT.draw(ms, itemX, currentY, nameBlockW, rowH, 2.5f, blockColor);
            getMediumFont().drawText(ms, key, itemX + p, currentY + rowH / 2f - fS / 2f, fS, new Color(255, 255, 255, alpha), 0f);

            float kBoxX = itemX + nameBlockW + gap;
            RenderUtil.BLUR_RECT.draw(ms, kBoxX, currentY, timeBoxW, rowH, 2.5f, blockColor);

            String time = data.get(key).text();
            float tW = getMediumFont().getWidth(time, fS);
            getMediumFont().drawText(ms, time, kBoxX + timeBoxW / 2f - tW / 2f, currentY + rowH / 2f - fS / 2f, fS, new Color(255, 255, 255, alpha), 0f);

            ms.pop();
            currentY += rowH + scaled(1f);
        }

        getDraggable().setWidth(headerW);
        getDraggable().setHeight(currentY - y);
    }

    private Map<String, ContainerElement.ColoredString> getCurrentCooldowns() {
        Map<String, ContainerElement.ColoredString> cooldownData = new HashMap<>();
        if (mc.player == null) return cooldownData;

        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        float delta = mc.getRenderTickCounter().getTickDelta(false);

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (manager.isCoolingDown(stack)) {
                Identifier groupId = manager.getGroup(stack);
                ItemCooldownManager.Entry entry = manager.entries.get(groupId);
                if (entry != null) {
                    int remaining = Math.max(0, entry.endTick() - (manager.tick + (int) delta));
                    if (remaining > 0) {
                        cooldownData.put(stack.getItem().getName().getString(),
                                new ContainerElement.ColoredString(TextUtil.getDurationText(remaining)));
                    }
                }
            }
        }
        return cooldownData;
    }
}
