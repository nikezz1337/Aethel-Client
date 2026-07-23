package antileak.base.api.utils.rpc;

import com.adl.nativeprotect.User;
import lombok.Getter;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.network.ServerInfo;
import antileak.base.api.QClient;
import antileak.base.api.utils.rpc.utils.DiscordEventHandlers;
import antileak.base.api.utils.rpc.utils.DiscordRPC;
import antileak.base.api.utils.rpc.utils.DiscordRichPresence;
import antileak.base.client.modules.impl.render.base.implement.WaterMark;

@Getter
public class DiscordManager implements QClient {

    private DiscordDaemonThread discordDaemonThread;
    private long APPLICATION_ID;

    private boolean running;

    private String image;
    private String site;
    private String discord;
    String uid = User.getInstance().profile("uid");
    String username = User.getInstance().profile("username");
    String hwid = User.getInstance().profile("hwid");
    private void cppInit() {
        discordDaemonThread = new DiscordDaemonThread();
        APPLICATION_ID = 1514684343556112598L;
        running = true;
        image = "https://i.ibb.co/QjKDYPnm/togif-s2.gif";
        site = "https://elysiumdlc.fun";
        discord = "https://discord.gg/kEFkH9BFtD";
    }

    String state = "";

    public static DiscordRichPresence discordRichPresence = new DiscordRichPresence();
    public static DiscordRPC discordRPC = DiscordRPC.INSTANCE;

    public void init() {
        cppInit();
        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder().build();

        DiscordRPC.INSTANCE.Discord_Initialize(String.valueOf(APPLICATION_ID), handlers, true, "");
        discordRichPresence.startTimestamp = System.currentTimeMillis() / 1000L;
        discordRPC.Discord_UpdatePresence(discordRichPresence);

        new Thread(() -> {
            while (running) {
                try {
                    discordRichPresence.details = "Name - " + username;
                    discordRichPresence.state = "ID - " + uid;
                    discordRichPresence.largeImageKey = image;
                    discordRichPresence.button_label_1 = "Купить";
                    discordRichPresence.button_url_1 = site;
                    discordRichPresence.button_label_2 = "Дискорд";
                    discordRichPresence.button_url_2 = discord;
                    DiscordRPC.INSTANCE.Discord_UpdatePresence(discordRichPresence);
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "Discord-RPC-Updater").start();

        discordDaemonThread.start();
    }

    public DiscordManager start() {
        init();
        return this;
    }

    public void stopRPC() {
        running = false;
        DiscordRPC.INSTANCE.Discord_Shutdown();
        if (discordDaemonThread != null) {
            discordDaemonThread.interrupt();
        }
    }

    private class DiscordDaemonThread extends Thread {
        @Override
        public void run() {
            this.setName("Discord-RPC");

            try {
                while (running) {
                    DiscordRPC.INSTANCE.Discord_RunCallbacks();
                    Thread.sleep(15 * 1000);
                }
            } catch (Exception exception) {
                stopRPC();
            }

            super.run();
        }
    }
}
