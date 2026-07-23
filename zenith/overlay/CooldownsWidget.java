package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.other.TextUtil;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Fonts;
import sweetie.evaware.client.ui.widget.ContainerWidget;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CooldownsWidget extends ContainerWidget {
    private final Map<String, Float> animMap = new HashMap<>();
    private final Map<String, Integer> orderMap = new HashMap<>();
    private int orderCounter = 0;

    public CooldownsWidget() {
        super(100f, 100f);
    }

    @Override public String getName() { return "Cooldowns"; }

    @Override
    public void render(MatrixStack ms) {
        if (mc.player == null) return;

        Map<String, ContainerElement.ColoredString> data = getCurrentCooldowns();

        data.keySet().forEach(k -> {
            if (!orderMap.containsKey(k)) {
                orderMap.put(k, orderCounter++);
            }
            animMap.put(k, animMap.getOrDefault(k, 0f) + (1f - animMap.getOrDefault(k, 0f)) * 0.25f);
        });
        
        List<String> toRemove = new ArrayList<>();
        animMap.forEach((k, v) -> {
            if (!data.containsKey(k)) {
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

        float h = scaled(11), p = scaled(3.5f), fS = scaled(6f), iconS = scaled(8f);

        Map<String, Float> widthMap = new HashMap<>();
        float maxCDW = 0;
        for (Map.Entry<String, ContainerElement.ColoredString> entry : data.entrySet()) {
            float nW = getMediumFont().getWidth(entry.getKey(), fS);
            float tW = getMediumFont().getWidth(entry.getValue().text(), fS);
            float w = p + nW + p * 4 + (tW + scaled(4)) + p;
            widthMap.put(entry.getKey(), w);
            if (w > maxCDW) maxCDW = w;
        }

        String title = "Cooldowns";
        String iconChar = "R";
        float titleW = getMediumFont().getWidth(title, fS);
        float iconW = Fonts.ICONS.getWidth(iconChar, iconS);
        float headerW = Math.max(titleW + iconW + p * 5, maxCDW);

        List<String> sortedKeys = new ArrayList<>(data.keySet());
        sortedKeys.sort((a, b) -> {
            int order1 = orderMap.getOrDefault(a, 0);
            int order2 = orderMap.getOrDefault(b, 0);
            return Integer.compare(order1, order2);
        });

        float x = getDraggable().getX(), y = getDraggable().getY();
        float dWidth = getDraggable().getWidth();
        boolean isRightSide = x + (dWidth / 2f) > mc.getWindow().getScaledWidth() / 2f;
        float headerX = isRightSide ? (x + dWidth - headerW) : x;

        RenderUtil.RECT.draw(ms, headerX, y, headerW, h, 3f, new Color(16, 16, 24, 240));
        getMediumFont().drawText(ms, title, headerX + p, y + h/2f - fS/2f, fS, Color.WHITE, 0f);
        Fonts.ICONS.drawGradientText(ms, iconChar, headerX + headerW - p - iconW -1, y + h/2f - iconS/2f, iconS,
                UIColors.primary(), UIColors.secondary(), 1.1f);

        float currentY = y + h + 1.5f;

        for (String key : sortedKeys) {
            float anim = animMap.getOrDefault(key, 0f);
            if (anim <= 0.05f) continue;

            float itemW = widthMap.getOrDefault(key, headerW);
            float itemX = isRightSide ? (x + dWidth - itemW) : x;
            float rowH = h * anim;
            int alpha = (int)(255 * anim);

            RenderUtil.BLUR_RECT.draw(ms, itemX, currentY, itemW, rowH, 3f, new Color(16, 16, 24, (int)(245 * anim)));

            float tY = currentY + (rowH / 2f) - fS/2f;

            getMediumFont().drawText(ms, key, itemX + p, tY, fS, new Color(180, 180, 180, alpha), 0f);

            String time = data.get(key).text();
            float tW = getMediumFont().getWidth(time, fS);
            float kRectW = tW + scaled(4);
            float kRectH = (fS + scaled(2)) * anim;
            float kRectX = itemX + itemW - p - kRectW;
            float kRectY = currentY + (rowH / 2f) - (kRectH / 2f);

            if (anim > 0.1f) {
                RenderUtil.RECT.draw(ms, kRectX, kRectY, kRectW, kRectH, 2f, new Color(50, 50, 50, (int)(220 * anim)));
                getMediumFont().drawText(ms, time, kRectX + (kRectW / 2f) - (tW / 2f), tY, fS, Color.WHITE, 0f);
            }

            currentY += rowH + 1.5f;
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
                        String name = stack.getItem().getName().getString();
                        String time = TextUtil.getDurationText(remaining);
                        cooldownData.put(name, new ContainerElement.ColoredString(time));
                    }
                }
            }
        }
        return cooldownData;
    }

    @Override protected Map<String, ContainerElement.ColoredString> getCurrentData() { return null; }
}
