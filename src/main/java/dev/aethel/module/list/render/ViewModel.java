package dev.aethel.module.list.render;

import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.settings.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

@ModuleInformation(
    moduleName = "ViewModel",
    moduleCategory = ModuleCategory.RENDER,
    moduleDesc = "Настройки вида руки"
)
public class ViewModel extends Module {

    public final SliderSetting mainHandX = new SliderSetting("Правая рука X", 0.0, -2.0, 2.0, 0.01);
    public final SliderSetting mainHandY = new SliderSetting("Правая рука Y", 0.0, -2.0, 2.0, 0.01);
    public final SliderSetting mainHandZ = new SliderSetting("Правая рука Z", 0.0, -2.0, 2.0, 0.01);

    public final SliderSetting offHandX = new SliderSetting("Левая рука X", 0.0, -2.0, 2.0, 0.01);
    public final SliderSetting offHandY = new SliderSetting("Левая рука Y", 0.0, -2.0, 2.0, 0.01);
    public final SliderSetting offHandZ = new SliderSetting("Левая рука Z", 0.0, -2.0, 2.0, 0.01);

    public final BooleanSetting onlyAura = new BooleanSetting("Только с аурой", false);

    public void applyHandPosition(MatrixStack matrices, Arm arm) {
        if (arm == Arm.RIGHT) {
            matrices.translate(mainHandX.getFloatValue(), mainHandY.getFloatValue(), mainHandZ.getFloatValue());
        } else {
            matrices.translate(offHandX.getFloatValue(), offHandY.getFloatValue(), offHandZ.getFloatValue());
        }
    }
}
