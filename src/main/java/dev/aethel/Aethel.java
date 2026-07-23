package dev.aethel;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.mojang.brigadier.CommandDispatcher;
import dev.aethel.command.CommandDispatcherBuilder;
import dev.aethel.config.ConfigManager;
import dev.aethel.config.ClientConfig;
import dev.aethel.config.FriendManager;
import dev.aethel.config.MacroManager;
import dev.aethel.config.RCTHandler;
import dev.aethel.system.DiscordHook;
import dev.aethel.event.list.EventIntegration;
import dev.aethel.event.list.EventKeyInput;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleStorage;
import dev.aethel.module.list.combat.aura.rotation.FreeLookComponent;
import dev.aethel.ui.ThemeManagerWindow;
import dev.aethel.util.draggable.DragManager;
import dev.aethel.util.math.TPSGetter;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;

public class Aethel implements ModInitializer {

    private static Aethel instance;

    private final EventBus eventBus;
    private final ModuleStorage moduleStorage;
    private final DragManager dragManager;
    private final TPSGetter tpsGetter;
    private final CommandDispatcher<CommandSource> commandDispatcher;
    public String commandPrefix = ".";
    private long lastAutoSave;

    public Aethel() {
        instance = this;
        dragManager = new DragManager();

        eventBus = new EventBus();
        eventBus.register(this);

        moduleStorage = new ModuleStorage();
        tpsGetter = new TPSGetter();
        commandDispatcher = CommandDispatcherBuilder.build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DiscordHook.stopRPC();
            dragManager.saveDraggables();
        }));
    }

    public TPSGetter getTpsGetter() {
        return tpsGetter;
    }

    public static Aethel getInstance() {
        return instance == null ? new Aethel() : instance;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ModuleStorage getModuleStorage() {
        return moduleStorage;
    }

    public CommandDispatcher<CommandSource> getCommandDispatcher() {
        return commandDispatcher;
    }

    @Override
    public void onInitialize() {
        ConfigManager.init();
        FriendManager.loadFromFile();
        ThemeManagerWindow.initFromFile();
        new EventIntegration();
        getModuleStorage().injectRegisterModules();
        dragManager.load();
        eventBus.register(new FreeLookComponent());
        eventBus.register(RCTHandler.getInstance());
        ConfigManager.load("autocfg");
        DiscordHook.startRPC();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getWindow() != null) {
            mc.getWindow().setTitle(ClientConfig.getWindowTitle());
        }
    }

    @Subscribe
    private void onModuleKeyPressed(EventKeyInput event) {
        if (event.getAction() != 1 || MinecraftClient.getInstance().currentScreen != null) return;
        for (Module module : getModuleStorage().getModules()) {
            if (module.getKey() == event.getKey()) {
                module.toggle();
            }
        }
        var macro = MacroManager.getAll().stream().filter(m -> m.key == event.getKey()).findFirst();
        macro.ifPresent(m -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.networkHandler.sendChatMessage(m.command);
            }
        });
    }

    @Subscribe
    private void onTick(EventTick event) {
        if (System.currentTimeMillis() - lastAutoSave > 3000) {
            lastAutoSave = System.currentTimeMillis();
            ConfigManager.autoSave();
        }
    }
}
