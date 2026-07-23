package antileak.base.client.modules.impl.render;

import antileak.base.api.events.EventLink;
import antileak.base.api.events.Priority;
import antileak.base.api.events.implement.EventRender;
import antileak.base.api.utils.render.hands.ShaderHandsRenderer;
import antileak.base.client.modules.Module;
import antileak.base.client.modules.settings.implement.BooleanSetting;
import antileak.base.client.modules.settings.implement.FloatSetting;
import antileak.base.client.modules.settings.implement.ModeSetting;

public class ShaderHands extends Module {

    public static ShaderHands INSTANCE = new ShaderHands();
    private static final ShaderHandsRenderer RENDERER = ShaderHandsRenderer.getInstance();

    public final ModeSetting mode = new ModeSetting("Режим", "Свечение", "Свечение", "Красивый");

    public final FloatSetting waveSpeed = new FloatSetting("Скорость волн", 1.2f, 0.1f, 5.0f, 0.1f)
            .visible(() -> mode.is("Красивый"));
    public final FloatSetting waveScale = new FloatSetting("Частота волн", 1.0f, 1.0f, 3.0f, 0.1f)
            .visible(() -> mode.is("Красивый"));

    public final FloatSetting outline = new FloatSetting("Ширина обводки", 1.2f, 0.1f, 5.0f, 0.1f);
    public final FloatSetting glow = new FloatSetting("Сила свечения", 1.0f, 0.0f, 5.0f, 0.1f);
    public final FloatSetting fill = new FloatSetting("Заливка", 0.6f, 0.0f, 1.0f, 0.01f);
    public final FloatSetting alpha = new FloatSetting("Прозрачность", 1.0f, 0.0f, 1.0f, 0.05f);

    public ShaderHands() {
        super("ShaderHands", "Красивый шейдер на руки и предметы", ModuleCategory.RENDER);
        addSettings(
                mode,
                waveSpeed,
                waveScale,
                outline,
                glow,
                fill,
                alpha
        );
    }

    @EventLink(priority = Priority.MEDIUM)
    public void onRender2D(EventRender.Default event) {
        if (!isEnable()) return;
        RENDERER.renderOverlayIfPending(event.getPartialTicks());
    }
}
