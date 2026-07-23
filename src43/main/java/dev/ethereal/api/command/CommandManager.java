package dev.ethereal.api.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ethereal.client.features.commands.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class CommandManager {
    @Getter private static final CommandManager instance = new CommandManager();

    private final CommandDispatcher<CommandSource> dispatcher;
    private final ClientCommandSource source;

    public CommandManager() {
        this.dispatcher = new CommandDispatcher<>();
        this.source = new ClientCommandSource(null, MinecraftClient.getInstance());
    }

    private final List<Command> commands = new ArrayList<>();

    public void load() {
        register(
                new CommandConfig(), 
                new CommandFriend(),
                new CommandRCT(),
                new CommandStaffs(),
                new CommandMacro(), 
                new CommandGps(),
                new CommandSkin(),
                new CommandHelp(),
                new CommandFakePlayer(),
                new BaritoneCommand()
        );
    }

    public void register(Command... commands) {
        for (Command command : commands) {
            command.register(dispatcher);
            this.commands.add(command);
        }
    }

    public String getPrefix() {
        return ".";
    }

    public boolean dispatch(String message) {
        if (message.startsWith(getPrefix())) {
            try {
                getDispatcher().execute(message.substring(getPrefix().length()), getSource());
            } catch (CommandSyntaxException ignored) {
                // Даже если команда не распознана, отменяем сообщение
            }
            // Всегда возвращаем true для сообщений начинающихся с префикса
            return true;
        }
        return false;
    }

    public void executeCommands(String message, CallbackInfo ci) {
        if (dispatch(message)) {
            ci.cancel();
        }
    }
}