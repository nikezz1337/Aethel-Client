package dev.ethereal.client.features.modules.other;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.utils.other.StopWatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ModuleRegister(name = "AutoDuel", category = Category.OTHER, description = "Автоматически вызывает игроков на дуэль")
public class AutoDuelModule extends Module {
    @Getter private static final AutoDuelModule instance = new AutoDuelModule();

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");

    private final ModeSetting gameMode = new ModeSetting("Сервер").value("ReallyWorld").values("ReallyWorld", "Funtime");

    private final ModeSetting rwKit = new ModeSetting("Набор (RW)").value("Шары")
            .values("Шары", "Щит", "Шипы 3", "Незеритка", "Читерский рай", "Лук", "Классик", "Тотемы", "Нодебафф")
            .setVisible(() -> gameMode.is("ReallyWorld"));

    private final ModeSetting ftArmor = new ModeSetting("Броня (FT)").value("Незеритовая")
            .values("Незеритовая", "Алмазная")
            .setVisible(() -> gameMode.is("Funtime"));

    public AutoDuelModule() {
        addSettings(gameMode, rwKit, ftArmor);
    }

    private final Set<String> sent = new HashSet<>();
    private int currentIndex = 0;
    private final StopWatch duelTimer = new StopWatch();
    private final StopWatch cycleTimer = new StopWatch();
    private final StopWatch kitTimer = new StopWatch();
    private final StopWatch confirmTimer = new StopWatch();

    private double lastX, lastY, lastZ;

    @Override
    public void onEnable() {
        sent.clear();
        currentIndex = 0;
        if (mc.player != null) {
            lastX = mc.player.getX();
            lastY = mc.player.getY();
            lastZ = mc.player.getZ();
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        double dx = mc.player.getX() - lastX;
        double dy = mc.player.getY() - lastY;
        double dz = mc.player.getZ() - lastZ;
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) > 500) {
            setEnabled(false);
            return;
        }
        lastX = mc.player.getX();
        lastY = mc.player.getY();
        lastZ = mc.player.getZ();

        List<String> players = getOnlinePlayers();

        if (cycleTimer.isReached(800L * Math.max(1, players.size()))) {
            sent.clear();
            currentIndex = 0;
            cycleTimer.reset();
        }

        if (gameMode.is("ReallyWorld")) {
            handleReallyWorld(players);
        } else {
            handleFuntime();
        }
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (!event.isReceive()) return;
        if (!(event.packet() instanceof GameMessageS2CPacket pkt)) return;
        String text = pkt.content().getString().toLowerCase();

        if (text.contains("начало") && text.contains("через") && text.contains("секунд")) {
            setEnabled(false);
        }
    }

    private void handleReallyWorld(List<String> players) {
        if (!players.isEmpty() && duelTimer.isReached(300)) {
            if (currentIndex >= players.size()) currentIndex = 0;
            String target = players.get(currentIndex);
            if (!sent.contains(target) && !target.equals(mc.player.getGameProfile().getName())) {
                mc.player.networkHandler.sendChatMessage("/duel " + target);
                sent.add(target);
            }
            currentIndex++;
            duelTimer.reset();
        }

        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            String title = screen.getTitle().getString();

            if (title.contains("Выбор набора") && kitTimer.isReached(50)) {
                int slot = rwKitSlot();
                mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
                kitTimer.reset();
            } else if (title.contains("Настройка поединка") && confirmTimer.isReached(50)) {
                mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, 0, 0, SlotActionType.QUICK_MOVE, mc.player);
                confirmTimer.reset();
            }
        }
    }

    private void handleFuntime() {
        List<String> targets = getFuntimeTargets();
        if (duelTimer.isReached(300)) {
            for (String target : targets) {
                if (!sent.contains(target) && !target.equals(mc.player.getGameProfile().getName())) {
                    mc.player.networkHandler.sendChatMessage("/duel invite " + target);
                    sent.add(target);
                    duelTimer.reset();
                    break;
                }
            }
        }
    }

    private int rwKitSlot() {
        return switch (rwKit.getValue()) {
            case "Шипы 3"       -> 1;
            case "Лук"          -> 2;
            case "Тотемы"       -> 3;
            case "Нодебафф"     -> 4;
            case "Шары"         -> 5;
            case "Классик"      -> 6;
            case "Читерский рай"-> 7;
            case "Незеритка"    -> 8;
            default             -> 0;
        };
    }

    private List<String> getOnlinePlayers() {
        return mc.player.networkHandler.getPlayerList().stream()
                .map(e -> e.getProfile().getName())
                .filter(name -> NAME_PATTERN.matcher(name).matches())
                .collect(Collectors.toList());
    }

    private List<String> getFuntimeTargets() {
        List<String> online = getOnlinePlayers();
        List<String> result = new ArrayList<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            String name = player.getGameProfile().getName();
            if (!online.contains(name)) continue;
            if (name.equals(mc.player.getGameProfile().getName())) continue;
            if (mc.player.distanceTo(player) > 250) continue;
            boolean matches = ftArmor.is("Незеритовая") ? hasArmor(player, true) : hasArmor(player, false);
            if (matches) result.add(name);
        }
        return result;
    }

    private boolean hasArmor(PlayerEntity player, boolean netherite) {
        for (ItemStack stack : player.getArmorItems()) {
            if (stack.isEmpty()) continue;
            if (netherite && isNetheriteArmor(stack)) return true;
            if (!netherite && isDiamondArmor(stack)) return true;
        }
        return false;
    }

    private boolean isNetheriteArmor(ItemStack stack) {
        return stack.getItem() == Items.NETHERITE_HELMET
                || stack.getItem() == Items.NETHERITE_CHESTPLATE
                || stack.getItem() == Items.NETHERITE_LEGGINGS
                || stack.getItem() == Items.NETHERITE_BOOTS;
    }

    private boolean isDiamondArmor(ItemStack stack) {
        return stack.getItem() == Items.DIAMOND_HELMET
                || stack.getItem() == Items.DIAMOND_CHESTPLATE
                || stack.getItem() == Items.DIAMOND_LEGGINGS
                || stack.getItem() == Items.DIAMOND_BOOTS;
    }
}
