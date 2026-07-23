package dev.ethereal.client.features.modules.other;

import dev.ethereal.api.event.events.player.world.ClickSlotEvent;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.KeyEvent;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BindSetting;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.utils.auction.AuctionUtil;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.player.InventoryActionUtil;
import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;


import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Auction Helper", category = Category.OTHER)
public class AuctionHelperModule extends Module {
    @Getter private static final AuctionHelperModule instance = new AuctionHelperModule();

    private final SliderSetting slots = new SliderSetting("Slots").value(3f).range(1f, 6f).step(1f);

    private final MultiBooleanSetting utilities = new MultiBooleanSetting("Utilities").value(
            new BooleanSetting("Fast Buy").value(true),
            new BooleanSetting("Auto Confirm").value(true)
    );

    private final BindSetting keySell = new BindSetting("Sell Key").value(-1).setVisible(PlayerUtil::isHW);

    private final MultiBooleanSetting filterItems = new MultiBooleanSetting("Filter Items").value(
            new BooleanSetting("Check Durability").value(false),
            new BooleanSetting("Only Enchanted").value(false),
            new BooleanSetting("Armor Protection").value(false),
            new BooleanSetting("Armor Unbreaking").value(false),
            new BooleanSetting("Armor Mending").value(false),
            new BooleanSetting("No Thorns").value(false),
            new BooleanSetting("Sword Sharpness").value(false),
            new BooleanSetting("Sword Unbreaking").value(false),
            new BooleanSetting("Sword Mending").value(false),
            new BooleanSetting("Potion Strength").value(false),
            new BooleanSetting("Potion Speed").value(false),
            new BooleanSetting("Potion Heal").value(false)
    );

    private final SliderSetting minDurability = new SliderSetting("Min Durability").value(100f).range(1f, 100f).step(1f)
            .setVisible(() -> filterItems.isEnabled("Check Durability"));

    private final SliderSetting protectionLevel = new SliderSetting("Protection Level").value(5f).range(1f, 5f).step(1f)
            .setVisible(() -> filterItems.isEnabled("Armor Protection"));
    private final SliderSetting armorUnbreakingLevel = new SliderSetting("Armor Unbreaking Lvl").value(5f).range(1f, 5f).step(1f)
            .setVisible(() -> filterItems.isEnabled("Armor Unbreaking"));
    private final SliderSetting sharpnessLevel = new SliderSetting("Sharpness Level").value(8f).range(1f, 7f).step(1f)
            .setVisible(() -> filterItems.isEnabled("Sword Sharpness"));
    private final SliderSetting swordUnbreakingLevel = new SliderSetting("Sword Unbreaking Lvl").value(5f).range(1f, 5f).step(1f)
            .setVisible(() -> filterItems.isEnabled("Sword Unbreaking"));
    private final SliderSetting strengthLevel = new SliderSetting("Strength Level").value(3f).range(1f, 3f).step(1f)
            .setVisible(() -> filterItems.isEnabled("Potion Strength"));
    private final SliderSetting speedLevel = new SliderSetting("Speed Level").value(3f).range(1f, 3f).step(1f)
            .setVisible(() -> filterItems.isEnabled("Potion Speed"));
    private final SliderSetting healLevel = new SliderSetting("Heal Level").value(2f).range(1f, 2f).step(1f)
            .setVisible(() -> filterItems.isEnabled("Potion Heal"));

    private final List<Slot> minPriceSlots = new ArrayList<>();
    private final TimerUtil timer = new TimerUtil();
    private boolean waitConf = false;

    public AuctionHelperModule() {
        addSettings(slots, utilities, keySell, filterItems, minDurability,
                protectionLevel, armorUnbreakingLevel, sharpnessLevel, swordUnbreakingLevel,
                strengthLevel, speedLevel, healLevel);
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        handleUpdateEvent();
        fastBuy();
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (event.action() != 1) return;
        if (event.key() == keySell.getValue() && keySell.getValue() != - 1 && mc.currentScreen == null) {
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
        boolean buy = timer.finished(50L) && utilities.isEnabled("Fast Buy");
        if (AuctionUtil.buyMenu(title) && buy && !TaksaBuy.getInstance().isEnabled()) {
            InventoryActionUtil.clickSlot(1, 0, SlotActionType.PICKUP);
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
                .limit(slots.getValue().intValue())
                .toList();
    }

    private int getPrice(ItemStack stack) {
        return AuctionUtil.getPrice(stack);
    }

    public void onRenderChest(DrawContext context, Slot slot) {
        if (minPriceSlots.contains(slot)) {
            int alpha = (int)(1 + 110 * Math.abs(Math.sin(System.currentTimeMillis() * 0.005)));
            RenderUtil.RECT.draw(context.getMatrices(), slot.x, slot.y, 16, 16, 0, new Color(0, 255, 0, alpha));
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

        if (PlayerUtil.isFT() && stack.getItem() == Items.GRAY_DYE) return false;

        if (filterItems.isEnabled("Check Durability")) {
            if (isArmorOrElytra(stack)) {
                int durabilityPercent = getDurabilityPercent(stack);
                if (durabilityPercent < minDurability.getValue().intValue()) {
                    return false;
                }
            } else {
                if (stack.isDamageable() && stack.getDamage() != 0) {
                    return false;
                }
            }
        }

        if (filterItems.isEnabled("Only Enchanted")) {
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
        if (!filterItems.isEnabled("Armor Protection") && !filterItems.isEnabled("Armor Unbreaking") &&
                !filterItems.isEnabled("Armor Mending") && !filterItems.isEnabled("No Thorns")) {
            return true;
        }

        ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants == null) enchants = ItemEnchantmentsComponent.DEFAULT;

        var registry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        int protLvl = registry.getOptional(Enchantments.PROTECTION).map(enchants::getLevel).orElse(0);
        int unbreakLvl = registry.getOptional(Enchantments.UNBREAKING).map(enchants::getLevel).orElse(0);
        boolean hasMending = registry.getOptional(Enchantments.MENDING).map(enchants::getLevel).orElse(0) > 0;
        boolean hasThorns = registry.getOptional(Enchantments.THORNS).map(enchants::getLevel).orElse(0) > 0;

        if (filterItems.isEnabled("Armor Protection") && protLvl < protectionLevel.getValue().intValue()) return false;
        if (filterItems.isEnabled("Armor Unbreaking") && unbreakLvl < armorUnbreakingLevel.getValue().intValue()) return false;
        if (filterItems.isEnabled("Armor Mending") && !hasMending) return false;
        if (filterItems.isEnabled("No Thorns") && hasThorns) return false;

        return true;
    }

    private boolean checkSwordFilters(ItemStack stack) {
        if (!filterItems.isEnabled("Sword Sharpness") && !filterItems.isEnabled("Sword Unbreaking") &&
                !filterItems.isEnabled("Sword Mending")) {
            return true;
        }

        ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants == null) enchants = ItemEnchantmentsComponent.DEFAULT;

        var registry = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        int sharpLvl = registry.getOptional(Enchantments.SHARPNESS).map(enchants::getLevel).orElse(0);
        int unbreakLvl = registry.getOptional(Enchantments.UNBREAKING).map(enchants::getLevel).orElse(0);
        boolean hasMending = registry.getOptional(Enchantments.MENDING).map(enchants::getLevel).orElse(0) > 0;

        if (filterItems.isEnabled("Sword Sharpness") && sharpLvl < sharpnessLevel.getValue().intValue()) return false;
        if (filterItems.isEnabled("Sword Unbreaking") && unbreakLvl < swordUnbreakingLevel.getValue().intValue()) return false;
        if (filterItems.isEnabled("Sword Mending") && !hasMending) return false;

        return true;
    }

    private boolean checkPotionFilters(ItemStack stack) {
        if (!filterItems.isEnabled("Potion Strength") && !filterItems.isEnabled("Potion Speed") &&
                !filterItems.isEnabled("Potion Heal")) {
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

        if (filterItems.isEnabled("Potion Strength") && strengthLvl >= strengthLevel.getValue().intValue()) return true;
        if (filterItems.isEnabled("Potion Speed") && speedLvl >= speedLevel.getValue().intValue()) return true;
        if (filterItems.isEnabled("Potion Heal") && healLvl >= healLevel.getValue().intValue()) return true;

        return false;
    }
}
