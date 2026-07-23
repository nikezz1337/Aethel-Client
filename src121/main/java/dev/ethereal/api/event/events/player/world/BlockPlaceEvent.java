package dev.ethereal.api.event.events.player.world;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class BlockPlaceEvent {
    private final Block block;
    private final BlockState state;
    private final BlockPos pos;
    private final LivingEntity placer;
}
