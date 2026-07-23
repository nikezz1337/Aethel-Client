package fun.wonderful.mixin;

import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fun.wonderful.Wonderful;

@Mixin(Main.class)
public class MainMixin {

    @Inject(method = "main([Ljava/lang/String;)V", at = @At("HEAD"))
    private static void onMain(String[] args, CallbackInfo ci) {
        if (Wonderful.INSTANCE.isServer) {
            try {
                Wonderful.INSTANCE.closeMinecraft();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Wonderful.INSTANCE.isServer = false;
        }
    }
}