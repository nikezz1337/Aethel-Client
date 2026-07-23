package dev.ethereal.client.features.modules.movement;

import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import lombok.Getter;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import net.minecraft.item.ExperienceBottleItem;

@ModuleRegister(name = "NoDelay", category = Category.PLAYER)
public class NoJumpDelayModule extends Module {
    @Getter private static final NoJumpDelayModule instance = new NoJumpDelayModule();

    private final MultiBooleanSetting remove = new MultiBooleanSetting("Убирать задержку").value(
            new BooleanSetting("Опыт").value(true),
            new BooleanSetting("Прыжки").value(true),
            new BooleanSetting("Ставить").value(false)
    );

    public NoJumpDelayModule() {
        addSettings(remove);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
          if (remove.isEnabled("Прыжки")) {
              mc.player.jumpingCooldown = 0;
          }

          if (remove.isEnabled("Ставить")) {
              mc.itemUseCooldown = 0;
          }

          if (remove.isEnabled("Опыт")) {
              if (mc.player.getMainHandStack().getItem() instanceof ExperienceBottleItem || mc.player.getOffHandStack().getItem() instanceof ExperienceBottleItem) {
                  mc.itemUseCooldown = 0;
              }
          }
        }));

        addEvents(updateEvent);
    }
}
