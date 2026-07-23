package dev.ethereal.paste.xweb;

public enum Role {
    DEFAULT,
    USER,
    BETA,
    MEDIA,
    SUPPORT,
    MODERATOR,
    ADMIN,
    OWNER;

    public boolean isAtLeast(Role minimum) {
        return ordinal() >= minimum.ordinal();
    }
}
