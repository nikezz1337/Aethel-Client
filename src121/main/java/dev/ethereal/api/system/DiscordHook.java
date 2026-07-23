package dev.ethereal.api.system;

import dev.ethereal.api.system.backend.ClientInfo;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.paste.xweb.Profile;
import fun.crashsystem.jdrpc.DiscordIPC;
import fun.crashsystem.jdrpc.DiscordIPCConfig;
import fun.crashsystem.jdrpc.activity.Activity;
import fun.crashsystem.jdrpc.activity.ActivityType;
import fun.crashsystem.jdrpc.entity.DiscordBuild;
import fun.crashsystem.jdrpc.entity.User;
import fun.crashsystem.jdrpc.event.DiscordEventListener;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class DiscordHook implements QuickImports {
    private final long CLIENT_ID = 1516914591920160979L;
    private final String LARGE_IMAGE_KEY = "photo_2026-04-20_19-43-25";
    private final String OWNER_URL = "https://t.me/Dershanya";
    private final String TELEGRAM_URL = "https://t.me/EtherealSoftik";

    private DiscordIPC client;
    private ScheduledExecutorService updater;
    private long startedAt;

    public synchronized void startRPC() {
        if (client != null) return;

        startedAt = System.currentTimeMillis() / 1000L;
        client = DiscordIPC.create(DiscordIPCConfig.builder()
                .clientId(CLIENT_ID)
                .preferredBuilds(List.of(DiscordBuild.STABLE, DiscordBuild.CANARY, DiscordBuild.PTB))
                .reconnect(true)
                .maxReconnectAttempts(8)
                .build());

        client.addListener(new DiscordEventListener() {
            @Override
            public void onReady(User user) {
                updatePresence();
            }

            @Override
            public void onDisconnect(int errorCode, String message) {
                System.out.println("RPC Disconnected: " + message + " (" + errorCode + ")");
            }

            @Override
            public void onError(int errorCode, String message) {
                System.out.println("RPC Errored: " + message + " (" + errorCode + ")");
            }
        });

        updater = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "Ethereal Discord RPC");
            thread.setDaemon(true);
            return thread;
        });

        client.connectAsync()
                .thenRun(DiscordHook::updatePresence)
                .exceptionally(error -> {
                    System.out.println("RPC Startup failed: " + error.getMessage());
                    return null;
                });

        updater.scheduleAtFixedRate(DiscordHook::updatePresenceSafely, 15L, 15L, TimeUnit.SECONDS);
    }

    public synchronized void stopRPC() {
        if (updater != null) {
            updater.shutdownNow();
            updater = null;
        }

        if (client != null) {
            try {
                client.close();
            } catch (Exception ignored) {
            }
            client = null;
        }
    }

    private void updatePresenceSafely() {
        try {
            updatePresence();
        } catch (Exception ignored) {
        }
    }

    private void updatePresence() {
        DiscordIPC rpc = client;
        if (rpc == null || !rpc.isConnected()) return;

        try {
            rpc.setActivity(new Activity.Builder()
                    .setType(ActivityType.PLAYING)
                    .setDetails("Ethereal " + ClientInfo.VERSION)
                    .setState(normalizeActivityText(buildState()))
                    .setLargeImage(LARGE_IMAGE_KEY, "Ethereal")
                    .setStartTimestamp(startedAt)
                    .addButton("Owner", OWNER_URL)
                    .addButton("Telegram", TELEGRAM_URL)
                    .build());
        } catch (IOException error) {
            System.out.println("RPC Activity update failed: " + error.getMessage());
        }
    }

    private String buildState() {
        String username = Profile.getUsername();
        if (username == null || username.isBlank()) {
            username = "Ethereal";
        }

        String server = getServerAddress();
        if (server.isEmpty()) {
            return username;
        }
        return username + " | " + server;
    }

    private String getServerAddress() {
        try {
            if (mc.isInSingleplayer()) return "Singleplayer";
            if (mc.getNetworkHandler() == null) return "";

            var serverInfo = mc.getNetworkHandler().getServerInfo();
            if (serverInfo != null && serverInfo.address != null && !serverInfo.address.isBlank()) {
                return serverInfo.address.trim();
            }

            if (mc.getNetworkHandler().getConnection() == null) return "";

            String address = mc.getNetworkHandler().getConnection().getAddress().toString();
            address = address.replaceAll("/.*", "")
                    .replaceAll(":.*", "")
                    .replaceAll("\\.+$", "")
                    .trim();
            return address.isEmpty() ? "" : address;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String normalizeActivityText(String text) {
        if (text == null || text.isBlank()) {
            return "Ethereal";
        }

        text = text.trim();
        if (text.length() < 2) {
            return text + " ";
        }
        if (text.length() > 128) {
            return text.substring(0, 128);
        }
        return text;
    }
}
