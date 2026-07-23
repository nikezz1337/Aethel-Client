package antileak.base.client.modules.impl.render;

import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.client.modules.Module;
import antileak.base.mods.particular.ParticularWaterSplash;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParticularWater extends Module {
    public static final ParticularWater INSTANCE = new ParticularWater();

    private final ParticularWaterSplash splash = new ParticularWaterSplash();
    private final Map<UUID, Boolean> itemWasInWater = new HashMap<>();
    private final Map<UUID, Deque<Float>> itemVelocities = new HashMap<>();

    private boolean wasInWater;
    private World trackedWorld;

    public ParticularWater() {
        super("Particular", "Water splash particles", ModuleCategory.RENDER);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    @EventLink
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            resetState();
            return;
        }

        if (trackedWorld != mc.world) {
            resetState();
            trackedWorld = mc.world;
        }

        ClientPlayerEntity player = mc.player;
        splash.trackVelocity(player);

        boolean inWater = player.isTouchingWater() || player.isSubmergedInWater();
        if (inWater && !wasInWater) {
            splash.trySpawnOnWaterEntry(player);
        }
        wasInWater = inWater;

        for (Entity entity : mc.world.getOtherEntities(null, player.getBoundingBox().expand(64.0D), e -> e instanceof ItemEntity)) {
            ItemEntity item = (ItemEntity) entity;
            UUID uuid = item.getUuid();
            boolean currentlyInWater = item.isTouchingWater() || item.isSubmergedInWater();
            boolean wasIn = itemWasInWater.getOrDefault(uuid, false);

            Deque<Float> velocities = itemVelocities.computeIfAbsent(uuid, key -> new ArrayDeque<>(4));
            velocities.addLast((float) Math.abs(item.getVelocity().y));
            if (velocities.size() > 4) {
                velocities.removeFirst();
            }

            if (currentlyInWater && !wasIn) {
                float speed = velocities.isEmpty() ? 0f : Collections.max(velocities);
                ParticularWaterSplash.spawnEmitter(item.getWorld(), item.getX(), item.getY(), item.getZ(), item.getWidth() * 2f, speed);
            }

            itemWasInWater.put(uuid, currentlyInWater);
        }
    }

    private void resetState() {
        wasInWater = false;
        trackedWorld = null;
        itemWasInWater.clear();
        itemVelocities.clear();
    }
}
