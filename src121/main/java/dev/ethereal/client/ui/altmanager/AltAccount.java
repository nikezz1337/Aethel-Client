package dev.ethereal.client.ui.altmanager;

import java.util.UUID;

public record AltAccount(String username, UUID uuid, long lastUsed) {
    public AltAccount withLastUsed(long lastUsed) {
        return new AltAccount(username, uuid, lastUsed);
    }
}
