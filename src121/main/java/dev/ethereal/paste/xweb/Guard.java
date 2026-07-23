package dev.ethereal.paste.xweb;

import dev.ethereal.paste.xweb.annotation.Compile;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class Guard {

    private static final boolean ENFORCE = BuildFlags.ENFORCE;

    private static volatile boolean authenticated;

    private Guard() {
    }

    @Compile
    private static String SERVER_PUBLIC_KEY_BASE64() {
        return "MCowBQYDK2VwAyEAmtfVZyOvZy9BL4HiwdJqHihRWWOok8OnJhOZVshml9w=";
    }

    public static boolean isAuthenticated() {
        return authenticated;
    }

    public static void bootstrap() {
        try {
            runChecks();
            authenticated = true;
            Heartbeat.start();
            SubscriptionSeeyer.getInstance().start();
        } catch (ProtectionException exception) {
            authenticated = false;
            applyFailure(exception.getMessage());
        }
    }

    static void applyFailure(String reason) {
        authenticated = false;
        if (ENFORCE) {
            fail(reason);
        } else {
            System.err.println("[xweb] protection bypass (" + reason + ")");
        }
    }

    static void runChecks() {
        if (Profile.isPlaceholder()) {
            throw new ProtectionException("запуск вне лоадера");
        }
        if (Profile.isExpired()) {
            throw new ProtectionException("подписка истекла");
        }
        String hwid = HwidProvider.current();
        if (!hwid.equalsIgnoreCase(Profile.getHwid())) {
            throw new ProtectionException("HWID не совпадает");
        }
        if (!verifySignature()) {
            throw new ProtectionException("невалидная подпись профиля");
        }
    }

    private static boolean verifySignature() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(SERVER_PUBLIC_KEY_BASE64());
            PublicKey publicKey = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(keyBytes));

            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(Profile.descriptor().getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(Profile.getSignature()));
        } catch (Exception exception) {
            return false;
        }
    }

    public static void requireAuthenticated() {
        if (!authenticated) {
            throw new ProtectionException("нет валидной сессии");
        }
    }

    public static void requireRole(Role minimum) {
        requireAuthenticated();
        if (!Profile.getRole().isAtLeast(minimum)) {
            throw new ProtectionException("недостаточно прав: нужна роль " + minimum);
        }
    }

    private static void fail(String reason) {
        System.err.println("[xweb] доступ запрещён: " + reason);
        Runtime.getRuntime().halt(1);
    }
}
