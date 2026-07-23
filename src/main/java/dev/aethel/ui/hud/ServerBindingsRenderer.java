package dev.aethel.ui.hud;

import dev.aethel.module.list.player.Assistant;
import dev.aethel.module.list.render.Interface;
import dev.aethel.module.settings.BindSetting;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.base.Instance;
import dev.aethel.util.keyboard.KeyStorage;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServerBindingsRenderer implements IMinecraft {
    private final Interface interfaceModule;
    private final Animation alpha = new Animation(Easing.EXPO_OUT, 200);

    private static final float ITEM_SPACING = 3.0f;
    private static final float MAIN_BLOCK_HEIGHT = 26f;
    private static final float BIND_BLOCK_HEIGHT = 14f;
    private static final float TOTAL_BLOCK_HEIGHT = MAIN_BLOCK_HEIGHT + BIND_BLOCK_HEIGHT;
    private static final float MIN_WIDTH = 28f;
    private static final float ICON_SIZE = 14f;
    private static final float BLOCK_SHRINK = 2f;
    private static final float ROUNDING = 4f;
    private static final float BIND_ROUNDING = 3f;
    private static final float BIND_TEXT_SIZE = 7f;

    public ServerBindingsRenderer(Interface interfaceModule) {
        this.interfaceModule = interfaceModule;
    }

    public void render(DrawContext context) {
        if (mc.player == null || mc.world == null) return;
        renderBindings(context);
    }

    private void renderBindings(DrawContext context) {
        if (mc.player == null || mc.world == null) return;

        Assistant assistant = Instance.get(Assistant.class);
        if (assistant == null || !assistant.isEnabled()) return;

        List<BindingEntry> entries = collectEntries(assistant);
        if (entries.isEmpty()) {
            interfaceModule.getServerBindingsDrag().setWidth(0);
            interfaceModule.getServerBindingsDrag().setHeight(0);
            return;
        }

        final boolean chatOpen = mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
        alpha.run(chatOpen ? 0.9f : 1f);

        float globalAlpha = (float) alpha.getValue();
        if (globalAlpha <= 0.05f) return;

        int aInt = MathHelper.clamp((int) (255f * globalAlpha), 0, 255);

        float x = interfaceModule.getServerBindingsDrag().getX();
        float y = interfaceModule.getServerBindingsDrag().getY();

        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();

        float maxBlockWidth = MIN_WIDTH;
        for (BindingEntry entry : entries) {
            String keyName = getKeyDisplayName(entry.setting.getValue());
            float keyNameWidth = Fonts.SFBOLD.get().getWidth(keyName, BIND_TEXT_SIZE) + 6f;
            maxBlockWidth = Math.max(maxBlockWidth, keyNameWidth + 8f);
        }

        float totalWidth = maxBlockWidth * entries.size() + (entries.size() - 1) * ITEM_SPACING;

        float currentX = x;
        Matrix4f m2 = context.getMatrices().peek().getPositionMatrix();

        for (BindingEntry entry : entries) {
            String keyName = getKeyDisplayName(entry.setting.getValue());
            float blockWidth = maxBlockWidth;
            float keyNameWidth = Fonts.SFBOLD.get().getWidth(keyName, BIND_TEXT_SIZE) + 6f;

            float bx = currentX + BLOCK_SHRINK * 0.5f;
            float by = y;
            float bw = blockWidth - BLOCK_SHRINK;
            float bh = MAIN_BLOCK_HEIGHT;

            interfaceModule.drawGlow(m2, bx, by, bw, bh, ROUNDING, globalAlpha);

            int[] glow = ColorProvider.getOrbitalRect(t1, t2, 800.0, aInt);
            int tmp = glow[1]; glow[1] = glow[3]; glow[3] = tmp;
            DrawUtil.drawRound(bx - 0.5f, by - 0.5f, bw + 1f, bh + 1f, ROUNDING, glow[0], glow[1], glow[2], glow[3]);

            int bgColor = ColorProvider.rgba(
                    ((t1 >> 16) & 0xFF) >> 2,
                    ((t1 >> 8) & 0xFF) >> 2,
                    (t1 & 0xFF) >> 2,
                    Math.min(135, aInt)
            );
            DrawUtil.drawRound(bx, by, bw, bh, ROUNDING, bgColor);

            float iconX = currentX + (blockWidth - ICON_SIZE) / 2f;
            float iconY = by + (bh - ICON_SIZE) / 2f;

            context.getMatrices().push();
            context.getMatrices().translate(iconX, iconY, 50);
            float scale = ICON_SIZE / 16f;
            context.getMatrices().scale(scale, scale, 1f);
            context.drawItem(entry.stack, 0, 0);
            context.drawStackOverlay(mc.textRenderer, entry.stack, 0, 0);
            context.getMatrices().pop();

            if (mc.player != null) {
                ItemCooldownManager cooldownManager = mc.player.getItemCooldownManager();
                float progress = cooldownManager.getCooldownProgress(entry.stack, 0);
                if (progress > 0) {
                    float cw = bw * (1.0f - progress);
                    float cx = bx + (bw * progress);
                    DrawUtil.drawRound(cx, by, cw, bh, ROUNDING, ColorProvider.rgba(80, 80, 80, (int) (140 * globalAlpha)));
                }
            }

            float pillW = Math.min(bw - 2f, keyNameWidth + 4f);
            float pillH = BIND_BLOCK_HEIGHT;
            float pillX = currentX + (blockWidth - pillW) / 2f;
            float pillY = by + MAIN_BLOCK_HEIGHT + 1f;

            int[] pillGlow = ColorProvider.getOrbitalRect(t1, t2, 800.0, aInt);
            DrawUtil.drawRound(pillX - 0.5f, pillY - 0.5f, pillW + 1f, pillH + 1f, BIND_ROUNDING,
                    pillGlow[0], pillGlow[1], pillGlow[2], pillGlow[3]);

            int pillBg = ColorProvider.rgba(
                    ((t1 >> 16) & 0xFF) >> 2,
                    ((t1 >> 8) & 0xFF) >> 2,
                    (t1 & 0xFF) >> 2,
                    Math.min(135, aInt)
            );
            DrawUtil.drawRound(pillX, pillY, pillW, pillH, BIND_ROUNDING, pillBg);

            float textX = pillX + (pillW - Fonts.SFBOLD.get().getWidth(keyName, BIND_TEXT_SIZE)) / 2f;
            float textY = pillY + (pillH - BIND_TEXT_SIZE) / 2f + 1f;
            DrawUtil.drawText(Fonts.SFBOLD.get(), keyName, textX, textY, ColorProvider.rgba(255, 255, 255, aInt), BIND_TEXT_SIZE);

            currentX += blockWidth + ITEM_SPACING;
        }

        interfaceModule.getServerBindingsDrag().setWidth(totalWidth);
        interfaceModule.getServerBindingsDrag().setHeight(TOTAL_BLOCK_HEIGHT);
    }

    private List<BindingEntry> collectEntries(Assistant assistant) {
        List<BindingEntry> result = new ArrayList<>();

        for (Assistant.ItemUsageEntry entry : assistant.getItemUsages()) {
            int key = entry.setting().getValue();
            if (key == -999) continue;
            ItemStack stack = findStack(entry.itemUsage().getItem());
            if (stack.isEmpty()) continue;
            result.add(new BindingEntry(stack, entry.setting()));
        }

        for (Assistant.NamedKeyBind named : assistant.getNamedKeyBinds()) {
            int key = named.setting.getValue();
            if (key == -999) continue;
            ItemStack stack = findNamedStack(named.item, named.namePart);
            if (stack.isEmpty()) continue;
            result.add(new BindingEntry(stack, named.setting));
        }

        return result;
    }

    private ItemStack findStack(Item item) {
        if (mc.player == null) return new ItemStack(item);
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) return stack;
        }
        if (mc.player.getOffHandStack().getItem() == item) {
            return mc.player.getOffHandStack();
        }
        return ItemStack.EMPTY;
    }

    private ItemStack findNamedStack(Item item, String namePart) {
        if (mc.player == null) return ItemStack.EMPTY;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty() || stack.getItem() != item) continue;
            if (stack.getName().getString().toLowerCase(Locale.ROOT).contains(namePart)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static String getKeyDisplayName(int code) {
        if (code <= -100) {
            int mouseButton = 100 + code;
            return switch (mouseButton) {
                case 0 -> "LMB";
                case 1 -> "RMB";
                case 2 -> "MMB";
                default -> "M" + (mouseButton + 1);
            };
        }
        String mapped = KeyStorage.getKey(code);
        if (mapped != null && !mapped.isEmpty()) return mapped;
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(code, 0);
        return (name != null) ? name.toUpperCase() : "KEY" + code;
    }

    private record BindingEntry(ItemStack stack, BindSetting setting) {}
}
