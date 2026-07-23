package dev.aethel.util.auction.ab;

import dev.aethel.util.IMinecraft;
import dev.aethel.util.auction.AuctionUtil;
import dev.aethel.util.render.math.MathUtil;
import lombok.Getter;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AutoPriceParser implements IMinecraft {
    @Getter
    private static boolean enabled = false;
    @Getter
    private static int discountPercent = 30;

    private static Stage stage = Stage.IDLE;
    private static long stageUntilMs = 0;
    private static int currentIndex = 0;
    private static List<BuyableItem> itemsToProcess = new ArrayList<>();
    private static BuyableItem currentItem = null;
    private static int retryCount = 0;
    private static long noScreenTimeout = 0;
    private static int totalItems = 0;

    private enum Stage {
        IDLE,
        SEND_SEARCH,
        WAIT_RESULTS,
        WAIT_BETWEEN,
        FINISH_WAIT
    }

    public static void setDiscountPercent(int percent) {
        discountPercent = Math.max(0, Math.min(100, percent));
    }

    public static void setEnabled(boolean value) {
        if (enabled == value) return;

        if (value) {
            BuyableRegistry.init();
            List<BuyableItem> allItems = BuyableRegistry.getAll();

            ArrayList<BuyableItem> toProcess = new ArrayList<>();
            for (BuyableItem item : allItems) {
                if (item.isEnabled()) {
                    toProcess.add(item);
                }
            }

            if (toProcess.isEmpty()) {
                sendMsg("§cНет активных предметов для парсинга. Добавьте предметы в AutoBuy.");
                return;
            }

            Collections.shuffle(toProcess);
            itemsToProcess = toProcess;
            enabled = true;
            stage = Stage.SEND_SEARCH;
            stageUntilMs = 0;
            currentIndex = 0;
            currentItem = null;
            totalItems = itemsToProcess.size();
            retryCount = 0;

            sendMsg("§8[AutoParser] Запущен парсинг " + totalItems + " предметов...");
        } else {
            enabled = false;
            stage = Stage.IDLE;
            currentIndex = 0;
            currentItem = null;
            itemsToProcess.clear();
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
        if (retryCount < 2 && currentItem != null) {
            String searchQuery = getSearchQuery(currentItem.getName());
            mc.player.networkHandler.sendChatCommand("ah search " + searchQuery);
            stageUntilMs = now + 1000;
            noScreenTimeout = now + 2000;
        } else {
            currentIndex++;
            retryCount = 0;
            stage = Stage.WAIT_BETWEEN;
            stageUntilMs = now + (long) MathUtil.random(300f, 400f);
            noScreenTimeout = 0;
        }
    }

    public static void tickAuction(GenericContainerScreen screen) {
        if (!enabled || mc.player == null) return;
        if (itemsToProcess.isEmpty()) {
            disableAndCleanup();
            return;
        }

        noScreenTimeout = 0;
        long now = System.currentTimeMillis();

        switch (stage) {
            case SEND_SEARCH -> {
                if (currentIndex >= itemsToProcess.size()) {
                    stage = Stage.FINISH_WAIT;
                    stageUntilMs = now + 2000;
                    sendMsg("§8[AutoParser] Все предметы обработаны");
                    return;
                }

                currentItem = itemsToProcess.get(currentIndex);
                retryCount = 0;
                String searchQuery = getSearchQuery(currentItem.getName());

                sendMsg("§7[AutoParser] (" + (currentIndex + 1) + "/" + totalItems + ") Ищем §f" + currentItem.getName() + "§7...");
                mc.player.networkHandler.sendChatCommand("ah search " + searchQuery);

                stage = Stage.WAIT_RESULTS;
                stageUntilMs = now + (long) MathUtil.random(800f, 1000f);
            }

            case WAIT_RESULTS -> {
                if (now < stageUntilMs) return;

                int cheapest = findCheapestPrice(screen, currentItem);

                if (cheapest > 0) {
                    int newPrice = (int) Math.floor(cheapest * (100.0 - discountPercent) / 100.0);
                    if (newPrice < 1) newPrice = 1;

                    currentItem.setMaxPrice(newPrice);
                    BuyableRegistry.save();
                    sendMsg("§7[AutoParser] §f" + currentItem.getName() + " §a— $" + formatPrice(cheapest) + " → $" + formatPrice(newPrice) + " (скидка " + discountPercent + "%)");

                    currentIndex++;
                    retryCount = 0;
                    stage = Stage.WAIT_BETWEEN;
                    stageUntilMs = now + (long) MathUtil.random(600f, 7000f);
                } else {
                    retryCount++;
                    if (retryCount < 2) {
                        String searchQuery = getSearchQuery(currentItem.getName());
                        mc.player.networkHandler.sendChatCommand("ah search " + searchQuery);
                        stageUntilMs = now + (long) MathUtil.random(700f, 800f);
                    } else {
                        currentIndex++;
                        retryCount = 0;
                        stage = Stage.WAIT_BETWEEN;
                        stageUntilMs = now + (long) MathUtil.random(600f, 900f);
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

    private static String getSearchQuery(String itemName) {
        if (itemName.contains("Рюкзак IV")) return "Рюкзак 4 уровень";
        if (itemName.contains("Рюкзак III")) return "Рюкзак 3 уровень";
        if (itemName.contains("Рюкзак II")) return "Рюкзак 2 уровень";
        if (itemName.contains("Рюкзак I")) return "Рюкзак 1 уровень";
        if (itemName.contains("Рюкзак Infinity")) return "Рюкзак инфинити";
        if (itemName.contains("Трезубец с самонаводкой")) return "Трезубец";
        if (itemName.contains("Осколочное эндер яйцо")) return "осколочное яйцо энд";
        if (itemName.contains("Фармер")) return "Фармер";
        if (itemName.contains("Чарка")) return "Зачарованное золотое яблоко";
        if (itemName.contains("Гепл")) return "Золотое яблоко";
        if (itemName.contains("Тотем")) return "Тотем бессмертия";

        return itemName;
    }

    private static int findCheapestPrice(GenericContainerScreen screen, BuyableItem targetItem) {
        if (screen == null || targetItem == null) return -1;

        int cheapest = Integer.MAX_VALUE;

        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot == null || slot.id > 44) continue;

            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            if (!targetItem.matches(stack)) continue;

            int price = AuctionUtil.getPrice(stack);
            if (price <= 0) continue;

            int count = Math.max(1, stack.getCount());
            int pricePerItem = price / count;

            if (pricePerItem > 0 && pricePerItem < cheapest) {
                cheapest = pricePerItem;
            }
        }

        return cheapest == Integer.MAX_VALUE ? -1 : cheapest;
    }

    private static void disableAndCleanup() {
        if (mc.player != null) {
            mc.player.networkHandler.sendChatCommand("ah");
        }

        sendMsg("§a[AutoParser] Парсинг завершён! Обработано " + Math.min(currentIndex, totalItems) + "/" + totalItems + " предметов.");

        enabled = false;
        stage = Stage.IDLE;
        currentIndex = 0;
        currentItem = null;
        itemsToProcess.clear();
    }

    private static void sendMsg(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§7[§bAethel§7] §f" + message), false);
        }
    }

    private static String formatPrice(int price) {
        StringBuilder sb = new StringBuilder();
        String s = String.valueOf(price);
        int c = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            if (c > 0 && c % 3 == 0) sb.insert(0, '.');
            sb.insert(0, s.charAt(i));
            c++;
        }
        return sb.toString();
    }
}
