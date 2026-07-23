package antileak.base.client.modules.impl.render;

import antileak.base.client.modules.Module;

public class NoVignette extends Module {

    public static NoVignette INSTANCE = new NoVignette();
    public NoVignette() {
        super("NoVignette", "Убирает затемнения на краях экрана", ModuleCategory.RENDER);
    }
}

