package dev.ethereal.inject.accessors;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x")
    int ethereal$getX();

    @Accessor("y")
    int ethereal$getY();

    @Invoker("onMouseClick")
    void ethereal$onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);
}
