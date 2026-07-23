package dev.ethereal.client.features.modules.render;

import dev.ethereal.api.event.EventListener;
import dev.ethereal.api.event.Listener;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.utils.render.hands.ShaderHandsRenderer;
import lombok.Getter;

@ModuleRegister(name = "ShaderHands", category = Category.RENDER)
public class ShaderHandsModule extends Module {
    @Getter private static final ShaderHandsModule instance = new ShaderHandsModule();
    private static final ShaderHandsRenderer RENDERER = ShaderHandsRenderer.getInstance();

    public final ModeSetting mode = new ModeSetting("Режим")
            .value("Свечение").values("Свечение", "Красивый");

    public final SliderSetting waveSpeed = new SliderSetting("Скорость волн").value(1.2f).range(0.1f, 5.0f).step(0.1f)
            .setVisible(() -> mode.is("Красивый"));
    public final SliderSetting waveScale = new SliderSetting("Частота волн").value(1.0f).range(1.0f, 3.0f).step(0.1f)
            .setVisible(() -> mode.is("Красивый"));

    public final SliderSetting outline = new SliderSetting("Ширина обводки").value(1.2f).range(0.1f, 5.0f).step(0.1f);
    public final SliderSetting glow = new SliderSetting("Сила свечения").value(1.0f).range(0.0f, 5.0f).step(0.1f);
    public final SliderSetting fill = new SliderSetting("Заливка").value(0.6f).range(0.0f, 1.0f).step(0.01f);
    public final SliderSetting alpha = new SliderSetting("Прозрачность").value(1.0f).range(0.0f, 1.0f).step(0.05f);

    public ShaderHandsModule() {
        addSettings(mode, waveSpeed, waveScale, outline, glow, fill, alpha);
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render2DEvent.getInstance().subscribe(new Listener<>(0, event -> {
            if (!isEnabled()) return;
            RENDERER.renderOverlayIfPending();
        }));
        addEvents(renderEvent);
    }

    @Override
    public void onDisable() {
        RENDERER.invalidateState();
        super.onDisable();
    }
}
