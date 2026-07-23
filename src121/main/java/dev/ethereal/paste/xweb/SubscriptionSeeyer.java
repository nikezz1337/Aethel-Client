package dev.ethereal.paste.xweb;

import dev.ethereal.api.system.backend.SharedClass;
import dev.ethereal.api.utils.math.TimerUtil;
import dev.ethereal.api.utils.other.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SubscriptionSeeyer {

    private static final SubscriptionSeeyer INSTANCE = new SubscriptionSeeyer();

    public static final long UNREACHABLE_GRACE_SECONDS = 45;
    private static final int[] SHUTDOWN_ANNOUNCE_AT = {45, 30, 15, 5};

    private static final Duration REMINDER_INTERVAL = Duration.ofMinutes(5);
    private static final Duration RENEW_WARNING = Duration.ofDays(3);
    private static final Duration RENEW_CRITICAL = Duration.ofHours(1);

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final TimerUtil shutdownTimer = new TimerUtil();
    private final TimerUtil reminderTimer = new TimerUtil();

    private volatile ScheduledExecutorService executor;

    private volatile boolean shutdownPending;
    private volatile String shutdownReason;
    private volatile long shutdownGraceSeconds;
    private volatile boolean announceCountdown;
    private int lastAnnouncedThreshold = Integer.MAX_VALUE;
    private boolean pvpNoticeSent;

    private SubscriptionSeeyer() {
    }

    public static SubscriptionSeeyer getInstance() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (executor != null) return;
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "xweb-subscription-seeyer");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public synchronized void requestShutdown(String reason, long graceSeconds) {
        if (shutdownPending) return;
        shutdownPending = true;
        shutdownReason = reason;
        shutdownGraceSeconds = graceSeconds;
        announceCountdown = graceSeconds >= UNREACHABLE_GRACE_SECONDS;
        lastAnnouncedThreshold = Integer.MAX_VALUE;
        pvpNoticeSent = false;
        shutdownTimer.reset();
    }

    private void tick() {
        try {
            if (shutdownPending) {
                handleShutdown();
            } else {
                handleReminders();
            }
        } catch (Exception ignored) {
        }
    }

    private void handleShutdown() {
        long remainingSeconds = Math.max(0, shutdownGraceSeconds - shutdownTimer.getElapsedTime() / 1000);

        if (announceCountdown && remainingSeconds > 0) {
            for (int threshold : SHUTDOWN_ANNOUNCE_AT) {
                if (remainingSeconds <= threshold && lastAnnouncedThreshold > threshold) {
                    lastAnnouncedThreshold = threshold;
                    sendCritical("Сервер недоступен. Клиент закроется через " + threshold + " секунд");
                    break;
                }
            }
        }

        if (remainingSeconds > 0) return;

        if (SharedClass.inPvp()) {
            if (!pvpNoticeSent) {
                pvpNoticeSent = true;
                sendCritical("После окончания пвп клиент закроется, причина: " + shutdownReason);
            }
            return;
        }

        Guard.applyFailure(shutdownReason);
    }

    private void handleReminders() {
        if (mc.player == null || mc.world == null) return;
        if (!reminderTimer.finished(REMINDER_INTERVAL.toMillis())) return;

        Duration remaining = remainingSubscription();
        if (remaining == null || remaining.isNegative() || remaining.isZero()) return;

        if (remaining.compareTo(RENEW_CRITICAL) < 0) {
            sendCritical("Подписка закончится уже через " + Math.max(1, remaining.toMinutes()) + " мин, продли чтобы клиент не вылетел!");
            reminderTimer.reset();
        } else if (remaining.compareTo(RENEW_WARNING) < 0) {
            sendWarn("Твоя подписка заканчивается через " + Math.max(1, remaining.toDays()) + " дней, пора продлить её!");
            reminderTimer.reset();
        }
    }

    private Duration remainingSubscription() {
        try {
            LocalDate end = LocalDate.parse(Profile.getExpireDate(), Profile.DATE_FORMAT);
            return Duration.between(LocalDateTime.now(), end.plusDays(1).atStartOfDay());
        } catch (Exception exception) {
            return null;
        }
    }

    private void sendCritical(String text) {
        send(Text.literal(text).styled(style -> style.withBold(true).withColor(Formatting.RED)));
    }

    private void sendWarn(String text) {
        send(Text.literal(text).styled(style -> style.withBold(true).withColor(Formatting.YELLOW)));
    }

    private void send(Text message) {
        mc.execute(() -> {
            if (mc.player != null) {
                TextUtil.sendMessage(message);
            }
        });
    }
}
