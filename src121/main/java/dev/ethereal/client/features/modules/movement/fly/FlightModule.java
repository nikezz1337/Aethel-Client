package dev.ethereal.client.features.modules.movement.fly;


import lombok.Getter;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.player.move.MotionEvent;
import dev.ethereal.api.event.events.player.move.TravelEvent;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.system.backend.Choice;
import dev.ethereal.client.features.modules.movement.fly.modes.FlightGrim;
import dev.ethereal.client.features.modules.movement.fly.modes.FlightVanilla;

@ModuleRegister(name = "Flight", category = Category.MOVEMENT)
public class FlightModule extends Module {
    @Getter private static final FlightModule instance = new FlightModule();

    private final FlightGrim flightGrim = new FlightGrim(() -> getMode().is("Grim"), this);
    private final FlightVanilla flightVanilla = new FlightVanilla(() -> getMode().is("Vanilla"));

    private final FlightMode[] modes = new FlightMode[]{
            flightVanilla, flightGrim
    };

    private FlightMode currentMode = flightGrim;

    @Getter private final ModeSetting mode = new ModeSetting("Режим").value(flightGrim.getName())
            .values(Choice.getValues(modes))
            .onAction(() -> {
                currentMode = (FlightMode) Choice.getChoiceByName(getMode().getValue(), modes);
            });

    public FlightModule() {
        addSettings(mode);

        addSettings(flightGrim.getSettings());
        addSettings(flightVanilla.getSettings());
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

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        currentMode.onUpdate();
    }

    @EventHandler
    public void onMotion(MotionEvent event) {
        currentMode.onMotion(event);
    }
}