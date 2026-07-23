package dev.ethereal.client.features.modules.render;

import lombok.Getter;
import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.render.EntityColorEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.utils.color.ColorUtil;

import java.awt.*;

@ModuleRegister(name = "See Invisibles", category = Category.RENDER)
public class SeeInvisiblesModule extends Module {
    @Getter private static final SeeInvisiblesModule instance = new SeeInvisiblesModule();

    private final SliderSetting alpha = new SliderSetting("Alpha").value(0.5f).range(0.0f, 1f).step(0.01f);

    public SeeInvisiblesModule() {
        addSettings(alpha);
    }

    // TODO: пофиксить hurt.
    @EventHandler
    public void onEntityColor(EntityColorEvent event) {
        int donichka = (int) (alpha.getValue() * 255);
        event.color(ColorUtil.setAlpha(new Color(event.color()), donichka).getRGB());
        event.setCancel(true);
    }
}
