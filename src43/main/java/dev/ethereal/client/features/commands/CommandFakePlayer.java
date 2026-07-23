package dev.ethereal.client.features.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import dev.ethereal.api.command.Command;
import dev.ethereal.api.command.CommandRegister;
import dev.ethereal.api.system.client.FakePlayerManager;

@CommandRegister(name = "fakeplayer")
public class CommandFakePlayer extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add")
                .then(argument("name", StringArgumentType.word())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            if (FakePlayerManager.isSpawned()) {
                                print("§cFake player already exists. Use .fakeplayer delete to remove it.");
                            } else {
                                FakePlayerManager.spawn(name);
                                print("§aFake player spawned: " + name);
                            }
                            return SINGLE_SUCCESS;
                        })
                )
        );

        builder.then(literal("delete").executes(context -> {
            if (FakePlayerManager.isSpawned()) {
                FakePlayerManager.remove();
                print("§aFake player removed.");
            } else {
                print("§cNo fake player to remove.");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("clear").executes(context -> {
            if (FakePlayerManager.isSpawned()) {
                FakePlayerManager.remove();
                print("§aFake player cleared.");
            } else {
                print("§cNo fake player to clear.");
            }
            return SINGLE_SUCCESS;
        }));

        builder.executes(context -> {
            print("§aFake Player Commands:");
            print("§7.fakeplayer add <name> §f- spawn a fake player");
            print("§7.fakeplayer delete §f- remove fake player");
            print("§7.fakeplayer clear §f- clear fake player");
            return SINGLE_SUCCESS;
        });
    }
}
