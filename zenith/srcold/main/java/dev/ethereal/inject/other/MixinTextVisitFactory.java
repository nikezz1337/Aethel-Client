package dev.ethereal.inject.other;

import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import dev.ethereal.api.system.backend.SharedClass;
import dev.ethereal.api.utils.other.ReplaceUtil;

@Mixin(TextVisitFactory.class)
public class MixinTextVisitFactory {
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", ordinal = 0), method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", index = 0)
    private static String visitFormatted(String string) {
        if (SharedClass.player() == null) {
            return string;
        }

        return ReplaceUtil.protectedString(string);
    }
}
