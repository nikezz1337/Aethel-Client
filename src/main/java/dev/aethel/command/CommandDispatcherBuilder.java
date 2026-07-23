package dev.aethel.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.Registries;
import org.lwjgl.glfw.GLFW;

import dev.aethel.Aethel;
import dev.aethel.config.ConfigManager;
import dev.aethel.config.FriendManager;
import dev.aethel.config.StaffManager;
import dev.aethel.config.MacroManager;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public class CommandDispatcherBuilder {

    private static final SuggestionProvider<CommandSource> CONFIG_NAMES = (ctx, builder) -> {
        ConfigManager.getConfigs().forEach(cfg -> {
            if (cfg.equals("autocfg")) return;
            if (cfg.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(cfg);
            }
        });
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> FRIEND_NAMES = (ctx, builder) -> {
        FriendManager.getFriends().forEach(f -> {
            if (f.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(f);
            }
        });
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> STAFF_NAMES = (ctx, builder) -> {
        StaffManager.getStaff().forEach(s -> {
            if (s.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(s);
            }
        });
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> MODULE_NAMES = (ctx, builder) -> {
        Aethel.getInstance().getModuleStorage().getModules().forEach(m -> {
            if (m.getName().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(m.getName());
            }
        });
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> MACRO_NAMES = (ctx, builder) -> {
        MacroManager.getAll().forEach(m -> {
            if (m.name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(m.name);
            }
        });
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> BLOCK_NAMES = (ctx, builder) -> {
        Registries.BLOCK.getIds().forEach(id -> {
            String path = id.getPath().toLowerCase();
            if (path.startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(path);
            }
        });
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> KEYBIND_SUGGESTIONS = (ctx, builder) -> {
        try {
            var fields = GLFW.class.getDeclaredFields();
            for (var f : fields) {
                String name = f.getName();
                if (name.startsWith("GLFW_KEY_") && f.getType() == int.class) {
                    String displayName = name.replace("GLFW_KEY_", "").toLowerCase();
                    if (displayName.startsWith(builder.getRemainingLowerCase())) {
                        builder.suggest(displayName);
                    }
                }
            }
        } catch (Exception ignored) {}
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> ONLINE_PLAYERS = (ctx, builder) -> {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            mc.world.getPlayers().forEach(p -> {
                String name = p.getName().getString();
                if (name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(name);
                }
            });
        }
        return builder.buildFuture();
    };

    public static CommandDispatcher<CommandSource> build() {
        CommandDispatcher<CommandSource> d = new CommandDispatcher<>();

        d.register(literal("cfg")
            .then(literal("save")
                .then(argument("name", StringArgumentType.string()).suggests(CONFIG_NAMES).executes(ctx -> 0)))
            .then(literal("load")
                .then(argument("name", StringArgumentType.string()).suggests(CONFIG_NAMES).executes(ctx -> 0)))
            .then(literal("delete")
                .then(argument("name", StringArgumentType.string()).suggests(CONFIG_NAMES).executes(ctx -> 0)))
            .then(literal("list").executes(ctx -> 0))
        );

        d.register(literal("friend")
            .then(literal("add")
                .then(argument("name", StringArgumentType.string()).suggests(ONLINE_PLAYERS).executes(ctx -> 0)))
            .then(literal("remove")
                .then(argument("name", StringArgumentType.string()).suggests(FRIEND_NAMES).executes(ctx -> 0)))
            .then(literal("clear").executes(ctx -> 0))
            .then(literal("list").executes(ctx -> 0))
        );

        d.register(literal("staff")
            .then(literal("add")
                .then(argument("name", StringArgumentType.string()).executes(ctx -> 0)))
            .then(literal("remove")
                .then(argument("name", StringArgumentType.string()).suggests(STAFF_NAMES).executes(ctx -> 0)))
            .then(literal("list").executes(ctx -> 0))
        );

        d.register(literal("macro")
            .then(literal("add")
                .then(argument("key", StringArgumentType.word()).suggests(KEYBIND_SUGGESTIONS)
                    .then(argument("command", StringArgumentType.greedyString()).executes(ctx -> 0))))
            .then(literal("remove")
                .then(argument("name", StringArgumentType.string()).suggests(MACRO_NAMES).executes(ctx -> 0)))
            .then(literal("list").executes(ctx -> 0))
        );

        d.register(literal("bind")
            .then(literal("add")
                .then(argument("module", StringArgumentType.string()).suggests(MODULE_NAMES)
                    .then(argument("key", StringArgumentType.word()).suggests(KEYBIND_SUGGESTIONS).executes(ctx -> 0))))
            .then(literal("remove")
                .then(argument("module", StringArgumentType.string()).suggests(MODULE_NAMES).executes(ctx -> 0)))
        );

        d.register(literal("vclip")
            .then(literal("up").executes(ctx -> 0))
            .then(literal("down").executes(ctx -> 0))
            .then(argument("blocks", StringArgumentType.word()).executes(ctx -> 0))
        );

        d.register(literal("blockesp")
            .then(literal("add")
                .then(argument("block", StringArgumentType.string()).suggests(BLOCK_NAMES).executes(ctx -> 0)))
            .then(literal("remove")
                .then(argument("block", StringArgumentType.string()).suggests(BLOCK_NAMES).executes(ctx -> 0)))
            .then(literal("list").executes(ctx -> 0))
            .then(literal("clear").executes(ctx -> 0))
        );

        d.register(literal("gps")
            .then(argument("x", StringArgumentType.word())
                .then(argument("z", StringArgumentType.word()).executes(ctx -> 0)))
            .then(literal("clear").executes(ctx -> 0))
        );

        d.register(literal("rct")
            .then(argument("anarchy", StringArgumentType.word()).executes(ctx -> 0))
            .executes(ctx -> 0)
        );


        return d;
    }

    private static LiteralArgumentBuilder<CommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static <T> RequiredArgumentBuilder<CommandSource, T> argument(String name, com.mojang.brigadier.arguments.ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static CommandSource getSource() {
        return new ClientCommandSource(null, MinecraftClient.getInstance());
    }
}
