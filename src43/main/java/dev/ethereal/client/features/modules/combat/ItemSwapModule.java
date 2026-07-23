package dev.ethereal.client.features.modules.combat;

import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.events.client.KeyEvent;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BindSetting;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.utils.other.SlownessManager;
import dev.ethereal.api.utils.other.StopWatch;
import dev.ethereal.api.utils.player.InvUtil;
import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.client.features.modules.movement.InventoryMoveModule;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

@ModuleRegister(
        name = "AutoSwap",
        category = Category.COMBAT,
        bind = -1
)
@Environment(EnvType.CLIENT)
public class ItemSwapModule extends Module {
    @Getter
    private static final ItemSwapModule instance = new ItemSwapModule();
    
    private final ModeSetting firstItemSetting = new ModeSetting("Первый предмет").value("Шар").values("Золотое яблоко", "Щит", "Шар", "Тотем");
    private final ModeSetting secondItemSetting = new ModeSetting("Второй предмет").value("Тотем 2").values("Золотое яблоко 2", "Щит 2", "Шар 2", "Тотем 2");
    private final BindSetting bind = new BindSetting("Бинд");
    private final BooleanSetting swaprender = new BooleanSetting("Писать о свапе").value(true);
    private final BooleanSetting onlyEnchanted = new BooleanSetting("Только Чар. тотемы").value(false);

    private boolean swap;
    private boolean hand;
    private final StopWatch swapWatch = new StopWatch();
    private boolean bypassActive;
    private boolean bypassSwapped;
    private int bypassSlot = -1;
    private String bypassItemName = "";

    public ItemSwapModule() {
        addSettings(firstItemSetting, secondItemSetting, bind, swaprender, onlyEnchanted);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            update();
        }));

        EventListener keyEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            input(event);
        }));

        addEvents(tickEvent, keyEvent);
    }

    private void update() {
        ScreenHandler screenHandler = mc.player.currentScreenHandler;
        if (this.bypassActive) {
            if (!this.bypassSwapped && this.bypassSlot != -1) {
                mc.interactionManager
                        .clickSlot(screenHandler.syncId, this.bypassSlot < 9 ? this.bypassSlot + 36 : this.bypassSlot, 40, SlotActionType.SWAP, mc.player);
                mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(screenHandler.syncId));
//                ItemStack item = mc.player.getOffHandStack();
                if (swaprender.getValue() && mc.player != null) {
                         print("свапнул на " + this.bypassItemName, true);
                }

                this.bypassSwapped = true;
            }

            if (this.swapWatch.hasTimeElapsed(150L)) {
                this.bypassActive = false;
                this.bypassSwapped = false;
                this.bypassSlot = -1;
            }
        } else {
            if (this.swap && this.hand) {
                if (firstItemSetting.getValue().equals("Шар")) {
                    this.swap(Items.PLAYER_HEAD, "Шар", false);
                } else if (firstItemSetting.getValue().equals("Тотем")) {
                    this.swap(Items.TOTEM_OF_UNDYING, "Тотем", onlyEnchanted.getValue());
                } else if (firstItemSetting.getValue().equals("Золотое яблоко")) {
                    this.swap(Items.GOLDEN_APPLE, "Золотое яблоко", false);
                } else if (firstItemSetting.getValue().equals("Щит")) {
                    this.swap(Items.SHIELD, "Щит", false);
                }

                this.hand = false;
            }

            if (this.swap) {
                if (secondItemSetting.getValue().equals("Шар 2")) {
                    this.swap(Items.PLAYER_HEAD, "Шар", false);
                } else if (secondItemSetting.getValue().equals("Золотое яблоко 2")) {
                    this.swap(Items.GOLDEN_APPLE, "Золотое яблоко", false);
                } else if (secondItemSetting.getValue().equals("Тотем 2")) {
                    this.swap(Items.TOTEM_OF_UNDYING, "Тотем", onlyEnchanted.getValue());
                } else if (secondItemSetting.getValue().equals("Щит 2")) {
                    this.swap(Items.SHIELD, "Щит", false);
                }

                this.hand = true;
            }
        }
    }

    private void input(KeyEvent.KeyEventData event) {
        if (mc.currentScreen == null && event.action() == 1) { // 1 = нажатие, 0 = отжатие
            if (event.key() == bind.getValue()) {
                this.swap = true;
            }
        }
    }

    private void swap(Item item, String itemName, boolean onlyEnchanted) {
        int slot = item == Items.TOTEM_OF_UNDYING ? InvUtil.find(item, false, onlyEnchanted) : InvUtil.find(item);
        if (slot != -1) {
            if (SlownessManager.isEnabled()) {
                long delay = PlayerUtil.isST() ? 70L : 1L;
                SlownessManager.applySlowness(delay, () -> {
                });
            }
            this.bypassActive = true;
            this.bypassSwapped = false;
            this.bypassSlot = slot;
            this.bypassItemName = itemName;
            this.swapWatch.reset();
        }

        this.swap = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
