package dev.ethereal.inject.accessors;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Invoker("syncSelectedSlot")
    void ethereal$syncSelectedSlot();

    @Invoker("sendSequencedPacket")
    void ethereal$sendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator);

    @Accessor("blockBreakingCooldown")
    void ethereal$setBlockBreakingCooldown(int blockBreakingCooldown);

    @Accessor("currentBreakingProgress")
    float ethereal$getCurrentBreakingProgress();

    @Accessor("currentBreakingProgress")
    void ethereal$setCurrentBreakingProgress(float progress);
}
