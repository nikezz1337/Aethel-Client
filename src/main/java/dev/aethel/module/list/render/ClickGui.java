package dev.aethel.module.list.render;

import org.lwjgl.glfw.GLFW;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.list.misc.ClientSounds;
import dev.aethel.ui.ClickGuiFrame;
import dev.aethel.ui.Panel;

@ModuleInformation(moduleName = "Click Gui", moduleCategory = ModuleCategory.RENDER, moduleKeybind = GLFW.GLFW_KEY_RIGHT_SHIFT, moduleDesc = "Меню настроек")
public class ClickGui extends Module {

    private ClickGuiFrame clickGuiFrame;

    @Override
    protected boolean shouldPlayToggleSound() {
        return false;
    }

    @Override
    public void onEnable() {
        if (clickGuiFrame == null) clickGuiFrame = new ClickGuiFrame();
        clickGuiFrame.open = true;
        clickGuiFrame.openAnimation.setValue(0);
        mc.setScreen(clickGuiFrame);
        for (Panel panel : clickGuiFrame.getPanels()) {
            panel.getAnimationAlpha().setValue(0);
            panel.getAnimationAlpha().setStartValue(0);
            panel.getAnimationAlpha().reset();
        }
        if (ClientSounds.INSTANCE != null && ClientSounds.INSTANCE.isEnabled()) {
            ClientSounds.INSTANCE.playOpenGui();
        }
        toggle();
    }
}
