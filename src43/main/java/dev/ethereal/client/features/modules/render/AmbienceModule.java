package dev.ethereal.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.util.math.MathHelper;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.ColorSetting;
import dev.ethereal.api.module.setting.SliderSetting;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.system.backend.Choice;

import java.awt.*;

@ModuleRegister(name = "Ambience", category = Category.RENDER)
public class AmbienceModule extends Module {
    @Getter private static final AmbienceModule instance = new AmbienceModule();

    @AllArgsConstructor
    private enum WorldTime implements ModeSetting.NamedChoice {
        NO_CHANGE("No change"),
        DAWN("Dawn"),
        DAY("Day"),
        NOON("Noon"),
        DUSK("Dusk"),
        NIGHT("Night"),
        MID_NIGHT("Mid Night");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }


    private final ModeSetting time = new ModeSetting("Время").value(WorldTime.DAY).values(WorldTime.values());

    private final BooleanSetting customFog = new BooleanSetting("Пользовательский туман").value(false);
    private final ColorSetting fogColor = new ColorSetting("Цвет тумана").value(new Color(200, 200, 200)).setVisible(customFog::getValue);
    private final SliderSetting fogDistance = new SliderSetting("Дистанция тумана").value(-8f).range(-8f, 25f).step(1f).setVisible(customFog::getValue);
    private final SliderSetting fogDensity = new SliderSetting("Плотность тумана").value(100f).range(0f, 100f).step(1f).setVisible(customFog::getValue);


    public AmbienceModule() {
        addSettings(
                time,
                customFog, fogColor, fogDistance, fogDensity
        );
    }

    public long getTime(long original) {
        if (mc.world == null || !isEnabled()) return original;

        WorldTime selected = Choice.getChoiceByName(time.getValue(), WorldTime.values());

        return switch (selected) {
            case NO_CHANGE -> original;
            case DAWN -> 23041L;
            case DAY -> 1000L;
            case NOON -> 6000L;
            case DUSK -> 12610L;
            case NIGHT -> 13000L;
            case MID_NIGHT -> 18000L;
        };
    }

    public boolean applyBackgroundColor() {
        if (!isEnabled() || !customFog.getValue()) return false;

        GlStateManager._clearColor(
                fogColor.getValue().getRed() / 255f,
                fogColor.getValue().getGreen() / 255f,
                fogColor.getValue().getBlue() / 255f,
                fogColor.getValue().getAlpha() / 255f
        );

        return true;
    }

    public Fog applyCustomFog(Camera camera, float viewDistance, Fog fog) {
        if (!isEnabled() || !customFog.getValue()) return fog;

        float start = MathHelper.clamp(fogDistance.getValue(), -8f, viewDistance);
        float end = MathHelper.clamp(fogDistance.getValue() + fogDensity.getValue(), 0f, viewDistance);

        FogShape shape = fog.shape();
        CameraSubmersionType type = camera.getSubmersionType();

        if (type == CameraSubmersionType.NONE) {
            shape = FogShape.SPHERE;
        }

        return new Fog(start, end, shape,
                fogColor.getValue().getRed() / 255f,
                fogColor.getValue().getGreen() / 255f,
                fogColor.getValue().getBlue() / 255f,
                fogColor.getValue().getAlpha() / 255f
        );
    }

    @Override public void onEvent() {}
}