package dev.ethereal.api.system.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import dev.ethereal.api.system.interfaces.QuickImports;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class FakePlayerManager implements QuickImports {
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(1000000);
    private static OtherClientPlayerEntity fakePlayer;

    public static void spawn(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || fakePlayer != null) return;

        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        fakePlayer = new OtherClientPlayerEntity(mc.world, profile);
        fakePlayer.copyPositionAndRotation(mc.player);
        fakePlayer.setHeadYaw(mc.player.headYaw);
        fakePlayer.setBodyYaw(mc.player.bodyYaw);
        fakePlayer.getInventory().clone(mc.player.getInventory());
        fakePlayer.setHealth(mc.player.getHealth());
        fakePlayer.setAbsorptionAmount(mc.player.getAbsorptionAmount());
        fakePlayer.setId(ENTITY_ID_COUNTER.getAndIncrement());

        mc.world.addEntity(fakePlayer);
    }

    public static void remove() {
        if (fakePlayer != null) {
            fakePlayer.remove(Entity.RemovalReason.DISCARDED);
            fakePlayer = null;
        }
    }

    public static boolean isSpawned() {
        return fakePlayer != null;
    }
}
