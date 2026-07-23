package dev.ethereal.client.features.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;
import dev.ethereal.api.command.Command;
import dev.ethereal.api.command.CommandRegister;
import dev.ethereal.client.features.modules.movement.BaritoneModule;

@CommandRegister(name = "baritone")
public class BaritoneCommand extends Command {

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        BaritoneModule baritone = BaritoneModule.getInstance();

        // .baritone goto <x> <y> <z>
        builder.then(literal("goto")
            .then(argument("x", IntegerArgumentType.integer())
            .then(argument("y", IntegerArgumentType.integer())
            .then(argument("z", IntegerArgumentType.integer())
            .executes(context -> {
                int x = IntegerArgumentType.getInteger(context, "x");
                int y = IntegerArgumentType.getInteger(context, "y");
                int z = IntegerArgumentType.getInteger(context, "z");
                
                if (!baritone.isEnabled()) {
                    baritone.setEnabled(true);
                }
                
                baritone.goToBlock(new BlockPos(x, y, z));
                print("§aBaritone идет к координатам: " + x + ", " + y + ", " + z);
                return SINGLE_SUCCESS;
            })))));

        // .baritone mine <block>
        builder.then(literal("mine")
            .then(argument("block", StringArgumentType.string())
            .executes(context -> {
                String blockName = StringArgumentType.getString(context, "block");
                
                if (!baritone.isEnabled()) {
                    baritone.setEnabled(true);
                }
                
                baritone.mine(blockName);
                print("§aBaritone начал копать: " + blockName);
                return SINGLE_SUCCESS;
            })));

        // .baritone follow <player>
        builder.then(literal("follow")
            .then(argument("player", StringArgumentType.string())
            .executes(context -> {
                String playerName = StringArgumentType.getString(context, "player");
                
                if (!baritone.isEnabled()) {
                    baritone.setEnabled(true);
                }
                
                baritone.follow(playerName);
                print("§aBaritone следует за: " + playerName);
                return SINGLE_SUCCESS;
            })));

        // .baritone cancel
        builder.then(literal("cancel").executes(context -> {
            baritone.cancelPath();
            print("§aBaritone остановлен");
            return SINGLE_SUCCESS;
        }));

        // .baritone stop (alias for cancel)
        builder.then(literal("stop").executes(context -> {
            baritone.cancelPath();
            print("§aBaritone остановлен");
            return SINGLE_SUCCESS;
        }));

        // .baritone toggle
        builder.then(literal("toggle").executes(context -> {
            baritone.toggle();
            print(baritone.isEnabled() ? "§aBaritone включен" : "§cBaritone выключен");
            return SINGLE_SUCCESS;
        }));

        // .baritone (help)
        builder.executes(context -> {
            print("§cИспользование:");
            print("§7.baritone goto <x> <y> <z> - идти к координатам");
            print("§7.baritone mine <block> - копать блок");
            print("§7.baritone follow <player> - следовать за игроком");
            print("§7.baritone cancel - отменить путь");
            print("§7.baritone toggle - включить/выключить модуль");
            return SINGLE_SUCCESS;
        });
    }
}
