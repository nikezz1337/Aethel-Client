package dev.ethereal.inject.client;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ethereal.client.ui.widget.WidgetManager;
import dev.ethereal.client.ui.widget.overlay.NotifWidget;

@Mixin(ChatHud.class)
public class MixinChatHud {
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"))
    private void onChat(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        NotifWidget widget = (NotifWidget) WidgetManager.getInstance().getWidgets().stream()
                .filter(w -> w instanceof NotifWidget)
                .findFirst()
                .orElse(null);
        
        if (widget == null || !widget.specRequest) return;
        
        String full = message.getString();
        String msg = full.toLowerCase();
        
        if (msg.contains("spec") || msg.contains("спек") || msg.contains("спэк")) {
            String sender = "Игрок";
            if (full.contains(":")) {
                sender = full.substring(0, full.indexOf(":")).trim();
            } else if (full.contains(">")) {
                int idx = full.indexOf(">");
                if (idx > 0) {
                    String temp = full.substring(0, idx).trim();
                    if (temp.startsWith("<")) temp = temp.substring(1);
                    sender = temp;
                }
            }
            widget.addNotif(sender + " просит о спеке!");
        }
    }
}
