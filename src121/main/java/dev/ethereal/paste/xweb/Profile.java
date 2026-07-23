package dev.ethereal.paste.xweb;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class Profile {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static String username = envOr("ETH_USERNAME", "xweb");
    public static int uid = envInt("ETH_UID", -1);
    public static Role role = envRole("ETH_ROLE", Role.DEFAULT);
    public static String hwid = envOr("ETH_HWID", "");
    public static String expireDate = envOr("ETH_EXPIRE", "01-01-1970");
    public static String avatarUrl = envOr("ETH_AVATAR", "");

    public static String signature = envOr("ETH_SIG", "");

    private Profile() {
    }

    private static String envOr(String key, String def) {
        String v = System.getenv(key);
        if (v == null || v.isEmpty()) {
            return def;
        }
        return v;
    }

    private static int envInt(String key, int def) {
        String v = System.getenv(key);
        if (v == null || v.isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(v);
        } catch (Exception ignored) {
            return def;
        }
    }

    private static Role envRole(String key, Role def) {
        String v = System.getenv(key);
        if (v == null || v.isEmpty()) {
            return def;
        }
        try {
            return Role.valueOf(v);
        } catch (Exception ignored) {
            return def;
        }
    }

    public static String getUsername() {
        return username;
    }

    public static int getUid() {
        return uid;
    }

    public static Role getRole() {
        return role;
    }

    public static String getHwid() {
        return hwid;
    }

    public static String getExpireDate() {
        return expireDate;
    }

    public static String getAvatarUrl() {
        return avatarUrl;
    }

    public static String getSignature() {
        return signature;
    }

    public static String descriptor() {
        return username + '|' + uid + '|' + role.name() + '|' + hwid + '|' + expireDate;
    }

    public static boolean isPlaceholder() {
        return signature == null || signature.isBlank() || uid < 0;
    }

    public static boolean isExpired() {
        try {
            LocalDate end = LocalDate.parse(expireDate, DATE_FORMAT);
            return LocalDate.now().isAfter(end);
        } catch (Exception ignored) {
            return true;
        }
    }
}
