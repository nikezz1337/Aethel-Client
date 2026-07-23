package dev.ethereal.client.features.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ethereal.api.command.Command;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleManager;
import net.minecraft.command.CommandSource;

import java.util.Locale;

public abstract class ToggleCommand extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("module", StringArgumentType.greedyString())
                .suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(
                        ModuleManager.getInstance().getModules().stream().map(Module::getName),
                        suggestionsBuilder
                ))
                .executes(context -> {
                    String moduleName = StringArgumentType.getString(context, "module");
                    Module module = findModule(moduleName);

                    if (module == null) {
                        print("Module not found: " + moduleName);
                        return SINGLE_SUCCESS;
                    }

                    module.toggle();
                    print(module.getName() + (module.isEnabled() ? " enabled." : " disabled."));
                    return SINGLE_SUCCESS;
                }));

        builder.executes(context -> {
            print("Usage: ." + getName() + " <module>");
            return SINGLE_SUCCESS;
        });
    }

    private Module findModule(String input) {
        String normalizedInput = normalize(input);

        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module.getName().equalsIgnoreCase(input) || normalize(module.getName()).equals(normalizedInput)) {
                return module;
            }
        }

        return null;
    }

    private String normalize(String value) {
        return value.replace(" ", "").replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }
}
