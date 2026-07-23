package dev.ethereal.client.features.modules.render.targetesp;

import lombok.Getter;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.events.player.other.UpdateEvent;
import dev.ethereal.api.event.events.render.Render3DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.client.features.modules.render.targetesp.modes.TargetEspCrystals;
import dev.ethereal.client.features.modules.render.targetesp.modes.TargetEspGhosts;
import dev.ethereal.client.features.modules.render.targetesp.modes.TargetEspTexture;

@ModuleRegister(name = "Target Esp", category = Category.RENDER)
public class TargetEspModule extends Module {
    @Getter private static final TargetEspModule instance = new TargetEspModule();

    private final TargetEspTexture  espTexture  = new TargetEspTexture();
    private final TargetEspCrystals espCrystals = new TargetEspCrystals();
    private final TargetEspGhosts   espGhosts   = new TargetEspGhosts();
//    private final TargetEspOrbit    espOrbit    = new TargetEspOrbit();

    private TargetEspMode currentMode = espTexture;

    @Getter private final ModeSetting mode = new ModeSetting("Режим")
            .value("Маркер")
            .values("Маркер", "Кристалы", "Призраки")
            .onAction(() -> currentMode = switch (getMode().getValue()) {
                case "Кристалы" -> espCrystals;
                case "Призраки"   -> espGhosts;
                default         -> espTexture;
            });

    private final SliderSetting crystalsCount = new SliderSetting("Кол-во")
            .value(14f).range(1f, 20f).step(1f)
            .setVisible(() -> mode.is("Кристалы"));

    private final SliderSetting crystalsSpeed = new SliderSetting("Скорость")
            .value(3f).range(0f, 5f).step(0.5f)
            .setVisible(() -> mode.is("Кристалы"));

    public TargetEspModule() {
        addSettings(mode, crystalsCount, crystalsSpeed);
    }

    @Override
    public void onEvent() {
        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            TargetEspMode.updatePositions();
            currentMode.onRender3D(event);
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.updateAnimation();
            currentMode.updateTarget();
            currentMode.onUpdate();
        }));

        addEvents(render3DEvent, updateEvent);
    }

    public int   getCrystalsCount() { return crystalsCount.getValue().intValue(); }
    public float getCrystalsSpeed() { return crystalsSpeed.getValue().floatValue(); }
}
