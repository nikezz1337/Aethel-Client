package dev.ethereal.paste.xweb;

import dev.ethereal.paste.xweb.annotation.Compile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class Heartbeat {

    private static final long INTERVAL_SECONDS = 60;
    private static final int MAX_FAILURES = 3;
    private static final int HTTP_TIMEOUT_MS = 5000;

    private static volatile ScheduledExecutorService executor;
    private static volatile int consecutiveFailures;

    private Heartbeat() {
    }

    @Compile
    private static String HEARTBEAT_URL() {
        return "https://ethereal-client.tech/api/loader/heartbeat";
    }

    public static synchronized void start() {
        if (executor != null) return;
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "xweb-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(Heartbeat::tick, INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public static synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private enum PingResult {
        OK,
        DENIED,
        UNREACHABLE
    }

    private static void tick() {
        try {
            Guard.runChecks();
        } catch (ProtectionException exception) {
            SubscriptionSeeyer.getInstance().requestShutdown("heartbeat (локально): " + exception.getMessage(), 0);
            return;
        }
        switch (pingServer()) {
            case OK -> consecutiveFailures = 0;
            case DENIED -> SubscriptionSeeyer.getInstance().requestShutdown("сервер отозвал подписку", 0);
            case UNREACHABLE -> {
                if (++consecutiveFailures >= MAX_FAILURES) {
                    SubscriptionSeeyer.getInstance().requestShutdown("сервер недоступен", SubscriptionSeeyer.UNREACHABLE_GRACE_SECONDS);
                }
            }
        }
    }

    private static PingResult pingServer() {
        String base = HEARTBEAT_URL();
        if (base == null || base.isBlank()) {
            return PingResult.OK;
        }
        try {
            String url = base + (base.contains("?") ? "&" : "?") + "user=" + URLEncoder.encode(Profile.getUsername(), StandardCharsets.UTF_8) + "&hwid=" + URLEncoder.encode(Profile.getHwid(), StandardCharsets.UTF_8);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            int code = connection.getResponseCode();
            drain(connection, code);
            connection.disconnect();
            if (code == 200) return PingResult.OK;
            if (code == 401 || code == 403) return PingResult.DENIED;
            return PingResult.UNREACHABLE;
        } catch (Exception exception) {
            return PingResult.UNREACHABLE;
        }
    }

    private static void drain(HttpURLConnection connection, int code) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                code == 200 ? connection.getInputStream() : connection.getErrorStream(),
                StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {

            }
        } catch (Exception ignored) {
        }
    }
}
