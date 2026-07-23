package dev.aethel.module.list.player;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.MultiBooleanSetting;
import net.minecraft.item.ExperienceBottleItem;

@ModuleInformation(
        moduleName = "NoDelay",
        moduleCategory = ModuleCategory.PLAYER,
        moduleDesc = "Убирает задержки прыжков и использования предметов"
)
public class NoDelayModule extends Module {

    private final MultiBooleanSetting remove = new MultiBooleanSetting("Убирать задержку", "",
            new BooleanSetting("Опыт", true),
            new BooleanSetting("Прыжки", true),
            new BooleanSetting("Ставить", false)
    );

    @Subscribe
    public void onEvent(EventTick event) {
        if (mc.player == null) return;

        if (remove.getValue("Прыжки")) {
            mc.player.jumpingCooldown = 0;
        }

        if (remove.getValue("Ставить")) {
            mc.itemUseCooldown = 0;
        }

        if (remove.getValue("Опыт")) {
            if (mc.player.getMainHandStack().getItem() instanceof ExperienceBottleItem ||
                    mc.player.getOffHandStack().getItem() instanceof ExperienceBottleItem) {
                mc.itemUseCooldown = 0;
            }
        }
    }
}
