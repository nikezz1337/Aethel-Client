package dev.ethereal.client.features.modules.other.autobuy;

import dev.ethereal.api.utils.other.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class FunTimePriceParser {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static boolean enabled = false;
    private static int discountPercent = 0;
    private static final Set<String> parserItemNames = new HashSet<>();

    private static Stage stage = Stage.IDLE;
    private static long stageUntilMs = 0;
    private static int currentIndex = 0;
    private static List<String> itemNames = new ArrayList<>();
    private static String currentItemName = null;
    private static int retryCount = 0;
    private static final int MAX_RETRIES = 2;
    private static long noScreenTimeout = 0;
    private static int totalItems = 0;
    private static final Pattern LEVEL_PATTERN = Pattern.compile("(.+?)\\s*\\[(\\d+)\\s*[уУ]р\\.?\\]");

    private enum Stage {
        IDLE, SEND_SEARCH, WAIT_RESULTS, WAIT_BETWEEN, FINISH_WAIT
    }

    private static String cleanSearchName(String name) {
        if (name == null) return "";
        String clean = name.replace("[★] ", "").replace("[★]", "").trim();
        var m = LEVEL_PATTERN.matcher(clean);
        if (m.find()) {
            String base = m.group(1).trim().replace(" опыта", "").replace(" Опыта", "").trim();
            clean = base + " с уровнем " + m.group(2);
        }
        return clean;
    }

    public static boolean isEnabled() { return enabled; }
    public static boolean hasItem(String name) { return name != null && parserItemNames.contains(name.toLowerCase()); }
    public static Set<String> getParserItemNames() { return new HashSet<>(parserItemNames); }
    public static int getDiscountPercent() { return discountPercent; }
    public static void setDiscountPercent(int percent) { discountPercent = Math.max(0, Math.min(100, percent)); }

    public static void setItemsForParser(List<String> names) {
        parserItemNames.clear();
        if (names != null) { for (String n : names) { if (n != null && !n.isEmpty()) parserItemNames.add(n.toLowerCase()); } }
    }

    public static void setEnabled(boolean value) {
        if (enabled == value) return;
        if (value) {
            if (parserItemNames.isEmpty()) { TextUtil.sendMessage("§cAutoParser: Нет предметов для парсинга!"); return; }
            enabled = true;
            stage = Stage.SEND_SEARCH;
            stageUntilMs = 0;
            currentIndex = 0;
            currentItemName = null;
            itemNames = new ArrayList<>(parserItemNames);
            totalItems = itemNames.size();
            retryCount = 0;
            TextUtil.sendMessage("§aAutoParser: Запущен парсинг " + totalItems + " предметов...");
        } else {
            enabled = false;
            stage = Stage.IDLE;
            currentIndex = 0;
            currentItemName = null;
        }
    }

    public static void tickNoContainer() {
        if (!enabled || mc.player == null) return;
        if (stage != Stage.WAIT_RESULTS) return;
        long now = System.currentTimeMillis();

        if (noScreenTimeout == 0) {
            noScreenTimeout = now + 2000;
            return;
        }
        if (now < noScreenTimeout) return;

        retryCount++;
        if (retryCount < MAX_RETRIES && currentItemName != null) {
            TextUtil.sendMessage("§cAutoParser: Экран не открылся, повтор (" + retryCount + "/" + MAX_RETRIES + ")");
            mc.player.networkHandler.sendChatCommand("ah search " + cleanSearchName(currentItemName));
            stageUntilMs = now + 1000;
            noScreenTimeout = now + 2000;
        } else {
            TextUtil.sendMessage("§cAutoParser: Не удалось найти §f" + currentItemName + " §c(экран не открылся)");
            currentIndex++;
            retryCount = 0;
            stage = Stage.WAIT_BETWEEN;
            stageUntilMs = now + 300;
            noScreenTimeout = 0;
        }
    }

    public static void tickAuction(List<Slot> slots) {
        if (!enabled || mc.player == null) return;
        if (itemNames.isEmpty()) { disableAndCleanup(); return; }

        noScreenTimeout = 0;
        long now = System.currentTimeMillis();

        switch (stage) {
            case SEND_SEARCH -> {
                if (currentIndex >= itemNames.size()) {
                    stage = Stage.FINISH_WAIT;
                    stageUntilMs = now + 2000;
                    TextUtil.sendMessage("§aAutoParser: Все предметы обработаны, завершение...");
                    return;
                }
                currentItemName = itemNames.get(currentIndex);
                retryCount = 0;
                String query = cleanSearchName(currentItemName);
                TextUtil.sendMessage("§7AutoParser: (" + (currentIndex + 1) + "/" + totalItems + ") Ищем §f" + currentItemName + "§7...");
                mc.player.networkHandler.sendChatCommand("ah search " + query);
                stage = Stage.WAIT_RESULTS;
                stageUntilMs = now + 1000;
            }
            case WAIT_RESULTS -> {
                if (now < stageUntilMs) return;

                AutoBuyableItem targetItem = findItemByName(currentItemName);
                if (targetItem == null) {
                    TextUtil.sendMessage("§cAutoParser: Предмет §f" + currentItemName + " §cне найден в конфиге");
                    currentIndex++;
                    stage = Stage.WAIT_BETWEEN;
                    stageUntilMs = now + 300;
                    return;
                }

                int cheapest = findCheapest(slots, targetItem);
                if (cheapest > 0) {
                    int newPrice = (int) Math.floor(cheapest * (100.0 - discountPercent) / 100.0);
                    if (newPrice < 0) newPrice = 0;
                    targetItem.getSettings().setBuyBelow(newPrice);
                    AutoBuyManager.getInstance().savePrice(targetItem.getDisplayName(), newPrice);
                    TextUtil.sendMessage("§aAutoParser: §f" + targetItem.getDisplayName() + " §a— $" + cheapest + " → $" + newPrice + " (скидка " + discountPercent + "%)");
                    currentIndex++;
                    retryCount = 0;
                    stage = Stage.WAIT_BETWEEN;
                    stageUntilMs = now + 800;
                } else {
                    retryCount++;
                    if (retryCount < MAX_RETRIES && currentItemName != null) {
                        String query = cleanSearchName(currentItemName);
                        TextUtil.sendMessage("§cAutoParser: Ничего не найдено, повтор (" + retryCount + "/" + MAX_RETRIES + ")");
                        mc.player.networkHandler.sendChatCommand("ah search " + query);
                        stageUntilMs = now + 1000;
                    } else {
                        TextUtil.sendMessage("§cAutoParser: Не удалось найти цену для §f" + currentItemName);
                        currentIndex++;
                        retryCount = 0;
                        stage = Stage.WAIT_BETWEEN;
                        stageUntilMs = now + 800;
                    }
                }
            }
            case WAIT_BETWEEN -> {
                if (now < stageUntilMs) return;
                stage = Stage.SEND_SEARCH;
            }
            case FINISH_WAIT -> {
                if (now < stageUntilMs) return;
                disableAndCleanup();
            }
        }
    }

    private static void disableAndCleanup() {
        if (mc.player != null) mc.player.networkHandler.sendChatCommand("ah");
        AutoBuyManager.getInstance().setEnabled(true);
        TextUtil.sendMessage("§aAutoParser: Парсинг завершён! Обработано " + Math.min(currentIndex, totalItems) + "/" + totalItems + " предметов.");
        enabled = false;
        stage = Stage.IDLE;
        currentIndex = 0;
        currentItemName = null;
    }

    private static int findCheapest(List<Slot> slots, AutoBuyableItem targetItem) {
        if (slots == null || targetItem == null) return -1;
        String expectedLower = targetItem.getDisplayName().toLowerCase().trim();
        int best = Integer.MAX_VALUE;

        for (int i = 0; i <= 44 && i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot == null || slot.getStack().isEmpty()) continue;

            ItemStack stack = slot.getStack();
            if (FunTimeAuctionHandler.isArmorItem(stack) && FunTimeAuctionHandler.hasThornsEnchantment(stack)) continue;

            String stackName = stack.getName().getString();
            String cleanName = stackName.replaceAll("§.", "").trim().toLowerCase();

            boolean matches = cleanName.contains(expectedLower) || expectedLower.contains(cleanName);
            if (!matches) continue;

            int price = FunTimeAuctionHandler.getPrice(stack);
            if (price <= 0) continue;

            int qty = stack.getCount();
            if (qty <= 0) qty = 1;
            int perItem = price / qty;
            if (perItem > 0 && perItem < best) best = perItem;
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private static AutoBuyableItem findItemByName(String displayName) {
        if (displayName == null) return null;
        for (AutoBuyableItem it : FunTimeProvider.getItems()) {
            if (it != null && displayName.equalsIgnoreCase(it.getDisplayName())) return it;
        }
        return null;
    }
}
