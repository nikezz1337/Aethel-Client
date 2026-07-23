package dev.ethereal.inject.accessors;

import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LightmapTextureManager.class)
public interface LightmapTextureManagerAccessor {
    @Accessor("dirty")
    void ethereal$setDirty(boolean dirty);
}
