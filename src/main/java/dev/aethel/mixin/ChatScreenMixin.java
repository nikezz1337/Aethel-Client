package dev.aethel.mixin;

import dev.aethel.ui.hud.ChatFriendPanel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.aethel.util.draggable.DragManager;
import dev.aethel.util.IMinecraft;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen implements IMinecraft {

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "removed", at = @At(value = "HEAD"))
    private void removed(CallbackInfo ci) {
        DragManager.onReleaseAll(0);
        ChatFriendPanel.getInstance().hide();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void aethel$handleFriendPanelClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0) {
            ChatFriendPanel panel = ChatFriendPanel.getInstance();
            if (panel.isVisible()) {
                if (panel.handleClick(mouseX, mouseY)) {
                    cir.setReturnValue(true);
                    return;
                }
                panel.hide();
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void injectDragClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0 && mc.inGameHud != null && mc.inGameHud.getChatHud() != null) {
            aethel$tryShowFriendPanel(mouseX, mouseY);
        }
        DragManager.onClickAll(button);
    }

    @Unique
    private void aethel$tryShowFriendPanel(double mouseX, double mouseY) {
        if (mc.inGameHud == null || mc.inGameHud.getChatHud() == null) return;
        if (mc.player == null || mc.world == null) return;

        ChatFriendPanel panel = ChatFriendPanel.getInstance();
        if (panel.isVisible()) return;

        ChatHudAccessor chatHudAccessor = (ChatHudAccessor) mc.inGameHud.getChatHud();
        List<ChatHudLine.Visible> visibleMessages = chatHudAccessor.aethel$getVisibleMessages();

        if (visibleMessages.isEmpty()) return;

        int chatWidth = mc.inGameHud.getChatHud().getWidth(mc.options.getChatWidth().getValue());
        int scaledHeight = mc.getWindow().getScaledHeight();

        float messageHeight = 9f;
        float chatBottom = scaledHeight - 30;

        if (mouseY < chatBottom - visibleMessages.size() * messageHeight || mouseY > chatBottom) return;
        if (mouseX < 0 || mouseX > chatWidth + 4) return;

        int clickedIndex = visibleMessages.size() - 1 - (int) ((mouseY - (chatBottom - visibleMessages.size() * messageHeight)) / messageHeight);
        if (clickedIndex < 0 || clickedIndex >= visibleMessages.size()) return;

        ChatHudLine.Visible line = visibleMessages.get(clickedIndex);
        OrderedText content = line.content();

        String foundName = aethel$extractPlayerName(content);
        if (foundName != null) {
            float panelX = (float) MathHelper.clamp(mouseX, 20, mc.getWindow().getScaledWidth() - 80);
            float panelY = (float) mouseY;
            panel.show(foundName, panelX, panelY);
        }
    }

    @Unique
    private String aethel$extractPlayerName(OrderedText content) {
        if (mc.world == null) return null;

        StringBuilder sb = new StringBuilder();
        content.accept((index, style, codepoint) -> {
            sb.appendCodePoint(codepoint);
            return true;
        });
        String lineContent = sb.toString();

        for (var player : mc.world.getPlayers()) {
            String name = player.getName().getString();
            if (lineContent.contains("<" + name + ">")) {
                return name;
            }
        }

        for (var player : mc.world.getPlayers()) {
            String name = player.getName().getString();
            if (!name.equals(mc.player.getName().getString()) && lineContent.contains(name)) {
                return name;
            }
        }

        return null;
    }

    @Inject(method = "render", at = @At("RETURN"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        DragManager.onDrawAll();
        ChatFriendPanel.getInstance().render(context);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        DragManager.onReleaseAll(button);
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
