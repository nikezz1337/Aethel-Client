package dev.ethereal;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import dev.ethereal.api.command.CommandManager;
import dev.ethereal.api.module.ModuleManager;
import dev.ethereal.api.system.DiscordHook;
import dev.ethereal.api.system.configs.ConfigManager;
import dev.ethereal.api.system.configs.ConfigSkin;
import dev.ethereal.api.system.configs.FriendManager;
import dev.ethereal.api.system.configs.MacroManager;
import dev.ethereal.api.system.draggable.DraggableManager;
import dev.ethereal.api.system.files.FileManager;
import dev.ethereal.api.utils.other.SoundUtil;
import dev.ethereal.api.utils.render.KawaseBlurProgram;
import dev.ethereal.api.utils.rotation.manager.RotationManager;
import dev.ethereal.client.services.HeartbeatService;
import dev.ethereal.client.services.RenderService;
import dev.ethereal.client.ui.altmanager.AltManager;
import dev.ethereal.client.ui.theme.ThemeEditor;
import dev.ethereal.client.ui.widget.WidgetManager;
import dev.ethereal.paste.xweb.Guard;
import dev.ethereal.paste.xweb.Heartbeat;

public class Ethereal implements ClientModInitializer {
    @Getter private static Ethereal instance = new Ethereal();

    @Override
    public void onInitializeClient() {
        instance = this;

        Guard.bootstrap();

        SoundUtil.load();

        loadManagers();
        loadServices();
        loadFiles();
    }

    public void postLoad() {
        KawaseBlurProgram.load();
    }

    private void loadFiles() {
        ConfigManager.getInstance().load("autoConfig");
        DraggableManager.getInstance().load();
        FriendManager.getInstance().load();
        MacroManager.getInstance().load();
        AltManager.getInstance().applySavedSession();
    }

    private void loadManagers() {
        WidgetManager.getInstance().load();
        RotationManager.getInstance().load();

        ModuleManager.getInstance().load();
        CommandManager.getInstance().load();

        ThemeEditor.getInstance().load();
    }

    private void loadServices() {
        HeartbeatService.getInstance().load();
        RenderService.getInstance().load();
        ConfigSkin.getInstance().load();

        DiscordHook.startRPC();
    }

    public void onClose() {
        ConfigManager.getInstance().save("autoConfig");
        FileManager.getInstance().save();
        ThemeEditor.getInstance().save(true);
        DraggableManager.getInstance().save();
        MacroManager.getInstance().save();

        DiscordHook.stopRPC();
        Heartbeat.stop();
    }
}
