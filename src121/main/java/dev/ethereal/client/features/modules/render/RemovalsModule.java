package dev.ethereal.client.features.modules.render;

import lombok.Getter;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.BooleanSetting;
import dev.ethereal.api.module.setting.MultiBooleanSetting;

import java.util.Arrays;

@ModuleRegister(name = "Removals", category = Category.RENDER)
public class RemovalsModule extends Module {
    @Getter private static final RemovalsModule instance = new RemovalsModule();

    private final String[] elements = {
            "Огонь", "HurtCam", "В блоках", "В воде",
            "Скорборд", "Свечение", "Негативные эффекты", "Боссбар"
    };

    private final MultiBooleanSetting remove = new MultiBooleanSetting("Remove").value(
            Arrays.stream(elements)
                    .map(name -> new BooleanSetting(name).value(false))
                    .toArray(BooleanSetting[]::new)
    );

    public RemovalsModule() {
        addSettings(remove);
    }

    public boolean isFireOverlay()   { return isEnabled() && remove.isEnabled("Огонь"); }
    public boolean isHurtCamera()    { return isEnabled() && remove.isEnabled("HurtCam"); }
    public boolean isInwallOverlay() { return isEnabled() && remove.isEnabled("В блоках"); }
    public boolean isWaterOverlay()  { return isEnabled() && remove.isEnabled("В воде"); }
    public boolean isScoreboard()    { return isEnabled() && remove.isEnabled("Скорборд"); }
    public boolean isGlowEffect()    { return isEnabled() && remove.isEnabled("Свечение"); }
    public boolean isBadEffects()    { return isEnabled() && remove.isEnabled("Негативные эффекты"); }
    public boolean isBossBar()       { return isEnabled() && remove.isEnabled("Боссбар"); }

}