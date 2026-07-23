package dev.aethel.mixin;

import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import dev.aethel.module.list.misc.NameProtect;

@Mixin(DrawContext.class)
public class DrawContextNameProtectMixin {

    @ModifyVariable(
            method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private String aethel$patchStringShadow(String text) {
        return patch(text);
    }

    @ModifyVariable(
            method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private String aethel$patchString(String text) {
        return patch(text);
    }

    private String patch(String text) {
        if (NameProtect.INSTANCE == null || !NameProtect.INSTANCE.isEnabled()) return text;
        return NameProtect.INSTANCE.patchIncomingText(text);
    }
}
