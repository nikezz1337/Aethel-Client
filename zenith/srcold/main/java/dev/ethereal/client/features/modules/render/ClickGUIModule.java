package dev.ethereal.client.features.modules.render;

import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.client.ui.clickgui.ScreenClickGUI;

@ModuleRegister(name = "Click GUI", category = Category.RENDER, bind = GLFW.GLFW_KEY_GRAVE_ACCENT)
public class ClickGUIModule extends Module {
    @Getter private static final ClickGUIModule instance = new ClickGUIModule();

    public ClickGUIModule() {
    }

    @Override
    public void onEnable() {
        if (mc.currentScreen != null) return;
        mc.setScreen(ScreenClickGUI.getInstance());
    }

    @Override
    public void onEvent() {
        toggle();
    }
}
