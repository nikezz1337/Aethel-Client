package dev.ethereal.inject.input;

import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ethereal.api.command.CommandManager;

@Mixin(ChatScreen.class)
public class MixinChatScreen {
    
    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
        if (CommandManager.getInstance().dispatch(message)) {
            ci.cancel();
        }
    }
}
