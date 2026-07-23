package dev.aethel.module.list.render;

import com.google.common.eventbus.Subscribe;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.MultiBooleanSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

@ModuleInformation(
        moduleName = "See Invisibles",
        moduleCategory = ModuleCategory.RENDER,
        moduleDesc = "Показывает невидимых игроков"
)
public class SeeInvisibles extends Module {

    private final MultiBooleanSetting targets = new MultiBooleanSetting("Цели", "Игроки",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Мобы", false),
            new BooleanSetting("Животные", false));

    @Subscribe
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        boolean players = targets.isSelected("Игроки");
        boolean mobs = targets.isSelected("Мобы");
        boolean animals = targets.isSelected("Животные");

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isInvisible()) continue;
            if (player.hasStatusEffect(StatusEffects.GLOWING)) continue;
            player.setInvisible(false);
        }

        if (mobs || animals) {
            for (var entity : mc.world.getEntities()) {
                if (entity == mc.player || entity instanceof PlayerEntity) continue;
                if (!(entity instanceof LivingEntity living)) continue;
                if (!living.isInvisible()) continue;
                if (living.hasStatusEffect(StatusEffects.GLOWING)) continue;
                living.setInvisible(false);
            }
        }
    }
}
