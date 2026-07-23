package dev.ethereal.client.features.modules.other;

import lombok.Getter;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.client.KeyEvent;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BindSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.client.features.modules.other.autobuy.AutoBuyManager;
import dev.ethereal.client.features.modules.other.autobuy.AutoBuyableItem;
import dev.ethereal.client.features.modules.other.autobuy.FunTimeAuctionHandler;
import dev.ethereal.client.features.modules.other.autobuy.FunTimePriceParser;
import dev.ethereal.client.features.modules.other.autobuy.FunTimeProvider;
import dev.ethereal.client.features.modules.other.autobuy.HolyTimeProvider;
import dev.ethereal.client.features.modules.other.autobuy.SpookyTimeProvider;
import dev.ethereal.client.ui.autobuy.ScreenAutoBuy;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleRegister(name = "AutoBuy", category = Category.OTHER)
public class AutoBuyModule extends Module {
    private static final AutoBuyModule instance = new AutoBuyModule();

    private final BindSetting guiBind = new BindSetting("GUI Key");
    @Getter private final ModeSetting mode = new ModeSetting("Режим").value("FunTime").values("FunTime", "HolyTime", "SpookyTime");

    private final AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();
    private final FunTimeAuctionHandler auctionHandler = new FunTimeAuctionHandler();
    private final ScreenAutoBuy screen = ScreenAutoBuy.getInstance();
    private final Pattern purchasePattern = Pattern.compile("Вы успешно купили (.+?) за \\$([\\d,]+)!");

    private final TimerUtil switchTimer = new TimerUtil();
    private final TimerUtil enterDelayTimer = new TimerUtil();
    private final TimerUtil ahSpamTimer = new TimerUtil();
    private final TimerUtil hubCheckTimer = new TimerUtil();
    private final TimerUtil serverSwitchCooldown = new TimerUtil();
    private final TimerUtil updateTimer = new TimerUtil();

    private boolean open = false;
    private boolean justEntered = false;
    private boolean spammingAh = false;
    private boolean waitingForServerLoad = false;
    private boolean inHub = false;
    private int currentServerIndex = 0;
    private long serverSwitchTime = 0;
    private List<AutoBuyableItem> cachedEnabledItems = new ArrayList<>();

    private final List<PurchaseRecord> purchaseHistory = new ArrayList<>();

    public record PurchaseRecord(String itemName, int price) {}

    private static final List<String> ANARCHY_SERVERS = new ArrayList<>();

    static {
        ANARCHY_SERVERS.addAll(List.of("an102", "an103", "an104", "an105", "an106", "an107"));
        for (int i = 203; i <= 221; i++) ANARCHY_SERVERS.add("an" + i);
        for (int i = 302; i <= 313; i++) ANARCHY_SERVERS.add("an" + i);
        ANARCHY_SERVERS.addAll(List.of("an502", "an503", "an504", "an505", "an506", "an507", "an602"));
    }

    public AutoBuyModule() {
        addSettings(guiBind, mode);
    }

    private boolean isFunTime() { return mode.is("FunTime"); }
    private boolean isSpookyTime() { return mode.is("SpookyTime"); }

    public static AutoBuyModule getInstance() { return instance; }

    @Override
    public void onEvent() {
        EventListener keyEvent = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.action() != 1) return;
            if (event.key() == guiBind.getValue() && guiBind.getValue() != -1 && guiBind.getValue() != -999) {
                if (mc.currentScreen == null) screen.openGui();
            }
        }));

        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isReceive()) return;
            if (!(event.packet() instanceof GameMessageS2CPacket packet)) return;
            String message = packet.content().getString();

            if (message.contains("Вы уже подключены к этому серверу!")) {
                switchToNextServer();
                return;
            }

            if (message.contains("не хватает Монет")) {
                FunTimeAuctionHandler.setNoMoneyCooldown();
                return;
            }

            if (autoBuyManager.isEnabled()) {
                Matcher matcher = purchasePattern.matcher(message);
                if (matcher.find()) {
                    String name = matcher.group(1);
                    String priceStr = matcher.group(2).replace(",", "").replace(".", "");
                    try {
                        int price = Integer.parseInt(priceStr);
                        purchaseHistory.add(0, new PurchaseRecord(name, price));
                        if (purchaseHistory.size() > 20) purchaseHistory.remove(purchaseHistory.size() - 1);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;

            boolean funTime = isFunTime();
            if (funTime) {
                handleBypass();
                handleAhSpam();
                FunTimePriceParser.tickNoContainer();
            }

            if (mc.currentScreen instanceof GenericContainerScreen screen) {
                String title = screen.getTitle().getString();
                int syncId = screen.getScreenHandler().syncId;

                if (title.contains("Аукцион") || title.contains("Поиск") || title.contains("Аукционы")) {
                    if (FunTimePriceParser.isEnabled()) {
                        FunTimePriceParser.tickAuction(screen.getScreenHandler().slots);
                        return;
                    }
                    if (!open) {
                        open = true;
                        auctionHandler.clear();
                        updateCachedItems();
                    }
                    if (!autoBuyManager.isEnabled()) return;
                    long updateInterval = funTime ? 0 : 100;
                    if (updateInterval == 0 || updateTimer.finished(updateInterval)) {
                        if (updateInterval > 0) updateTimer.reset();
                        auctionHandler.tick(syncId, screen.getScreenHandler().slots, cachedEnabledItems);
                    }
                } else if (title.contains("Подозрительная цена")) {
                    handlePidorasPioner(syncId, screen.getScreenHandler().slots);
                } else {
                    exitAuction();
                }
            } else {
                exitAuction();
            }
        }));

        EventListener renderEvent = Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!autoBuyManager.isEnabled()) return;
            if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;
            String title = screen.getTitle().getString();
            if (!title.contains("Аукцион") && !title.contains("Аукционы") && !title.contains("Поиск")) return;
            DrawContext ctx = event.context();
            int sw = mc.getWindow().getScaledWidth();
            int sh = mc.getWindow().getScaledHeight();
            int x = 4;
            int y = sh / 2 - purchaseHistory.size() * 10 / 2;
            ctx.fill(x, y - 4, x + 180, y + purchaseHistory.size() * 10 + 4, 0x80000000);
            for (int i = 0; i < purchaseHistory.size(); i++) {
                PurchaseRecord r = purchaseHistory.get(i);
                String text = "§7" + (i + 1) + ". §f" + r.itemName + " §a$" + formatPrice(r.price);
                ctx.drawText(mc.textRenderer, text, x + 4, y + i * 10, 0xFFFFFFFF, true);
            }
        }));

        addEvents(keyEvent, packetEvent, updateEvent, renderEvent);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        auctionHandler.clear();
        cachedEnabledItems.clear();
        open = false;
        if (isFunTime()) {
            justEntered = false;
            spammingAh = false;
            waitingForServerLoad = false;
            switchTimer.reset();
            enterDelayTimer.reset();
            ahSpamTimer.reset();
            hubCheckTimer.reset();
            serverSwitchCooldown.reset();
        }
    }

    private void exitAuction() {
        if (open) {
            open = false;
            auctionHandler.clear();
        }
    }

    private void updateCachedItems() {
        boolean fun = isFunTime();
        boolean st = isSpookyTime();
        auctionHandler.setRefreshInterval(st ? 250 : fun ? 350 : 250);
        auctionHandler.setBuyDelay(st ? 20 : fun ? 0 : 20);
        updateTimer.reset();

        cachedEnabledItems.clear();
        List<AutoBuyableItem> source;
        if (fun) source = FunTimeProvider.getItems();
        else if (st) source = SpookyTimeProvider.getItems();
        else source = HolyTimeProvider.getItems();
        for (AutoBuyableItem item : source) {
            if (item != null && item.isEnabled()) {
                cachedEnabledItems.add(item);
            }
        }
    }

    private void handlePidorasPioner(int syncId, List<Slot> slots) {
        for (Slot slot : slots) {
            if (!slot.getStack().isEmpty() && slot.getStack().getItem() == Items.GREEN_STAINED_GLASS_PANE) {
                mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                return;
            }
        }
    }

    private void handleBypass() {
        if (!isFunTime() || !autoBuyManager.isEnabled()) return;
        if (mc.world == null) return;

        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        boolean wasInHub = inHub;
        inHub = objective == null || !objective.getDisplayName().getString().contains("Анархия-");

        if (wasInHub && !inHub) {
            justEntered = true;
            enterDelayTimer.reset();
            switchTimer.reset();
            waitingForServerLoad = false;
        } else if (!inHub && !wasInHub && !justEntered && !open && !waitingForServerLoad) {
            justEntered = true;
            enterDelayTimer.reset();
            switchTimer.reset();
        }

        if (inHub && hubCheckTimer.finished(3000)) {
            joinAnarchy();
        }

        if (!inHub && switchTimer.finished(60000)) {
            switchToNextServer();
        }
    }

    private void handleAhSpam() {
        if (!isFunTime() || !autoBuyManager.isEnabled()) return;
        if (open) {
            spammingAh = false;
            return;
        }

        if (justEntered && enterDelayTimer.finished(2000)) {
            if (!spammingAh) {
                spammingAh = true;
                ahSpamTimer.reset();
            }
        }

        if (spammingAh && ahSpamTimer.finished(1250)) {
            if (mc.player != null && mc.player.networkHandler != null) {
                mc.player.networkHandler.sendChatCommand("ah");
            }
            ahSpamTimer.reset();
        }
    }

    private void joinAnarchy() {
        if (ANARCHY_SERVERS.isEmpty()) return;
        String server = ANARCHY_SERVERS.get(0);
        if (mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.sendChatCommand(server);
            waitingForServerLoad = true;
            serverSwitchTime = System.currentTimeMillis();
            hubCheckTimer.reset();
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

    private void switchToNextServer() {
        if (!serverSwitchCooldown.finished(3000)) return;

        try {
            Scoreboard scoreboard = mc.world.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (objective != null) {
                String displayName = objective.getDisplayName().getString();
                if (displayName.contains("Анархия-")) {
                    String[] parts = displayName.split("-");
                    if (parts.length > 1) {
                        int currentAn = Integer.parseInt(parts[1].trim());
                        String currentCmd = "an" + currentAn;
                        int idx = ANARCHY_SERVERS.indexOf(currentCmd);
                        if (idx != -1) currentServerIndex = idx;
                    }
                }
            }
        } catch (Exception ignored) {}

        currentServerIndex = (currentServerIndex + 1) % ANARCHY_SERVERS.size();
        String server = ANARCHY_SERVERS.get(currentServerIndex);
        if (mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.sendChatCommand(server);
            waitingForServerLoad = true;
            serverSwitchTime = System.currentTimeMillis();
            serverSwitchCooldown.reset();
            switchTimer.reset();
        }
    }
}
