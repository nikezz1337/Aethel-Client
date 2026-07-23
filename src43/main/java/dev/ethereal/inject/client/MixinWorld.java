package dev.ethereal.inject.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.ethereal.api.system.backend.Choice;
import dev.ethereal.client.features.modules.render.AmbienceModule;

@Mixin(World.class)
public class MixinWorld {

}