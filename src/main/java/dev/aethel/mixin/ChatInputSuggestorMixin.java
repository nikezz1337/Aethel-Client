package dev.aethel.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.aethel.Aethel;
import dev.aethel.command.CommandDispatcherBuilder;

import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin {

    @Final @Shadow TextFieldWidget textField;
    @Shadow boolean completingSuggestions;
    @Shadow private ParseResults<CommandSource> parse;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow private ChatInputSuggestor.SuggestionWindow window;
    @Shadow public abstract void show(boolean narrateFirstSuggestion);
    @Shadow public abstract void clearWindow();

    @Inject(method = "refresh", at = @At("HEAD"), cancellable = true)
    public void onRefresh(CallbackInfo ci) {
        String text = this.textField.getText();
        String prefix = Aethel.getInstance().commandPrefix;

        if (!text.startsWith(prefix)) {
            if (this.window != null) {
                this.clearWindow();
            }
            this.parse = null;
            return;
        }

        StringReader reader = new StringReader(text);
        reader.setCursor(reader.getCursor() + prefix.length());

        CommandDispatcher<CommandSource> dispatcher = Aethel.getInstance().getCommandDispatcher();
        this.parse = dispatcher.parse(reader, CommandDispatcherBuilder.getSource());

        int cursor = this.textField.getCursor();
        if (cursor >= 1 && (this.window == null || !this.completingSuggestions)) {
            this.pendingSuggestions = dispatcher.getCompletionSuggestions(this.parse, cursor);
            this.pendingSuggestions.thenRun(() -> {
                if (this.pendingSuggestions.isDone()) {
                    Suggestions suggestions = this.pendingSuggestions.join();
                    if (suggestions.isEmpty()) {
                        if (this.window != null) {
                            this.clearWindow();
                        }
                    } else {
                        this.show(false);
                    }
                }
            });
        }

        ci.cancel();
    }
}
