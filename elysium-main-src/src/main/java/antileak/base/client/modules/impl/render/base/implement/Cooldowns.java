package antileak.base.client.modules.impl.render.base.implement;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UseCooldownComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
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
import antileak.base.client.modules.impl.misc.ServerHelper;
import antileak.base.mixin.ItemCooldownManagerAccessor;
import antileak.base.mixin.ItemCooldownManagerEntryAccessor;
import antileak.base.client.modules.impl.render.base.InterfaceProcessing;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Cooldowns extends InterfaceProcessing {

    private static final float BASE_MIN_WIDTH   = 76f;
    private static final float EXTRA_WIDTH      = 12f;
    private static final float ROW_RIGHT_MARGIN = 18f;
    private static final float ROW_HEIGHT       = 10f;
    private static final long CHAT_HINT_TTL_MS  = 120_000L;

    private final Map<Identifier, AnimationUtils> animations = new HashMap<>();
    private final Map<Identifier, CooldownSnapshot> snapshots = new HashMap<>();
    private final AnimationUtils widthAnimation = new AnimationUtils(70, 10.5f, Easings.QUAD_OUT);
    private static final Map<Identifier, ChatHint> chatHints = new ConcurrentHashMap<>();

    public Cooldowns(Draggable draggable) {
        super(draggable);
    }

    private Font issue(int size) {
        return Fonts.getFont("suisse", size);
    }

    private Font icon(int size) {
        return Fonts.getFont("icon", size);
    }
    private antileak.base.api.utils.render.fonts.ttf.MCFontRenderer divine_icons(int size) {
        return antileak.base.api.utils.render.fonts.ttf.Fonts.getFont("divine_icons.ttf", size);
    }
    private AnimationUtils getAnimation(Identifier group) {
        return animations.computeIfAbsent(group, key -> new AnimationUtils(0, 10.5f, Easings.QUAD_OUT));
    }

    public static void onGameMessage(String message) {
        if (message == null || message.isBlank()) return;

        String lower = message.toLowerCase(java.util.Locale.ROOT);
        long now = System.currentTimeMillis();

        putChatHint(lower, now, "использовал взрыв штучку", Registries.ITEM.getId(net.minecraft.item.Items.FIRE_CHARGE), "Взрыв штучка");
        putChatHint(lower, now, "использовал гул", Registries.ITEM.getId(net.minecraft.item.Items.FIREWORK_STAR), "Гул");
        putChatHint(lower, now, "использовал анти полет", Registries.ITEM.getId(net.minecraft.item.Items.FIREWORK_STAR), "Анти Полет");
        putChatHint(lower, now, "использовал стан", Registries.ITEM.getId(net.minecraft.item.Items.NETHER_STAR), "Стан");
        putChatHint(lower, now, "использовал взрыв трап", Registries.ITEM.getId(net.minecraft.item.Items.PRISMARINE_SHARD), "Взрыв трап");
        putChatHint(lower, now, "использовал снег заморозки", Registries.ITEM.getId(net.minecraft.item.Items.SNOWBALL), "Снег заморозки");
        putChatHint(lower, now, "использовал снег", Registries.ITEM.getId(net.minecraft.item.Items.SNOWBALL), "Снег");
        putChatHint(lower, now, "использовал трапку", Registries.ITEM.getId(net.minecraft.item.Items.POPPED_CHORUS_FRUIT), "Трапка");
        putChatHint(lower, now, "использовал ловушку", Registries.ITEM.getId(net.minecraft.item.Items.HEART_OF_THE_SEA), "Ловушка");
        putChatHint(lower, now, "использовал уник. трапка", Registries.ITEM.getId(net.minecraft.item.Items.CRYING_OBSIDIAN), "Уник. трапка");
        putChatHint(lower, now, "использовал деф лива", Registries.ITEM.getId(net.minecraft.item.Items.MAGMA_CREAM), "Деф лива");
        putChatHint(lower, now, "использовал лива с платформой", Registries.ITEM.getId(net.minecraft.item.Items.CLAY_BALL), "Лива с платформой");
        putChatHint(lower, now, "использовал дезориентацию", Registries.ITEM.getId(net.minecraft.item.Items.ENDER_EYE), "Дезориентация");
        putChatHint(lower, now, "использовал пласт", Registries.ITEM.getId(net.minecraft.item.Items.DRIED_KELP), "Пласт");
        putChatHint(lower, now, "использовал явную пыль", Registries.ITEM.getId(net.minecraft.item.Items.SUGAR), "Явная пыль");
        putChatHint(lower, now, "использовал божью ауру", Registries.ITEM.getId(net.minecraft.item.Items.PHANTOM_MEMBRANE), "Божья аура");
    }

    private static void putChatHint(String lowerMessage, long now, String needle, Identifier group, String displayName) {
        if (lowerMessage.contains(needle)) {
            chatHints.put(group, new ChatHint(displayName, now));
        }
    }

    private LinkedHashMap<Identifier, CooldownSnapshot> collectCooldowns() {
        LinkedHashMap<Identifier, CooldownSnapshot> result = new LinkedHashMap<>();
        if (mc == null || mc.player == null) return result;

        Object manager = mc.player.getItemCooldownManager();
        if (!(manager instanceof ItemCooldownManagerAccessor accessor)) return result;

        Map<Identifier, Object> entries = accessor.elysium$getEntries();
        if (entries == null || entries.isEmpty()) return result;

        for (Map.Entry<Identifier, Object> cooldownEntry : entries.entrySet()) {
            Identifier group = cooldownEntry.getKey();
            Object entry = cooldownEntry.getValue();
            if (!(entry instanceof ItemCooldownManagerEntryAccessor entryAccessor)) continue;

            float remainingSeconds = getRemainingSeconds(accessor, entryAccessor);
            if (remainingSeconds <= 0.01f) continue;

            ItemStack stack = findStackForGroup(group);
            CooldownSnapshot snapshot = snapshots.get(group);
            if (stack != null) {
                snapshot = new CooldownSnapshot(stack.copy(), resolveDisplayName(stack.getItem(), stack));
            } else if (snapshot == null) {
                snapshot = createSnapshotForGroup(group);
                if (snapshot == null) continue;
            }

            snapshots.put(group, snapshot);
            result.put(group, snapshot);
        }

        return result;
    }

    private ItemStack findStackForGroup(Identifier group) {
        if (mc == null || mc.player == null) return null;

        List<ItemStack> stacks = new java.util.ArrayList<>();
        stacks.addAll(mc.player.getInventory().main);
        stacks.addAll(mc.player.getInventory().armor);
        stacks.addAll(mc.player.getInventory().offHand);

        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            if (group.equals(getCooldownGroup(stack))) {
                return stack;
            }
        }
        return null;
    }

    private CooldownSnapshot createSnapshotForGroup(Identifier group) {
        Item item = Registries.ITEM.get(group);
        if (item == null) return null;

        ItemStack stack = item.getDefaultStack();
        if (stack.isEmpty()) return null;

        return new CooldownSnapshot(stack, resolveDisplayName(item, stack));
    }

    private String resolveDisplayName(Item item, ItemStack stack) {
        String serverHelperName = resolveServerHelperName(item);
        if (serverHelperName != null) {
            return serverHelperName;
        }

        String chatHint = resolveChatHint(item);
        if (chatHint != null) {
            return chatHint;
        }

        return stack.getName().getString();
    }

    private String resolveServerHelperName(Item item) {
        if (ServerHelper.INSTANCE == null) return null;

        return ServerHelper.INSTANCE.resolveHelperBindName(item);
    }

    private String resolveChatHint(Item item) {
        Identifier group = Registries.ITEM.getId(item);
        ChatHint hint = chatHints.get(group);
        if (hint == null) return null;

        if (System.currentTimeMillis() - hint.timestamp > CHAT_HINT_TTL_MS) {
            chatHints.remove(group);
            return null;
        }

        return hint.displayName;
    }

    private Identifier getCooldownGroup(ItemStack stack) {
        UseCooldownComponent useCooldown = stack.get(DataComponentTypes.USE_COOLDOWN);
        if (useCooldown != null) {
            return useCooldown.cooldownGroup().orElse(Registries.ITEM.getId(stack.getItem()));
        }
        return Registries.ITEM.getId(stack.getItem());
    }

    private float getRemainingSeconds(ItemCooldownManagerAccessor accessor, ItemCooldownManagerEntryAccessor entryAccessor) {
        int currentTick = accessor.elysium$getTick();
        int remainingTicks = entryAccessor.elysium$getEndTick() - currentTick;
        return Math.max(0f, remainingTicks / 20f);
    }

    private float getRemainingSeconds(Identifier group) {
        if (mc == null || mc.player == null) return 0f;

        Object manager = mc.player.getItemCooldownManager();
        if (!(manager instanceof ItemCooldownManagerAccessor accessor)) return 0f;

        Map<Identifier, Object> entries = accessor.elysium$getEntries();
        if (entries == null) return 0f;

        Object entry = entries.get(group);
        if (!(entry instanceof ItemCooldownManagerEntryAccessor entryAccessor)) return 0f;

        return getRemainingSeconds(accessor, entryAccessor);
    }

    private void drawItemIcon(DrawContext context, ItemStack stack, float x, float y, float scale) {
        MatrixStack matrices = context.getMatrices();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        matrices.push();
        matrices.translate(x, y, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        context.drawItem(stack, 0, 0);
        matrices.pop();
        RenderSystem.depthMask(true);
        RenderSystem.disableDepthTest();
    }

    @Override
    public void onRender(EventRender.Default eventRender) {
        DefaultStyle(eventRender);
        super.onRender(eventRender);
    }

    public void DefaultStyle(EventRender.Default eventRender) {
        float x = draggable.getX();
        float y = draggable.getY();

        int colorTheme;
        if (!elysium.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            colorTheme = elysium.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        } else {
            colorTheme = ColorUtils.getThemeColor();
        }

        LinkedHashMap<Identifier, CooldownSnapshot> activeCooldowns = collectCooldowns();
        for (Identifier group : activeCooldowns.keySet()) {
            getAnimation(group).update(1f);
        }

        for (Map.Entry<Identifier, AnimationUtils> entry : animations.entrySet()) {
            if (!activeCooldowns.containsKey(entry.getKey())) {
                entry.getValue().update(0f);
            }
        }

        List<Identifier> renderOrder = new java.util.ArrayList<>(activeCooldowns.keySet());
        for (Identifier group : animations.keySet()) {
            if (!activeCooldowns.containsKey(group)) {
                renderOrder.add(group);
            }
        }
        int visibleCount = 0;
        float targetWidth = BASE_MIN_WIDTH;
        float targetHeight = 16f;

        for (Identifier group : renderOrder) {
            CooldownSnapshot snapshot = snapshots.get(group);
            if (snapshot == null) continue;

            float animValue = getAnimation(group).getValue();
            if (animValue <= 0.01f) continue;

            float remainingSeconds = getRemainingSeconds(group);
            String timeText = snapshot.getTimeText(remainingSeconds);
            float rowWidth = issue(12).getWidth(snapshot.displayName)
                    + issue(10).getWidth(timeText)
                    + ROW_RIGHT_MARGIN;
            if (rowWidth > targetWidth) targetWidth = rowWidth;
            targetHeight += ROW_HEIGHT * animValue;
            visibleCount++;
        }

        if (visibleCount > 0) {
            targetHeight += 2f;
        }

        widthAnimation.update(targetWidth);
        float width = widthAnimation.getValue() + EXTRA_WIDTH;
        float height = targetHeight;

        RenderUtils.drawDefaultHudElementRects(eventRender.getContext().getMatrices(), x, y, width, height, colorTheme, isUnusualRectType());
        issue(14).draw(eventRender.getContext().getMatrices(), "Cooldowns", x + 5f, y + 6f, -1);
        divine_icons(15).drawString("e", x + width - 13.5f, y + 7f, colorTheme);

        float offsetY = 18f;
        for (Identifier group : renderOrder) {
            CooldownSnapshot snapshot = snapshots.get(group);
            if (snapshot == null) continue;

            AnimationUtils animation = getAnimation(group);
            float animValue = animation.getValue();
            if (animValue <= 0.01f) continue;

            ScissorUtils.push();
            ScissorUtils.setFromComponentCoordinates(x, y, width, height);

            int alpha = (int) (255 * animValue);
            int textColor = ColorUtils.rgba(255, 255, 255, alpha);
            int timeColor = ColorUtils.rgba(161, 161, 161, alpha);

            drawItemIcon(eventRender.getContext(), snapshot.stack, x + 3.8f, y + offsetY - 0.9f - 3, 0.6f);

            float textX = x + 15.5f;
            issue(12).draw(eventRender.getContext().getMatrices(), snapshot.displayName, textX, y + offsetY, textColor);

            float remainingSeconds = getRemainingSeconds(group);
            String timeText = snapshot.getTimeText(remainingSeconds);
            float timeWidth = issue(10).getWidth(timeText);
            float timeX = x + width - timeWidth - 5f;
            issue(10).draw(eventRender.getContext().getMatrices(), timeText, timeX, y + offsetY + 0.5f, timeColor);

            offsetY += ROW_HEIGHT * animValue;
            ScissorUtils.pop();
            ScissorUtils.unset();
        }

        animations.entrySet().removeIf(entry -> !activeCooldowns.containsKey(entry.getKey()) && entry.getValue().getValue() <= 0.01f);
        snapshots.keySet().removeIf(group -> !animations.containsKey(group));

        draggable.setWidth(width);
        draggable.setHeight(height);
    }

    private static final class CooldownSnapshot {
        private final ItemStack stack;
        private final String displayName;

        private CooldownSnapshot(ItemStack stack, String displayName) {
            this.stack = stack;
            this.displayName = displayName;
        }

        private String getTimeText(float remainingSeconds) {
            float seconds = Math.max(0f, remainingSeconds);
            if (seconds >= 10f) {
                return String.format(java.util.Locale.ROOT, "%.0fs", seconds);
            }
            return String.format(java.util.Locale.ROOT, "%.1fs", seconds);
        }
    }

    private record ChatHint(String displayName, long timestamp) {
    }
}
