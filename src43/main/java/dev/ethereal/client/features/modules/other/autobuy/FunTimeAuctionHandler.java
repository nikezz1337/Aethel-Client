package dev.ethereal.client.features.modules.other.autobuy;

import dev.ethereal.api.utils.auction.AuctionUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FunTimeAuctionHandler {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static final Pattern PRICE_PATTERN = Pattern.compile("\\$(\\d{1,3}(?:[\\s,.]\\d{3})*)");
    private static final Pattern PRICE_ALT = Pattern.compile("(\\d{1,3}(?:[\\s,.]\\d{3})*)\\s*\\$");

    private static final long ITEM_TTL = 30000;
    private static final long CLEANUP_INTERVAL = 30000;
    private static final long NO_MONEY_COOLDOWN = 2000;

    private long refreshInterval = 350;
    private long buyDelay = 20;
    private long lastBuyTime = 0;

    private static long noMoneyCooldownUntil = 0;

    private final Map<String, Long> processedSlots = new HashMap<>();
    private long lastCleanup = 0;
    private long lastRefresh = 0;

    public void setRefreshInterval(long ms) { this.refreshInterval = ms; }
    public void setBuyDelay(long ms) { this.buyDelay = ms; }

    public static void setNoMoneyCooldown() {
        noMoneyCooldownUntil = System.currentTimeMillis() + NO_MONEY_COOLDOWN;
    }

    public void clear() {
        processedSlots.clear();
        lastCleanup = 0;
        lastRefresh = 0;
    }

    public void tick(int syncId, List<Slot> slots, List<AutoBuyableItem> items) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (items == null || items.isEmpty()) return;
        if (System.currentTimeMillis() < noMoneyCooldownUntil) return;

        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL) {
            processedSlots.entrySet().removeIf(e -> now - e.getValue() > ITEM_TTL);
            lastCleanup = now;
        }

        for (AutoBuyableItem item : items) {
            if (item == null || !item.isEnabled()) continue;
            int maxPrice = getMaxPrice(item);
            if (maxPrice <= 0) continue;

            ItemStack target = item.createItemStack();

            for (int i = 0; i <= 44 && i < slots.size(); i++) {
                Slot slot = slots.get(i);
                if (slot == null || slot.getStack().isEmpty()) continue;

                ItemStack stack = slot.getStack();
                if (isArmorItem(stack) && hasThornsEnchantment(stack)) continue;

                int price = getPrice(stack);
                if (price <= 0 || price > maxPrice) continue;

                String key = i + "|" + price;
                if (processedSlots.containsKey(key)) continue;

                if (!compareItem(stack, target)) continue;

                if (now - lastBuyTime < buyDelay) continue;

                if (!strictVerify(stack, target, item, price, maxPrice)) continue;

                if (mc.player != null) {
                    mc.player.sendMessage(Text.literal("§7[AutoBuy] §fПокупка: §a" + item.getDisplayName() + " §f| Цена АН: §e$" + formatPrice(price) + " §f| Макс в GUI: §c$" + formatPrice(maxPrice)), false);
                }

                mc.interactionManager.clickSlot(syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                processedSlots.put(key, now);
                lastBuyTime = now;
                return;
            }
        }

        if (now - lastRefresh > refreshInterval && now - lastBuyTime > buyDelay + 50) {
            mc.interactionManager.clickSlot(syncId, 49, 0, SlotActionType.QUICK_MOVE, mc.player);
            processedSlots.clear();
            lastRefresh = now;
        }
    }

    public void handleSuspiciousPrice(int syncId, List<Slot> slots) {
        for (Slot slot : slots) {
            if (!slot.getStack().isEmpty() && slot.getStack().getItem() == Items.GREEN_STAINED_GLASS_PANE) {
                mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                return;
            }
        }
    }

    private int getMaxPrice(AutoBuyableItem item) {
        int p = item.getSettings().getBuyBelow();
        if (p > 0) return p;
        p = item.getPrice();
        return p > 0 ? p : 0;
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

    private boolean strictVerify(ItemStack auctionStack, ItemStack target, AutoBuyableItem item, int price, int maxPrice) {
        if (!item.isEnabled()) return false;
        if (item.getSettings().getBuyBelow() > 0 && item.getSettings().getBuyBelow() != maxPrice) {
            int currentMax = item.getSettings().getBuyBelow();
            if (price <= 0 || price > currentMax) return false;
        }
        if (price > maxPrice) return false;
        if (!compareItem(auctionStack, target)) return false;
        return true;
    }

    public static int getPrice(ItemStack stack) {
        if (mc.player == null) return -1;

        int bestPrice = -1;

        for (Text text : stack.getTooltip(Item.TooltipContext.DEFAULT, mc.player, TooltipType.BASIC)) {
            String str = text.getString().replace("§r", "").trim();
            if (str.contains("Цена:") || str.contains("Ценa:") || str.contains("Price:")) {
                String s = str.replaceAll("[^0-9]", "").trim();
                if (!s.isEmpty()) try { int p = Integer.parseInt(s); if (p > bestPrice) bestPrice = p; } catch (Exception ignored) {}
            }
            var m = PRICE_PATTERN.matcher(str); if (m.find()) { try { int p = Integer.parseInt(m.group(1).replaceAll("[^0-9]", "")); if (p > bestPrice) bestPrice = p; } catch (Exception ignored) {} }
            m = PRICE_ALT.matcher(str); if (m.find()) { try { int p = Integer.parseInt(m.group(1).replaceAll("[^0-9]", "")); if (p > bestPrice) bestPrice = p; } catch (Exception ignored) {} }
        }

        if (bestPrice > 0) return bestPrice;

        var tag = stack.getComponents();
        if (tag == null) return -1;

        String comp = tag.toString();
        String priceStr = null;

        int idx = comp.indexOf("literal{ $");
        if (idx != -1) {
            int end = comp.indexOf("}[style={color=green}]", idx);
            if (end != -1) {
                String between = comp.substring(idx + 10, end);
                if (!between.isEmpty()) priceStr = between;
            }
        }

        if (priceStr == null) { var m = PRICE_PATTERN.matcher(comp); if (m.find()) priceStr = m.group(1); }
        if (priceStr == null) { var m = PRICE_ALT.matcher(comp); if (m.find()) priceStr = m.group(1); }

        if (priceStr == null) {
            String name = stack.getName().getString();
            var m = PRICE_PATTERN.matcher(name);
            if (m.find()) priceStr = m.group(1);
            if (priceStr == null) { m = PRICE_ALT.matcher(name); if (m.find()) priceStr = m.group(1); }
        }

        if (priceStr == null) {
            var lore = stack.get(DataComponentTypes.LORE);
            if (lore != null) {
                for (Text line : lore.lines()) {
                    String t = line.getString();
                    if (t.contains("Цена:") || t.contains("Ценa:") || t.contains("Price:")) {
                        String s = t.replaceAll("[^0-9]", "").trim();
                        if (!s.isEmpty()) try { int p = Integer.parseInt(s); if (p > bestPrice) bestPrice = p; } catch (Exception ignored) {}
                    }
                }
            }
        }

        if (priceStr != null) {
            try { int p = Integer.parseInt(priceStr.replaceAll("[^0-9]", "")); if (p > bestPrice) bestPrice = p; } catch (NumberFormatException ignored) {}
        }

        if (bestPrice > 0) return bestPrice;

        int tp = AuctionUtil.getPrice(stack);
        if (tp > 0) return tp;

        return -1;
    }

    public static boolean softMatchEnchants(ItemStack auction, ItemStack target) {
        var tEnch = target.get(DataComponentTypes.ENCHANTMENTS);
        if (tEnch == null || tEnch.isEmpty()) return true;
        var aEnch = auction.get(DataComponentTypes.ENCHANTMENTS);
        if (aEnch == null || aEnch.isEmpty()) return false;
        for (RegistryEntry<Enchantment> entry : tEnch.getEnchantments()) {
            int tLevel = tEnch.getLevel(entry);
            int aLevel = aEnch.getLevel(entry);
            if (aLevel < tLevel) return false;
        }
        return true;
    }

    public static boolean compareItem(ItemStack a, ItemStack b) {
        if (a.getItem() != b.getItem()) return false;
        if (isArmorItem(a) && hasThornsEnchantment(a)) return false;

        if (!matchEnchantments(a, b)) return false;
        if (!matchPotionEffects(a, b)) return false;
        if (!matchSpecialNbt(a, b)) return false;
        if (!matchAttributes(a, b)) return false;

        var aLore = a.get(DataComponentTypes.LORE);
        var bLore = b.get(DataComponentTypes.LORE);
        boolean hasLore = bLore != null && !bLore.lines().isEmpty();

        if (hasLore) {
            if (aLore == null || aLore.lines().isEmpty()) return false;
            List<String> aLoreStrs = aLore.lines().stream()
                .map(t -> cleanString(t.getString())).filter(s -> !s.isEmpty()).collect(Collectors.toList());
            String aLoreJoined = String.join(" ", aLoreStrs);
            int match = 0;
            int required = 0;
            for (Text expected : bLore.lines()) {
                String e = cleanString(expected.getString());
                if (e.isEmpty()) continue;
                required++;
                if (aLoreStrs.stream().anyMatch(l -> l.contains(e) || e.contains(l)) || aLoreJoined.contains(e))
                    match++;
            }
            if (required > 0 && (double) match / required < 0.8) return false;
        }

        return true;
    }

    private static boolean matchEnchantments(ItemStack a, ItemStack b) {
        var bEnchants = b.get(DataComponentTypes.ENCHANTMENTS);
        if (bEnchants == null || bEnchants.isEmpty()) return true;
        var aEnchants = a.get(DataComponentTypes.ENCHANTMENTS);
        if (aEnchants == null || aEnchants.isEmpty()) return false;
        for (RegistryEntry<Enchantment> entry : bEnchants.getEnchantments()) {
            int bLevel = bEnchants.getLevel(entry);
            int aLevel = aEnchants.getLevel(entry);
            if (aLevel < bLevel) return false;
        }
        return true;
    }

    private static boolean matchPotionEffects(ItemStack a, ItemStack b) {
        var bCustom = b.get(DataComponentTypes.CUSTOM_DATA);
        if (bCustom == null) return true;
        var bNbt = bCustom.copyNbt();
        if (bNbt == null) return true;
        if (!bNbt.contains("effects", NbtElement.LIST_TYPE)) return true;

        NbtList bEffects = bNbt.getList("effects", NbtElement.COMPOUND_TYPE);
        if (bEffects.isEmpty()) return true;

        var aCustom = a.get(DataComponentTypes.CUSTOM_DATA);
        if (aCustom == null) return false;
        var aNbt = aCustom.copyNbt();
        if (aNbt == null) return false;
        if (!aNbt.contains("effects", NbtElement.LIST_TYPE)) return false;

        NbtList aEffects = aNbt.getList("effects", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < bEffects.size(); i++) {
            NbtCompound bEffect = bEffects.getCompound(i);
            String bId = bEffect.getString("effectId");
            int bAmp = bEffect.getInt("amplifier");
            int bDur = bEffect.getInt("duration");

            boolean found = false;
            for (int j = 0; j < aEffects.size(); j++) {
                NbtCompound aEffect = aEffects.getCompound(j);
                if (aEffect.getString("effectId").equals(bId) &&
                    aEffect.getInt("amplifier") >= bAmp &&
                    aEffect.getInt("duration") >= bDur) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static boolean matchSpecialNbt(ItemStack a, ItemStack b) {
        var bCustom = b.get(DataComponentTypes.CUSTOM_DATA);
        if (bCustom == null) return true;
        var bNbt = bCustom.copyNbt();
        if (bNbt == null) return true;
        if (!bNbt.contains("funItemType", NbtElement.STRING_TYPE)) return true;

        String bType = bNbt.getString("funItemType");
        var aCustom = a.get(DataComponentTypes.CUSTOM_DATA);
        if (aCustom == null) return false;
        var aNbt = aCustom.copyNbt();
        if (aNbt == null) return false;
        if (!aNbt.contains("funItemType", NbtElement.STRING_TYPE)) return false;
        return bType.equals(aNbt.getString("funItemType"));
    }

    private static boolean matchAttributes(ItemStack a, ItemStack b) {
        var bCustom = b.get(DataComponentTypes.CUSTOM_DATA);
        if (bCustom == null) return true;
        var bNbt = bCustom.copyNbt();
        if (bNbt == null) return true;
        if (!bNbt.contains("AttributeModifiers", NbtElement.LIST_TYPE)) return true;

        NbtList bAttrs = bNbt.getList("AttributeModifiers", NbtElement.COMPOUND_TYPE);
        if (bAttrs.isEmpty()) return true;

        var aCustom = a.get(DataComponentTypes.CUSTOM_DATA);
        if (aCustom == null) return false;
        var aNbt = aCustom.copyNbt();
        if (aNbt == null) return false;
        if (!aNbt.contains("AttributeModifiers", NbtElement.LIST_TYPE)) return false;

        NbtList aAttrs = aNbt.getList("AttributeModifiers", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < bAttrs.size(); i++) {
            NbtCompound bAttr = bAttrs.getCompound(i);
            String bName = bAttr.getString("AttributeName");
            double bAmount = bAttr.getDouble("Amount");
            int bOp = bAttr.getInt("Operation");
            String bSlot = bAttr.getString("Slot");

            boolean found = false;
            for (int j = 0; j < aAttrs.size(); j++) {
                NbtCompound aAttr = aAttrs.getCompound(j);
                if (aAttr.getString("AttributeName").equals(bName) &&
                    aAttr.getDouble("Amount") >= bAmount - 0.001 &&
                    aAttr.getInt("Operation") == bOp &&
                    aAttr.getString("Slot").equals(bSlot)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static String cleanString(String str) {
        if (str == null) return "";
        return str.toLowerCase().trim()
            .replaceAll("§.", "")
            .replaceAll("[^a-zа-яё0-9\\s\\[\\]★+]", "")
            .replaceAll("\\s+", " ");
    }

    public static boolean isArmorItem(ItemStack stack) {
        return stack.getItem() == Items.NETHERITE_HELMET || stack.getItem() == Items.NETHERITE_CHESTPLATE ||
            stack.getItem() == Items.NETHERITE_LEGGINGS || stack.getItem() == Items.NETHERITE_BOOTS ||
            stack.getItem() == Items.DIAMOND_HELMET || stack.getItem() == Items.DIAMOND_CHESTPLATE ||
            stack.getItem() == Items.DIAMOND_LEGGINGS || stack.getItem() == Items.DIAMOND_BOOTS ||
            stack.getItem() == Items.IRON_HELMET || stack.getItem() == Items.IRON_CHESTPLATE ||
            stack.getItem() == Items.IRON_LEGGINGS || stack.getItem() == Items.IRON_BOOTS ||
            stack.getItem() == Items.GOLDEN_HELMET || stack.getItem() == Items.GOLDEN_CHESTPLATE ||
            stack.getItem() == Items.GOLDEN_LEGGINGS || stack.getItem() == Items.GOLDEN_BOOTS ||
            stack.getItem() == Items.CHAINMAIL_HELMET || stack.getItem() == Items.CHAINMAIL_CHESTPLATE ||
            stack.getItem() == Items.CHAINMAIL_LEGGINGS || stack.getItem() == Items.CHAINMAIL_BOOTS ||
            stack.getItem() == Items.LEATHER_HELMET || stack.getItem() == Items.LEATHER_CHESTPLATE ||
            stack.getItem() == Items.LEATHER_LEGGINGS || stack.getItem() == Items.LEATHER_BOOTS ||
            stack.getItem() == Items.TURTLE_HELMET;
    }

    public static boolean hasThornsEnchantment(ItemStack stack) {
        var enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants != null) {
            for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
                String id = entry.getIdAsString();
                if (id != null && (id.contains("thorns") || id.contains("шип"))) return true;
            }
        }
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                String s = line.getString().toLowerCase();
                if (s.contains("thorns") || s.contains("шип")) return true;
            }
        }
        return false;
    }
}
