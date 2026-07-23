package dev.ethereal.client.features.modules.render;

import lombok.Getter;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;
import dev.ethereal.api.module.setting.ThemeSetting;
import dev.ethereal.client.services.RenderService;
import dev.ethereal.client.ui.widget.WidgetManager;

import java.util.List;

@ModuleRegister(name = "Interface", category = Category.RENDER)
public class InterfaceModule extends Module {
    @Getter private static final InterfaceModule instance = new InterfaceModule();

    public final ThemeSetting theme = new ThemeSetting("Theme");
    public final MultiBooleanSetting widgets = new MultiBooleanSetting("Widgets");
    public final MultiBooleanSetting watermarkElements = new MultiBooleanSetting("Watermark Elements")
            .value(List.of(
                    new BooleanSetting("Icon").value(true),
                    new BooleanSetting("Name").value(true),
                    new BooleanSetting("FPS").value(true),
                    new BooleanSetting("BPS").value(true),
                    new BooleanSetting("IP").value(true)
            ));
    public final SliderSetting watermarkAlpha = new SliderSetting("Watermark Alpha").value(200f).range(0f, 255f).step(1f);
    public final SliderSetting watermarkBlur = new SliderSetting("Watermark Blur").value(0.6f).range(0.0f, 1f).step(0.1f);
    public final SliderSetting scale = new SliderSetting("Scale").value(0.9f).range(0.6f, 1.5f).step(0.05f).onAction(() -> RenderService.getInstance().updateScale());
    public final SliderSetting glassy = new SliderSetting("Blur Strength").value(0.6f).range(0.0f, 1f).step(0.1f);

    public static float getScale() { return getInstance().scale.getValue(); }
    public static float getGlassy() { return 1f - getInstance().glassy.getValue(); }
    public static int getPasses() { return 3; }
    public static float getOffset() { return 12f; }

    public void init() {
        widgets.value(WidgetManager.getInstance().getWidgets().stream()
                .map(widget -> {
                    BooleanSetting setting = new BooleanSetting(widget.getName()).value(widget.isEnabled());
                    setting.onAction(() -> widget.setEnabled(setting.getValue()));
                    return setting;
                })
                .toList());

        addSettings(theme, widgets, watermarkElements, watermarkAlpha, watermarkBlur, scale, glassy);
    }

    @Override
    public void onEvent() {

    }
}
