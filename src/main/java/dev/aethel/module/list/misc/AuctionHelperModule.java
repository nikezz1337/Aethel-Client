package dev.aethel.module.list.misc;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventKeyInput;
import dev.aethel.event.list.EventPlayerUpdate;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.list.combat.aura.util.time.TimerUtil;
import dev.aethel.module.settings.BindSetting;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.MultiBooleanSetting;
import dev.aethel.module.settings.SliderSetting;
import dev.aethel.util.auction.AuctionUtil;
import dev.aethel.util.inventory.InventoryTask;
import dev.aethel.util.other.ServerUtil;
import dev.aethel.util.render.renderers.DrawUtil;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleInformation(
    moduleName = "Auction Helper",
    moduleCategory = ModuleCategory.MISC,
    moduleDesc = "Помощник аукциона"
)
public class AuctionHelperModule extends Module {
    @Getter private static final AuctionHelperModule instance = new AuctionHelperModule();

    private final SliderSetting slots = new SliderSetting("Slots", 3, 1, 6, 1);

    private final MultiBooleanSetting utilities = new MultiBooleanSetting("Utilities", "Утилиты",
            new BooleanSetting("Fast Buy", true),
            new BooleanSetting("Auto Confirm", true)
    );

    private final BindSetting keySell = new BindSetting("Sell Key", -1);
    {
        keySell.setVisible(ServerUtil::isHW);
    }

    private final MultiBooleanSetting filterItems = new MultiBooleanSetting("Filter Items", "Фильтры",
            new BooleanSetting("Check Durability", false),
            new BooleanSetting("Only Enchanted", false),
            new BooleanSetting("Armor Protection", false),
            new BooleanSetting("Armor Unbreaking", false),
            new BooleanSetting("Armor Mending", false),
            new BooleanSetting("No Thorns", false),
            new BooleanSetting("Sword Sharpness", false),
            new BooleanSetting("Sword Unbreaking", false),
            new BooleanSetting("Sword Mending", false),
            new BooleanSetting("Potion Strength", false),
            new BooleanSetting("Potion Speed", false),
            new BooleanSetting("Potion Heal", false)
    );

    private final SliderSetting minDurability = new SliderSetting("Min Durability", 100, 1, 100, 1);
    {
        minDurability.setVisible(() -> filterItems.isSelected("Check Durability"));
    }

    private final SliderSetting protectionLevel = new SliderSetting("Protection Level", 5, 1, 5, 1);
    {
        protectionLevel.setVisible(() -> filterItems.isSelected("Armor Protection"));
    }
    private final SliderSetting armorUnbreakingLevel = new SliderSetting("Armor Unbreaking Lvl", 5, 1, 5, 1);
    {
        armorUnbreakingLevel.setVisible(() -> filterItems.isSelected("Armor Unbreaking"));
    }
    private final SliderSetting sharpnessLevel = new SliderSetting("Sharpness Level", 8, 1, 7, 1);
    {
        sharpnessLevel.setVisible(() -> filterItems.isSelected("Sword Sharpness"));
    }
    private final SliderSetting swordUnbreakingLevel = new SliderSetting("Sword Unbreaking Lvl", 5, 1, 5, 1);
    {
        swordUnbreakingLevel.setVisible(() -> filterItems.isSelected("Sword Unbreaking"));
    }
    private final SliderSetting strengthLevel = new SliderSetting("Strength Level", 3, 1, 3, 1);
    {
        strengthLevel.setVisible(() -> filterItems.isSelected("Potion Strength"));
    }
    private final SliderSetting speedLevel = new SliderSetting("Speed Level", 3, 1, 3, 1);
    {
        speedLevel.setVisible(() -> filterItems.isSelected("Potion Speed"));
    }
    private final SliderSetting healLevel = new SliderSetting("Heal Level", 2, 1, 2, 1);
    {
        healLevel.setVisible(() -> filterItems.isSelected("Potion Heal"));
    }

    private final List<Slot> minPriceSlots = new ArrayList<>();
    private final TimerUtil timer = new TimerUtil();
    private boolean waitConf = false;

    @Subscribe
    public void onUpdate(EventPlayerUpdate event) {
        handleUpdateEvent();
        fastBuy();
    }

    @Subscribe
    public void onKey(EventKeyInput event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getAction() != 1) return;
        if (event.getKey() == keySell.getValue() && keySell.getValue() != -1 && mc.currentScreen == null) {
            mc.player.networkHandler.sendChatCommand("ah sell auto");
            waitConf = true;
        }
    }

    private void fastBuy() {
        if (mc.player == null || mc.world == null) return;

        if (waitConf) {
            mc.player.networkHandler.sendChatCommand("ah sell auto confirm");
            waitConf = false;
        }

        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;

        String title = screen.getTitle().getString();
        boolean buy = timer.hasReached(50L) && utilities.isSelected("Fast Buy");
        if (AuctionUtil.buyMenu(title) && buy && !AutoBuyModule.getInstance().isEnabled()) {
            InventoryTask.clickSlot(1, 0, SlotActionType.PICKUP);
            timer.reset();
        }
    }

    public void handleUpdateEvent() {
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler chest) {
            String title = mc.currentScreen.getTitle().getString();
            if (!AuctionUtil.isSearchScreen(title)) return;

            minPriceSlots.clear();
            minPriceSlots.addAll(getMinPriceSlots(chest));
        }
    }

    private List<Slot> getMinPriceSlots(GenericContainerScreenHandler chest) {
        return chest.slots.stream()
                .filter(s -> s.id <= 44 && !s.getStack().isEmpty() && getPrice(s.getStack()) != -1 && isValid(s.getStack()))
                .sorted((s1, s2) -> Integer.compare(getPrice(s1.getStack()), getPrice(s2.getStack())))
                .limit(slots.getIntValue())
                .toList();
    }

    private int getPrice(ItemStack stack) {
        return AuctionUtil.getPrice(stack);
    }

    public void onRenderChest(DrawContext context, Slot slot) {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) return;
        String title = mc.currentScreen.getTitle().getString();
        if (!AuctionUtil.isSearchScreen(title) && !AuctionUtil.buyMenu(title)) return;
        if (minPriceSlots.isEmpty()) return;

        if (minPriceSlots.contains(slot)) {
            int alpha = (int)(1 + 110 * Math.abs(Math.sin(System.currentTimeMillis() * 0.005)));
            DrawUtil.drawRound(slot.x, slot.y, 16, 16, 0, new Color(0, 255, 0, alpha).getRGB());
        }
    }

    private boolean isArmorOrElytra(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.ELYTRA) return true;
        return item instanceof ArmorItem;
    }

    private int getDurabilityPercent(ItemStack stack) {
        if (!stack.isDamageable()) return 100;
        int maxDamage = stack.getMaxDamage();
        if (maxDamage == 0) return 100;
        int currentDamage = stack.getDamage();
        int remaining = maxDamage - currentDamage;
        return (int) ((remaining * 100.0) / maxDamage);
    }

    public boolean isValid(ItemStack stack) {
        if (stack.isEmpty()) return false;

        if (ServerUtil.isFT() && stack.getItem() == Items.GRAY_DYE) return false;

        if (filterItems.isSelected("Check Durability")) {
            if (isArmorOrElytra(stack)) {
                int durabilityPercent = getDurabilityPercent(stack);
                if (durabilityPercent < minDurability.getIntValue()) {
                    return false;
                }
            } else {
                if (stack.isDamageable() && stack.getDamage() != 0) {
                    return false;
                }
            }
        }

        if (filterItems.isSelected("Only Enchanted")) {
            ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
            if (enchants == null || enchants.isEmpty()) {
                return false;
            }
        }

        return checkFilters(stack);
    }

    private boolean checkFilters(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof ArmorItem) {
            return checkArmorFilters(stack);
        }

        if (item instanceof SwordItem) {
            return checkSwordFilters(stack);
        }

        if (item instanceof PotionItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
            return checkPotionFilters(stack);
        }

        return true;
    }

    private boolean checkArmorFilters(ItemStack stack) {
        if (!filterItems.isSelected("Armor Protection") && !filterItems.isSelected("Armor Unbreaking") &&
                !filterItems.isSelected("Armor Mending") && !filterItems.isSelected("No Thorns")) {
            return true;
        }

        ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants == null) enchants = ItemEnchantmentsComponent.DEFAULT;

        var registry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        int protLvl = registry.getOptional(Enchantments.PROTECTION).map(enchants::getLevel).orElse(0);
        int unbreakLvl = registry.getOptional(Enchantments.UNBREAKING).map(enchants::getLevel).orElse(0);
        boolean hasMending = registry.getOptional(Enchantments.MENDING).map(enchants::getLevel).orElse(0) > 0;
        boolean hasThorns = registry.getOptional(Enchantments.THORNS).map(enchants::getLevel).orElse(0) > 0;

        if (filterItems.isSelected("Armor Protection") && protLvl < protectionLevel.getIntValue()) return false;
        if (filterItems.isSelected("Armor Unbreaking") && unbreakLvl < armorUnbreakingLevel.getIntValue()) return false;
        if (filterItems.isSelected("Armor Mending") && !hasMending) return false;
        if (filterItems.isSelected("No Thorns") && hasThorns) return false;

        return true;
    }

    private boolean checkSwordFilters(ItemStack stack) {
        if (!filterItems.isSelected("Sword Sharpness") && !filterItems.isSelected("Sword Unbreaking") &&
                !filterItems.isSelected("Sword Mending")) {
            return true;
        }

        ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants == null) enchants = ItemEnchantmentsComponent.DEFAULT;

        var registry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        int sharpLvl = registry.getOptional(Enchantments.SHARPNESS).map(enchants::getLevel).orElse(0);
        int unbreakLvl = registry.getOptional(Enchantments.UNBREAKING).map(enchants::getLevel).orElse(0);
        boolean hasMending = registry.getOptional(Enchantments.MENDING).map(enchants::getLevel).orElse(0) > 0;

        if (filterItems.isSelected("Sword Sharpness") && sharpLvl < sharpnessLevel.getIntValue()) return false;
        if (filterItems.isSelected("Sword Unbreaking") && unbreakLvl < swordUnbreakingLevel.getIntValue()) return false;
        if (filterItems.isSelected("Sword Mending") && !hasMending) return false;

        return true;
    }

    private boolean checkPotionFilters(ItemStack stack) {
        if (!filterItems.isSelected("Potion Strength") && !filterItems.isSelected("Potion Speed") &&
                !filterItems.isSelected("Potion Heal")) {
            return true;
        }

        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents == null) return true;

        int strengthLvl = 0;
        int speedLvl = 0;
        int healLvl = 0;

        for (var effect : contents.getEffects()) {
            if (effect.getEffectType().value() == StatusEffects.STRENGTH.value()) {
                strengthLvl = effect.getAmplifier() + 1;
            }
            if (effect.getEffectType().value() == StatusEffects.SPEED.value()) {
                speedLvl = effect.getAmplifier() + 1;
            }
            if (effect.getEffectType().value() == StatusEffects.INSTANT_HEALTH.value()) {
                healLvl = effect.getAmplifier() + 1;
            }
        }

        if (filterItems.isSelected("Potion Strength") && strengthLvl >= strengthLevel.getIntValue()) return true;
        if (filterItems.isSelected("Potion Speed") && speedLvl >= speedLevel.getIntValue()) return true;
        if (filterItems.isSelected("Potion Heal") && healLvl >= healLevel.getIntValue()) return true;

        return false;
    }
}
