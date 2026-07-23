package dev.ethereal.client.features.modules.other;

import dev.ethereal.api.event.events.client.KeyEvent;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BindSetting;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.utils.auction.AuctionUtil;
import dev.ethereal.api.utils.auction.ab.AutoPriceParser;
import dev.ethereal.api.utils.auction.ab.BuyableItem;
import dev.ethereal.api.utils.auction.ab.BuyableRegistry;
import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.player.InventoryActionUtil;
import dev.ethereal.api.utils.player.InventoryUtil;
import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.api.utils.rotation.manager.Rotation;
import dev.ethereal.api.utils.rotation.manager.RotationComponent;
import dev.ethereal.client.ui.autobuy.ScreenAutoBuy;
import lombok.Getter;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@ModuleRegister(name = "AutoBuy", category = Category.OTHER)
public class TaksaBuy extends Module {
    @Getter
    private static final TaksaBuy instance = new TaksaBuy();
    
    public final BindSetting gui = new BindSetting("Бинд гуи");
    private final BooleanSetting checkTotem = new BooleanSetting("Проверять тотемы").setVisible(PlayerUtil::isHW);

    private final ScreenAutoBuy screen = ScreenAutoBuy.getInstance();

    public TimerUtil timer = new TimerUtil();
    private final TimerUtil speed = new TimerUtil();
    private final TimerUtil rctTimer = new TimerUtil();
    
    private boolean confirm = false;
    private ItemStack buyItem = ItemStack.EMPTY;

    public int updCount = 0;
    private boolean fix = false;

    public TaksaBuy() {
        addSettings(gui, checkTotem);
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (event.action() != 1) return;
        if (event.key() == gui.getValue() && gui.getValue() != -1 && gui.getValue() != -999) {
            if (mc.currentScreen == null) screen.openGui(); // TODO переделать гуи
        }
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (!(event.packet() instanceof GameMessageS2CPacket packet)) return;
        String msg = packet.content().getString();

        if (msg.contains("▶ Аукцион обновлен.")) {
            event.cancel();
        }
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (updCount > 150) {
            updCount = 0;
            mc.player.networkHandler.sendChatMessage(".rct");
            rctTimer.reset();
            fix = true;
        }

        if (fix && rctTimer.finished(5000)) {
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
            InventoryActionUtil.clickSlot(45, 0, SlotActionType.PICKUP);
        }

        bypass();

        if (AuctionUtil.isSearchScreen(title)) {
            if (AutoPriceParser.isEnabled()) {
                AutoPriceParser.tickAuction(screen);
                speed.reset();
                return;
            }
            
            if (timer.finished(getDelay())) {
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
                    InventoryActionUtil.clickSlot(slot.id, 0, SlotActionType.PICKUP);
                    foundItem = true;
                    break;
                }
            }
        }

        if (!foundItem) {
            InventoryActionUtil.clickSlot(47, 0, SlotActionType.PICKUP);
            updCount++;
        }
    }
    
    private void confirmBuy(GenericContainerScreen screen) {
        Slot slot = screen.getScreenHandler().slots.get(13);
        ItemStack currentItem = slot.getStack();

        if (!currentItem.isEmpty() && isValid(currentItem, buyItem)) {
            InventoryActionUtil.clickSlot(1, 0, SlotActionType.PICKUP);
        } else {
            InventoryActionUtil.clickSlot(8, 0, SlotActionType.PICKUP);
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

        boolean totem = InventoryUtil.findItem(Items.TOTEM_OF_UNDYING) > 4 && checkTotem.getValue();
        if (totem && current.getItem() == Items.TOTEM_OF_UNDYING) {
            return false;
        }

        return currentName.equals(clickedName);
    }

    public long getDelay() {
        long elapsed = speed.getElapsedTime();

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
