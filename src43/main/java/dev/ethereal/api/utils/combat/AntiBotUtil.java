package dev.ethereal.api.utils.combat;

import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.player.PlayerUtil;
import dev.ethereal.client.features.modules.combat.AntiBotModule;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * AntiBot utility — ReallyWorld mode built-in.
 * Filters bots based on offline UUID mismatch (ReallyWorld servers use offline UUIDs for real players).
 */
public class AntiBotUtil implements QuickImports {

    private static final Set<UUID> botSet = new HashSet<>();

    public static void onPlayerList(PlayerListS2CPacket packet) {
        if (MinecraftClient().getNetworkHandler() == null) return;
        packet.getEntries().forEach(entry -> {
            UUID uuid = entry.profileId();
            if (entry.latency() > 0 || (entry.profile().getProperties() != null && !entry.profile().getProperties().isEmpty())) {
                botSet.remove(uuid);
                return;
            }
            boolean isDuplicate = MinecraftClient().getNetworkHandler().getPlayerList().stream()
                    .filter(p -> p.getProfile().getName().equals(entry.profile().getName())
                            && !p.getProfile().getId().equals(uuid))
                    .count() == 1;
            if (isDuplicate) botSet.add(uuid);
        });
    }

    public static boolean isBot(PlayerEntity entity) {
        if (entity == null) return false;
        if (botSet.contains(entity.getUuid())) return true;
        if (isInvalidName(entity.getGameProfile().getName())) return true;

        if (PlayerUtil.isRW()) {
            String name = entity.getGameProfile().getName();
            UUID expectedOffline = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (!entity.getUuid().equals(expectedOffline)) {
                botSet.add(entity.getUuid());
                return true;
            }
        }

        if (AntiBotModule.getInstance().isEnabled() && AntiBotModule.isBot(entity)) {
            return true;
        }

        return false;
    }

    public static void reset() {
        botSet.clear();
    }

    private static boolean isInvalidName(String name) {
        if (name == null || name.isEmpty()) return true;
        for (char c : name.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') return true;
        }
        return false;
    }

    private static net.minecraft.client.MinecraftClient MinecraftClient() {
        return net.minecraft.client.MinecraftClient.getInstance();
    }
}
