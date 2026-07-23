package antileak.base.api.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.command.CommandSource;

import antileak.base.elysium;
import antileak.base.api.commands.Command;
import antileak.base.api.utils.chat.ChatUtils;
import antileak.base.api.utils.cmd.waypoint.Waypoint;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class GPSCommand extends Command {

    public GPSCommand() {
        super("gps");
    }

    
    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder
                .then(arg("X", integer())
                        .then(arg("Z", integer())
                                .executes(context -> {
                                    int x = context.getArgument("X", Integer.class);
                                    int z = context.getArgument("Z", Integer.class);

                                    Waypoint waypoint = new Waypoint(x, z);
                                    elysium.INSTANCE.waypointStorage.set(waypoint);

                                    ChatUtils.sendMessage(I18n.translate("Метка поставлена: ", x, z));
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .then(literal("remove")
                        .executes(context -> {
                            if (!elysium.INSTANCE.waypointStorage.isEmpty()) {
                                elysium.INSTANCE.waypointStorage.clear();
                                ChatUtils.sendMessage(I18n.translate("Метка удалена!"));
                            } else {
                                ChatUtils.sendMessage(I18n.translate("Метки не было"));
                            }
                            return SINGLE_SUCCESS;
                        })
                );
    }
}