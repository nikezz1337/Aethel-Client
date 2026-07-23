package dev.ethereal.client.features.modules.movement.speed;


import lombok.Getter;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.player.move.TravelEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.system.backend.Choice;
import dev.ethereal.client.features.modules.movement.speed.modes.SpeedGrim;
import dev.ethereal.client.features.modules.movement.speed.modes.SpeedVanilla;

@ModuleRegister(name = "Speed", category = Category.MOVEMENT)
public class SpeedModule extends Module {
    @Getter private static final SpeedModule instance = new SpeedModule();

    private final SpeedGrim speedGrim = new SpeedGrim(() -> getMode().is("Grim"));
    private final SpeedVanilla speedVanilla = new SpeedVanilla(() -> getMode().is("Vanilla"));

    private final SpeedMode[] modes = new SpeedMode[]{
            speedVanilla, speedGrim
    };

    private SpeedMode currentMode = speedGrim;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value(speedGrim.getName())
            .values(Choice.getValues(modes))
            .onAction(() -> {
                currentMode = (SpeedMode) Choice.getChoiceByName(getMode().getValue(), modes);
            });

    public SpeedModule() {
        addSettings(mode);

        addSettings(speedGrim.getSettings());
        addSettings(speedVanilla.getSettings());
    }

    @Override
    public void toggle() {
        super.toggle();
        currentMode.toggle();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        currentMode.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentMode.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onUpdate();
        }));

        EventListener travelEvent = TravelEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onTravel();
        }));

        addEvents(updateEvent, travelEvent);
    }
}