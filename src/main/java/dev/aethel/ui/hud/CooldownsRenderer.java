package dev.aethel.ui.hud;

import dev.aethel.mixin.ItemCooldownManagerAccessor;
import dev.aethel.module.list.render.Interface;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CooldownsRenderer implements IMinecraft {
    private final Interface interfaceModule;
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);
    private final Animation widthAnim = new Animation(Easing.EXPO_OUT, 300);

    private static final float PADDING_X = 7f;
    private static final float HEADER_HEIGHT = 14f;
    private static final float ROW_HEIGHT = 16f;
    private static final float ICON_SIZE = 10f;
    private static final float ICON_GAP = 4f;
    private static final float MIN_WIDTH = 70f;
    private static final float ROUNDING = 4f;
    private static final float ROW_TEXT_SIZE = 6.5f;
    private static final float TITLE_TEXT_SIZE = 7f;

    public CooldownsRenderer(Interface interfaceModule) {
        this.interfaceModule = interfaceModule;
    }

    public void render(DrawContext context) {
        if (mc.player == null || mc.world == null) return;
        renderCooldowns(context);
    }

    @SuppressWarnings("unchecked")
    private void renderCooldowns(DrawContext context) {
        if (mc.player == null || mc.world == null) return;

        List<CooldownEntry> entries = collectCooldowns();

        final boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        alpha.run((entries.isEmpty() && !chatOpen) ? 0f : 1f);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) {
            interfaceModule.getCooldownsDrag().setWidth(0);
            interfaceModule.getCooldownsDrag().setHeight(0);
            return;
        }

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);

        float x = interfaceModule.getCooldownsDrag().getX();
        float y = interfaceModule.getCooldownsDrag().getY();

        float targetWidth = MIN_WIDTH;
        for (CooldownEntry entry : entries) {
            String time = formatSeconds(entry.seconds);
            float nameW = Fonts.SFBOLD.get().getWidth(entry.name, ROW_TEXT_SIZE);
            float timeW = Fonts.SFBOLD.get().getWidth(time, ROW_TEXT_SIZE);
            float rowW = PADDING_X + ICON_SIZE + ICON_GAP + nameW + timeW + PADDING_X * 2f;
            targetWidth = Math.max(targetWidth, rowW);
        }
        float titleW = Fonts.SFBOLD.get().getWidth("Cooldowns", TITLE_TEXT_SIZE) + PADDING_X * 2f;
        targetWidth = Math.max(targetWidth, titleW);

        widthAnim.run(targetWidth);
        float curW = Math.max(MIN_WIDTH, (float) widthAnim.getValue());

        float totalH = HEADER_HEIGHT + (entries.isEmpty() ? 0f : entries.size() * ROW_HEIGHT + 2f);

        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();

        Matrix4f m2 = context.getMatrices().peek().getPositionMatrix();

        interfaceModule.drawGlow(m2, x, y, curW, totalH, ROUNDING, globalAlpha);

        int[] glow = ColorProvider.getOrbitalRect(t1, t2, 800.0, aInt);
        int tmp = glow[1]; glow[1] = glow[3]; glow[3] = tmp;
        DrawUtil.drawRound(x - 0.5f, y - 0.5f, curW + 1f, totalH + 1f, ROUNDING, glow[0], glow[1], glow[2], glow[3]);

        int bgColor = ColorProvider.rgba(
                ((t1 >> 16) & 0xFF) >> 2,
                ((t1 >> 8) & 0xFF) >> 2,
                (t1 & 0xFF) >> 2,
                Math.min(135, aInt)
        );
        DrawUtil.drawRound(x, y, curW, totalH, ROUNDING, bgColor);

        float titleTextX = x + (curW - Fonts.SFBOLD.get().getWidth("Cooldowns", TITLE_TEXT_SIZE)) / 2f;
        DrawUtil.drawText(Fonts.SFBOLD.get(), "Cooldowns", titleTextX, y + 2.5f, ColorProvider.rgba(255, 255, 255, aInt), TITLE_TEXT_SIZE);

        int sepColor = ColorProvider.setAlpha(t1, aInt / 3);
        DrawUtil.drawRound(x + 3f, y + HEADER_HEIGHT, curW - 6f, 0.5f, 0.25f, sepColor);

        float curY = y + HEADER_HEIGHT + 1f;

        for (CooldownEntry entry : entries) {
            float iconX = x + PADDING_X;
            float iconY = curY + (ROW_HEIGHT - ICON_SIZE) / 2f;

            context.getMatrices().push();
            context.getMatrices().translate(iconX, iconY, 50);
            float scale = ICON_SIZE / 16f;
            context.getMatrices().scale(scale, scale, 1f);
            context.drawItem(entry.stack, 0, 0);
            context.drawStackOverlay(mc.textRenderer, entry.stack, 0, 0);
            context.getMatrices().pop();

            float textX = iconX + ICON_SIZE + ICON_GAP;
            float textY = curY + (ROW_HEIGHT - ROW_TEXT_SIZE) / 2f;
            DrawUtil.drawText(Fonts.SFBOLD.get(), entry.name, textX, textY, ColorProvider.rgba(255, 255, 255, aInt), ROW_TEXT_SIZE);

            String time = formatSeconds(entry.seconds);
            float timeW = Fonts.SFBOLD.get().getWidth(time, ROW_TEXT_SIZE);
            float timeX = x + curW - PADDING_X - timeW;
            DrawUtil.drawText(Fonts.SFBOLD.get(), time, timeX, textY, ColorProvider.rgba(255, 255, 255, (int) (aInt * 0.7f)), ROW_TEXT_SIZE);

            curY += ROW_HEIGHT;
        }

        interfaceModule.getCooldownsDrag().setWidth(curW);
        interfaceModule.getCooldownsDrag().setHeight(totalH);
    }

    @SuppressWarnings("unchecked")
    private List<CooldownEntry> collectCooldowns() {
        if (mc.player == null) return List.of();
        ItemCooldownManager cm = mc.player.getItemCooldownManager();
        ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) cm;
        Map<Identifier, ?> rawEntries = (Map<Identifier, ?>) accessor.getEntries();
        int tick = accessor.getTick();

        Map<Identifier, CooldownEntry> map = new LinkedHashMap<>();

        for (int i = 0; i < 36; i++) {
            addEntry(map, mc.player.getInventory().getStack(i), rawEntries, tick);
        }
        addEntry(map, mc.player.getOffHandStack(), rawEntries, tick);

        return map.values().stream()
                .sorted(Comparator.comparingDouble(e -> -e.seconds))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private void addEntry(Map<Identifier, CooldownEntry> map, ItemStack stack,
                          Map<Identifier, ?> rawEntries, int currentTick) {
        if (stack == null || stack.isEmpty()) return;
        ItemCooldownManager cm = mc.player.getItemCooldownManager();
        if (!cm.isCoolingDown(stack)) return;
        Identifier group = cm.getGroup(stack);
        Object rawEntry = rawEntries.get(group);
        if (rawEntry == null) return;
        try {
            var entryField = rawEntry.getClass().getField("endTick");
            int endTick = entryField.getInt(rawEntry);
            float seconds = Math.max(0f, (endTick - currentTick) / 20f);
            if (seconds > 0f) {
                map.putIfAbsent(group, new CooldownEntry(stack.copy(), stack.getName().getString(), seconds));
            }
        } catch (Exception ignored) {}
    }

    private static String formatSeconds(float seconds) {
        return String.format("%.1fs", Math.max(0f, seconds));
    }

    private record CooldownEntry(ItemStack stack, String name, float seconds) {}
}
