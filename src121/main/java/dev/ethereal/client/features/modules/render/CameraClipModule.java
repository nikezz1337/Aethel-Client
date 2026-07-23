package dev.ethereal.client.features.modules.render;

import lombok.Getter;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.SliderSetting;

@ModuleRegister(name = "Camera Clip", category = Category.RENDER)
public class CameraClipModule extends Module {
    @Getter private static final CameraClipModule instance = new CameraClipModule();

    public final SliderSetting distance = new SliderSetting("Distance").value(4f).range(1f,10f).step(1f);

    public CameraClipModule() {
        addSettings(distance);
    }

}
