package antileak.base.api.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;

import antileak.base.elysium;
import antileak.base.api.commands.Command;
import antileak.base.api.utils.chat.ChatUtils;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class StaffCommand extends Command {

    public StaffCommand() {
        super("staff");
    }

    
    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder
                .then(literal("add")
                        .then(arg("player", word())
                                .suggests((context, builder1) -> {
                                    for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                                        String name = entry.getProfile().getName();
                                        if (name.toLowerCase().startsWith(builder1.getRemaining().toLowerCase())) {
                                            builder1.suggest(name);
                                        }
                                    }
                                    return builder1.buildFuture();
                                })
                                .executes(context -> {
                                    String player = context.getArgument("player", String.class);
                                    if (!elysium.INSTANCE.staffStorage.isStaff(player)) {
                                        elysium.INSTANCE.staffStorage.add(player);
                                        ChatUtils.sendMessage("Игрок " + player + " добавлен в список стаффов!");
                                    } else {
                                        ChatUtils.sendMessage("Игрок " + player + " уже в списке стаффов!");
                                    }
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .then(literal("remove")
                        .then(arg("player", word())
                                .suggests((context, builder1) -> {
                                    elysium.INSTANCE.staffStorage.getStaffs().stream()
                                            .sorted(String::compareTo)
                                            .filter(name -> name.startsWith(builder1.getRemaining()))
                                            .forEach(builder1::suggest);
                                    return builder1.buildFuture();
                                })
                                .executes(context -> {
                                    String player = context.getArgument("player", String.class);
                                    if (elysium.INSTANCE.staffStorage.isStaff(player)) {
                                        elysium.INSTANCE.staffStorage.remove(player);
                                        ChatUtils.sendMessage("Игрок " + player + " удалён из списка стаффов!");
                                    } else {
                                        ChatUtils.sendMessage("Игрок " + player + " не найден в списке стаффов!");
                                    }
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .then(literal("list")
                        .executes(context -> {
                            StringBuilder builder1 = new StringBuilder();
                            if (elysium.INSTANCE.staffStorage.getStaffs().isEmpty()) {
                                ChatUtils.sendMessage("Список стаффов пуст!");
                            } else {
                                for (int i = 0; i < elysium.INSTANCE.staffStorage.getStaffs().size(); i++) {
                                    builder1.append(elysium.INSTANCE.staffStorage.getStaffs().get(i));
                                    if (i < elysium.INSTANCE.staffStorage.getStaffs().size() - 1) {
                                        builder1.append(", ");
                                    }
                                }
                                builder1.append(".");
                                ChatUtils.sendMessage("Стаффы: " + builder1);
                            }
                            return SINGLE_SUCCESS;
                        })
                )
                .then(literal("clear")
                        .executes(context -> {
                            if (!elysium.INSTANCE.staffStorage.isEmpty()) {
                                elysium.INSTANCE.staffStorage.clear();
                                ChatUtils.sendMessage("Список стаффов очищен!");
                            } else {
                                ChatUtils.sendMessage("Список стаффов пуст!");
                            }
                            return SINGLE_SUCCESS;
                        })
                );
    }
}