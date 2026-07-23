//package dev.ethereal.client.features.modules.other;
//
//import lombok.Getter;
//import net.minecraft.item.ItemStack;
//import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
//import net.minecraft.text.Text;
//import dev.ethereal.api.event.EventListener;
//import dev.ethereal.api.event.Listener;
//import dev.ethereal.api.event.events.player.other.UpdateEvent;
//import dev.ethereal.api.module.Category;
//import dev.ethereal.api.module.Module;
//import dev.ethereal.api.module.ModuleRegister;
//import dev.ethereal.api.module.setting.BooleanSetting;
//import dev.ethereal.api.module.setting.SliderSetting;
//import dev.ethereal.api.utils.math.TimerUtil;
//
//@ModuleRegister(name = "AutoSell", category = Category.OTHER)
//public class AutoSellModule extends Module {
//    @Getter private static final AutoSellModule instance = new AutoSellModule();
//
//    private final StringSe itemName = new StringSetting("Предмет").value("");
//    private final StringSetting price = new StringSetting("Цена").value("");
//    private final BooleanSetting logChat = new BooleanSetting("Логи в чат").value(false);
//    private final SliderSetting sellDelay = new SliderSetting("Задержка (мс)").value(1000f).range(200f, 5000f).step(100f);
//
//    private String lastSoldKey = "";
//    private int scanIndex = 0;
//    private final TimerUtil sellTimer = new TimerUtil();
//
//    public AutoSellModule() {
//        addSettings(itemName, price, logChat, sellDelay);
//    }
//
//    @Override
//    public void onEnable() {
//        lastSoldKey = "";
//        scanIndex = 0;
//
//        if (mc.player == null) {
//            toggle();
//            return;
//        }
//
//        String itemQuery = itemName.getValue().trim().toLowerCase();
//        String pr = price.getValue().trim();
//
//        if (itemQuery.isEmpty()) {
//            sendMessage("Укажите название предмета");
//        }
//        if (pr.isEmpty()) {
//            sendMessage("Укажите цену");
//        }
//
//        sellTimer.reset();
//    }
//
//    @Override
//    public void onDisable() {
//        lastSoldKey = "";
//        scanIndex = 0;
//    }
//
//    @Override
//    public void onEvent() {
//        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
//            if (mc.player == null) return;
//
//            String itemQuery = itemName.getValue().trim().toLowerCase();
//            String pr = price.getValue().trim();
//
//            if (itemQuery.isEmpty() || pr.isEmpty()) {
//                return;
//            }
//
//            // Проверяем предмет в руке
//            ItemStack held = mc.player.getMainHandStack();
//            if (held == null || held.isEmpty()) {
//                lastSoldKey = "";
//            }
//
//            if (held != null && !held.isEmpty()) {
//                String heldKey = held.getItem().getTranslationKey().toLowerCase();
//                String heldName = held.getName().getString().toLowerCase();
//
//                if ((heldKey.contains(itemQuery) || heldName.contains(itemQuery))
//                    && !heldKey.equals(lastSoldKey)) {
//
//                    mc.player.networkHandler.sendChatMessage("/ah sell " + pr);
//                    lastSoldKey = heldKey;
//
//                    if (logChat.getValue()) {
//                        sendMessage("Предмет " + heldName + " выставлен за " + pr + " (в руке)");
//                    }
//
//                    sellTimer.reset();
//                    return;
//                }
//            }
//
//            // Проверяем задержку
//            if (!sellTimer.hasReached((long) sellDelay.getValue().intValue())) {
//                return;
//            }
//
//            sellTimer.reset();
//
//            // Сканируем инвентарь
//            int total = mc.player.getInventory().main.size();
//            int found = -1;
//            String foundKey = null;
//            String foundName = null;
//
//            for (int i = 0; i < total; i++) {
//                int idx = (scanIndex + i) % total;
//                ItemStack stack = mc.player.getInventory().main.get(idx);
//
//                if (stack == null || stack.isEmpty()) continue;
//
//                String key = stack.getItem().getTranslationKey().toLowerCase();
//                String name = stack.getName().getString().toLowerCase();
//
//                if (key.contains(itemQuery) || name.contains(itemQuery)) {
//                    found = idx;
//                    foundKey = key;
//                    foundName = name;
//                    break;
//                }
//            }
//
//            if (found != -1 && foundKey != null) {
//                ItemStack heldNow = mc.player.getMainHandStack();
//
//                if (heldNow != null && !heldNow.isEmpty()) {
//                    String heldKey = heldNow.getItem().getTranslationKey().toLowerCase();
//                    if (heldKey.equals(lastSoldKey) && heldKey.equals(foundKey)) {
//                        scanIndex = (found + 1) % total;
//                        return;
//                    }
//                }
//
//                // Переключаемся на слот
//                if (found < 9) {
//                    mc.player.getInventory().selectedSlot = found;
//                    if (logChat.getValue()) {
//                        sendMessage("Переключаюсь на слот " + found + " (хотбар)");
//                    }
//                } else {
//                    // Перемещаем в хотбар
//                    mc.interactionManager.clickSlot(
//                        mc.player.currentScreenHandler.syncId,
//                        found,
//                        0,
//                        net.minecraft.screen.slot.SlotActionType.PICKUP,
//                        mc.player
//                    );
//                    mc.interactionManager.clickSlot(
//                        mc.player.currentScreenHandler.syncId,
//                        0,
//                        0,
//                        net.minecraft.screen.slot.SlotActionType.PICKUP,
//                        mc.player
//                    );
//
//                    mc.player.getInventory().selectedSlot = 0;
//
//                    if (logChat.getValue()) {
//                        sendMessage("Перемещаю предмет из слота " + found + " в хотбар");
//                    }
//                }
//
//                scanIndex = (found + 1) % total;
//                return;
//            }
//
//            scanIndex = (scanIndex + 1) % total;
//        }));
//
//        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
//            if (event.getType() != PacketEvent.Type.RECEIVE) return;
//
//            if (event.getPacket() instanceof GameMessageS2CPacket packet) {
//                Text message = packet.content();
//                String msg = message.getString().toLowerCase();
//
//                // Убираем форматирование
//                msg = msg.replaceAll("§[0-9a-fk-or]", "");
//
//                if (msg.contains("освободите хранилище") || msg.contains("уберите предметы с продажи")) {
//                    sendMessage("§c[AutoSell] Отключаюсь: " + msg);
//                    toggle();
//                } else if (msg.contains("используете команду слишком часто") || msg.contains("слишком часто")) {
//                    sendMessage("§e[AutoSell] Слишком часто, попробую ещё раз");
//                    lastSoldKey = "";
//                    sellTimer.reset();
//                }
//            }
//        }));
//
//        addEvents(updateEvent, packetEvent);
//    }
//
//    private void sendMessage(String text) {
//        if (mc.player != null) {
//            mc.player.sendMessage(Text.literal(text));
//        }
//    }
//}
