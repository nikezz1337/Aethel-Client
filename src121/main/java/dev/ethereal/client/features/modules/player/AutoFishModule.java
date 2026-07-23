package dev.ethereal.client.features.modules.player;

import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.system.interfaces.QuickImports;

@ModuleRegister(name = "AutoFish", category = Category.PLAYER)
public class AutoFishModule extends Module implements QuickImports {
    @Getter private static final AutoFishModule instance = new AutoFishModule();

    private long lastActionTime = 0;
    private boolean isHooked = false;
    private boolean needToHook = false;

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (event.packet() instanceof PlaySoundS2CPacket packet) {
            if (packet.getSound().value().equals(SoundEvents.ENTITY_FISHING_BOBBER_SPLASH)) {
                isHooked = true;
                lastActionTime = System.currentTimeMillis();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        ItemStack mainHand = mc.player.getStackInHand(Hand.MAIN_HAND);
        if (!mainHand.isOf(Items.FISHING_ROD)) return;

        long currentTime = System.currentTimeMillis();

        if (isHooked && currentTime - lastActionTime >= 600) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            isHooked = false;
            needToHook = true;
            lastActionTime = currentTime;
        }

        if (needToHook && currentTime - lastActionTime >= 300) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            needToHook = false;
            lastActionTime = currentTime;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        isHooked = false;
        needToHook = false;
        lastActionTime = 0;
    }
}
