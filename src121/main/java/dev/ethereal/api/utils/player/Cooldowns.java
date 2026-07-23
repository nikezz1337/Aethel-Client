package dev.ethereal.api.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class Cooldowns {
    private final ConcurrentHashMap<Item, Long> endTimeMs = new ConcurrentHashMap<>();

    public Map<Item, Long> getAllCooldowns() {
        long now = System.currentTimeMillis();
        endTimeMs.entrySet().removeIf(entry -> entry.getValue() <= now);
        return new HashMap<>(endTimeMs);
    }

    public void onCooldownPacket(Item item, int ticks) {
        if (item == null) return;
        if (ticks <= 0) {
            endTimeMs.remove(item);
            return;
        }
        endTimeMs.put(item, System.currentTimeMillis() + ticks * 50L);
    }

    public long getLeftMs(Item item) {
        if (item == null) return 0L;
        Long end = endTimeMs.get(item);
        if (end == null) return 0L;
        long left = end - System.currentTimeMillis();
        if (left <= 0) {
            endTimeMs.remove(item);
            return 0L;
        }
        return left;
    }

    public void clear(Item item) {
        if (item != null) endTimeMs.remove(item);
    }

    public void clearAll() {
        endTimeMs.clear();
    }
}
