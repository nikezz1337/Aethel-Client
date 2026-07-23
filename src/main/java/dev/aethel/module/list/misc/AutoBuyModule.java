package dev.aethel.module.list.misc;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventKeyInput;
import dev.aethel.event.list.EventPacket;
import dev.aethel.event.list.EventPlayerUpdate;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.list.combat.aura.rotation.Rotation;
import dev.aethel.module.list.combat.aura.rotation.RotationComponent;
import dev.aethel.module.list.combat.aura.util.time.TimerUtil;
import dev.aethel.module.settings.BindSetting;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.ui.autobuy.ScreenAutoBuy;
import dev.aethel.util.auction.AuctionUtil;
import dev.aethel.util.auction.ab.AutoPriceParser;
import dev.aethel.util.auction.ab.BuyableItem;
import dev.aethel.util.auction.ab.BuyableRegistry;
import dev.aethel.util.inventory.InventoryTask;
import dev.aethel.util.other.ServerUtil;
import dev.aethel.util.render.math.MathUtil;
import lombok.Getter;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInformation(
    moduleName = "AutoBuy",
    moduleCategory = ModuleCategory.MISC,
    moduleDesc = "Автоматическая покупка предметов на аукционе"
)
public class AutoBuyModule extends Module {
    @Getter
    private static final AutoBuyModule instance = new AutoBuyModule();

    public final BindSetting gui = new BindSetting("Бинд гуи", -1);
    private final BooleanSetting checkTotem = new BooleanSetting("Проверять тотемы", false);
    {
        checkTotem.setVisible(ServerUtil::isHW);
    }

    private final ScreenAutoBuy screen = ScreenAutoBuy.getInstance();

    public TimerUtil timer = new TimerUtil();
    private final TimerUtil speed = new TimerUtil();
    private final TimerUtil rctTimer = new TimerUtil();

    private boolean confirm = false;
    private ItemStack buyItem = ItemStack.EMPTY;

    public int updCount = 0;
    private boolean fix = false;

    @Subscribe
    public void onKey(EventKeyInput event) {
        if (event.getAction() != 1) return;
        if (event.getKey() == gui.getValue() && gui.getValue() != -1 && gui.getValue() != -999) {
            if (mc.currentScreen == null) screen.openGui();
        }
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (event.getType() != EventPacket.Type.RECEIVE) return;
        if (!(event.getPacket() instanceof GameMessageS2CPacket packet)) return;
        String msg = packet.content().getString();

        if (msg.contains("▶ Аукцион обновлен.")) {
            event.setCancelled(true);
        }
    }

    @Subscribe
    public void onUpdate(EventPlayerUpdate event) {
        if (updCount > 150) {
            updCount = 0;
            mc.player.networkHandler.sendChatMessage(".rct");
            rctTimer.reset();
            fix = true;
        }

        if (fix && rctTimer.hasReached(5000)) {
            mc.player.networkHandler.sendChatCommand("ah");
            fix = false;
            updCount = 0;
            rctTimer.reset();
        }

        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            speed.reset();
            confirm = false;
            buyItem = ItemStack.EMPTY;
            AutoPriceParser.tickNoContainer();
            return;
        }

        String title = screen.getTitle().getString();

        if (AuctionUtil.isContainerScreen(title)) {
            InventoryTask.clickSlot(45, 0, SlotActionType.PICKUP);
        }

        bypass();

        if (AuctionUtil.isSearchScreen(title)) {
            if (AutoPriceParser.isEnabled()) {
                AutoPriceParser.tickAuction(screen);
                speed.reset();
                return;
            }

            if (timer.hasReached(getDelay())) {
                checkAndBuy(screen);
                timer.reset();
            }
        }

        if (confirm && AuctionUtil.buyMenu(title)) {
            confirmBuy(screen);
        }
    }

    private void checkAndBuy(GenericContainerScreen screen) {
        boolean foundItem = false;

        for (Slot slot : screen.getScreenHandler().slots) {

            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            BuyableItem buyable = BuyableRegistry.findMatch(stack);
            if (buyable != null) {
                int price = AuctionUtil.getPrice(stack);
                if (price > 0 && price <= buyable.getMaxPrice()) {
                    buyItem = stack.copy();
                    confirm = true;
                    InventoryTask.clickSlot(slot.id, 0, SlotActionType.PICKUP);
                    foundItem = true;
                    break;
                }
            }
        }

        if (!foundItem) {
            InventoryTask.clickSlot(47, 0, SlotActionType.PICKUP);
            updCount++;
        }
    }

    private void confirmBuy(GenericContainerScreen screen) {
        Slot slot = screen.getScreenHandler().slots.get(13);
        ItemStack currentItem = slot.getStack();

        if (!currentItem.isEmpty() && isValid(currentItem, buyItem)) {
            InventoryTask.clickSlot(1, 0, SlotActionType.PICKUP);
        } else {
            InventoryTask.clickSlot(8, 0, SlotActionType.PICKUP);
        }

        confirm = false;
        buyItem = ItemStack.EMPTY;
        timer.reset();
    }

    public void bypass() {
        if (mc.player == null) return;

        float targetYaw = mc.player.getYaw() + (float) MathUtil.random(-180, 180);
        float targetPitch = (float) MathUtil.random(-89, 89);

        Rotation target = new Rotation(targetYaw, targetPitch);
        float yawSpeed = (float) MathUtil.random(31, 39);
        float pitchSpeed = (float) MathUtil.random(4, 5.2);

        RotationComponent.update(
            target,
            yawSpeed,
            pitchSpeed,
            yawSpeed,
            pitchSpeed,
            0,
            5,
            false
        );
    }

    private boolean isValid(ItemStack current, ItemStack clicked) {
        if (current.isEmpty() || clicked.isEmpty()) return false;
        if (current.getItem() != clicked.getItem()) return false;

        String currentName = current.getName().getString();
        String clickedName = clicked.getName().getString();

        int totemCount = 0;
        if (mc.player != null) {
            for (int i = 0; i < 36; i++) {
                ItemStack s = mc.player.getInventory().main.get(i);
                if (s.getItem() == Items.TOTEM_OF_UNDYING) {
                    totemCount += s.getCount();
                }
            }
        }
        boolean totem = totemCount > 4 && checkTotem.getValue();
        if (totem && current.getItem() == Items.TOTEM_OF_UNDYING) {
            return false;
        }

        return currentName.equals(clickedName);
    }

    public long getDelay() {
        long elapsed = speed.getElapsed();

        if (elapsed >= 30000) {
            speed.reset();
            elapsed = 0;
        }

        if (elapsed < 2000) {
            return (long) MathUtil.random(0f, 50f);
        } else if (elapsed < 4000) {
            return (long) MathUtil.random(250f, 300f);
        } else if (elapsed < 10000) {
            return (long) MathUtil.random(500f, 600f);
        } else if (elapsed < 13000) {
            return (long) MathUtil.random(950f, 1100f);
        } else {
            return (long) MathUtil.random(900f, 1100f);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        BuyableRegistry.init();
        timer.reset();
        speed.reset();
        rctTimer.reset();
        updCount = 0;
        fix = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        updCount = 0;
        fix = false;
    }
}
