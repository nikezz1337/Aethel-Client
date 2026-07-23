package antileak.base;

import com.adl.nativeprotect.Native;
import lombok.Getter;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.lwjgl.glfw.GLFW;
import antileak.base.api.QClient;
import antileak.base.api.storages.InitializeStorage;
import antileak.base.api.storages.implement.*;
import antileak.base.api.events.EventInvoker;
import antileak.base.api.utils.client.UserInfo;
import antileak.base.api.utils.draggable.Draggable;
import antileak.base.api.utils.rpc.DiscordManager;
import antileak.base.api.utils.tps.TPSCalc;
import antileak.base.client.modules.Module;

import java.io.File;

public class elysium implements ModInitializer, QClient {

    public static elysium INSTANCE;

    public elysium() {
        INSTANCE = this;
    }


    public boolean isServer;
    private static double prevTime = 0.0;
    public static double deltaTime = 0.0;

    public InitializeStorage initializer;
    public ModuleStorage moduleStorage;
    public ThemeStorage themeStorage;
    public TPSCalc tpsCalc;
    public ServerStorage serverStorage;
    public RotationStorage rotationStorage;
    public FreeLookStorage freeLookStorage;
    public CommandStorage commandStorage;
    public LocalizationStorage localizationStorage;
    public ConfigStorage configStorage;
    public FriendStorage friendStorage;
    public MacroStorage macroStorage;
    public StaffStorage staffStorage;
    public WaypointStorage waypointStorage;
    public DiscordManager discordManager;
    @Getter public UserInfo userInfo = UserInfo.empty();

    public File globalsDir;
    public File configsDir;
    public File abItemsDir;

    @Native
    @Override
    public void onInitialize() {
        this.initStorage();
        WorldRenderEvents.START.register(client -> {
            double currentTime = GLFW.glfwGetTime();
            deltaTime = currentTime - prevTime;
            prevTime = currentTime;
            deltaTime = mc.isPaused() ? 0.0 : Math.min(0.05, deltaTime);
        });
    }

    @Native
    private void initStorage() {
        this.globalsDir = new File("C:\\Elysium", "elysium");
        this.configsDir = new File(globalsDir, "configs");
        this.abItemsDir = new File(globalsDir, "abitems");

        EventInvoker.register(this);
        createDirs(globalsDir, configsDir, abItemsDir);
        this.initializer = new InitializeStorage();
        this.initializer.onInitialize();
        this.discordManager = new DiscordManager().start();
    }

    private void createDirs(File... file) {
        for (File f : file) f.mkdirs();
    }

    public void closeMinecraft() {
        try {
            configStorage.saveConfig(configStorage.currentConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (discordManager != null) {
            discordManager.stopRPC();
        }
    }

    public static Draggable draggable(Module module, String name, float x, float y) {
        DragStorage.draggables.put(name, new Draggable(module, name, x, y));
        return DragStorage.draggables.get(name);
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo == null ? UserInfo.empty() : userInfo;
    }
}
