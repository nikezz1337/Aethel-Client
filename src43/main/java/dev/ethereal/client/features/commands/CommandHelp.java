package dev.ethereal.client.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import dev.ethereal.api.command.Command;
import dev.ethereal.api.command.CommandRegister;

@CommandRegister(name = "help")
public class CommandHelp extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            print("§e.help §7- Показать эту справку");
            print("§e.cfg list §7- Список конфигов");
            print("§e.cfg load <name> §7- Загрузить конфиг");
            print("§e.cfg save <name> §7- Сохранить конфиг");
            print("§e.cfg remove <name> §7- Удалить конфиг");
            print("§e.cfg folder §7- Открыть папку конфигов");
            print("§e.friend list §7- Список друзей");
            print("§e.friend add <player> §7- Добавить друга");
            print("§e.friend remove <player> §7- Удалить друга");
            print("§e.friend clear §7- Очистить список друзей");
            print("§e.macro list §7- Список макросов");
            print("§e.macro add <name> <command> §7- Добавить макрос");
            print("§e.macro remove <name> §7- Удалить макрос");
            print("§e.gps set §7- Установить GPS точку");
            print("§e.gps clear §7- Очистить GPS");
            print("§e.skin <name> §7- Изменить скин");
            print("§e.baritone <command> §7- Команды Baritone");
            print("§e.staffs §7- Список модераторов");
            return SINGLE_SUCCESS;
        });
    }
}
