package antileak.base.mixin;

import net.minecraft.entity.player.ItemCooldownManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.entity.player.ItemCooldownManager$Entry")
public interface ItemCooldownManagerEntryAccessor {
    @Accessor("startTick")
    int elysium$getStartTick();

    @Accessor("endTick")
    int elysium$getEndTick();
}
