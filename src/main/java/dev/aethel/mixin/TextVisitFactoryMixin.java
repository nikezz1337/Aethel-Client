package dev.aethel.mixin;

import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import dev.aethel.module.list.misc.NameProtect;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {

    @ModifyArg(
            method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
                    ordinal = 0
            ),
            index = 0
    )
    private static String aethel$patchVisitedText(String text) {
        if (NameProtect.INSTANCE == null || !NameProtect.INSTANCE.isEnabled()) return text;
        return NameProtect.INSTANCE.patchIncomingText(text);
    }
}
