package fun.wonderful.client.modules.impl.misc;

import fun.wonderful.client.modules.Module;
import fun.wonderful.client.modules.settings.implement.BindSetting;

public class AutoBuy extends Module {

    public static AutoBuy INSTANCE = new AutoBuy();

    public BindSetting openKey = new BindSetting("Бинд гуи", -1);

    public AutoBuy() {
        super("AutoBuy", ModuleCategory.MISC);
        addSettings(openKey);
    }
}

