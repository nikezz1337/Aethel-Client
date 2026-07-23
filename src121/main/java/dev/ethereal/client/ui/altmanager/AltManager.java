package dev.ethereal.client.ui.altmanager;

import dev.ethereal.api.auth.UUIDUtils;
import dev.ethereal.api.system.backend.ClientInfo;
import dev.ethereal.inject.accessors.MinecraftClientSessionAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class AltManager {
    private static final AltManager INSTANCE = new AltManager();
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private final File file = new File(ClientInfo.CONFIG_PATH_OTHER, "alts.json");
    private final List<AltAccount> accounts = new ArrayList<>();
    private String status = "";
    private String activeUsername = "";

    private AltManager() {
        load();
    }

    public static AltManager getInstance() {
        return INSTANCE;
    }

    public List<AltAccount> getAccounts() {
        return accounts;
    }

    public String getStatus() {
        return status;
    }

    public String getActiveUsername() {
        return activeUsername;
    }

    public boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public AltAccount add(String username) {
        username = username == null ? "" : username.trim();
        if (!isValidUsername(username)) {
            status = "Use 3-16 latin letters, numbers or _";
            return null;
        }

        String finalUsername = username;
        accounts.removeIf(account -> account.username().equalsIgnoreCase(finalUsername));
        AltAccount account = new AltAccount(username, UUIDUtils.generateOfflinePlayerUuid(username), 0L);
        accounts.add(0, account);
        save();
        status = "Added " + username;
        return account;
    }

    public void remove(AltAccount account) {
        if (account == null) return;
        accounts.remove(account);
        if (account.username().equalsIgnoreCase(activeUsername)) {
            activeUsername = accounts.isEmpty() ? "" : accounts.getFirst().username();
        }
        save();
        status = "Removed " + account.username();
    }

    public void login(AltAccount account) {
        if (account == null) return;

        setSession(account.username(), account.uuid());

        accounts.remove(account);
        AltAccount updated = account.withLastUsed(System.currentTimeMillis());
        accounts.add(0, updated);
        activeUsername = updated.username();
        save();
        status = "Logged in as " + account.username();
    }

    public void applySavedSession() {
        if (!isValidUsername(activeUsername)) return;

        AltAccount account = accounts.stream()
                .filter(alt -> alt.username().equalsIgnoreCase(activeUsername))
                .findFirst()
                .orElseGet(() -> new AltAccount(activeUsername, UUIDUtils.generateOfflinePlayerUuid(activeUsername), 0L));

        setSession(account.username(), account.uuid());
    }

    public void load() {
        accounts.clear();
        activeUsername = "";
        try {
            if (!file.exists()) return;
            Object root;
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                root = new JSONTokener(reader).nextValue();
            }

            JSONArray array;
            if (root instanceof JSONObject object) {
                activeUsername = object.optString("activeUsername", "");
                array = object.optJSONArray("accounts");
                if (array == null) array = new JSONArray();
            } else if (root instanceof JSONArray oldFormatArray) {
                array = oldFormatArray;
            } else {
                return;
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;

                String username = object.optString("username", "");
                if (!isValidUsername(username)) continue;

                UUID uuid;
                try {
                    uuid = UUID.fromString(object.optString("uuid", ""));
                } catch (Exception ignored) {
                    uuid = UUIDUtils.generateOfflinePlayerUuid(username);
                }

                accounts.add(new AltAccount(username, uuid, object.optLong("lastUsed", 0L)));
            }
            accounts.sort(Comparator.comparingLong(AltAccount::lastUsed).reversed());
            if (!isValidUsername(activeUsername)) activeUsername = "";
        } catch (Exception ignored) {
            status = "Could not load alts";
        }
    }

    public void save() {
        try {
            JSONArray array = new JSONArray();
            for (AltAccount account : accounts) {
                JSONObject object = new JSONObject();
                object.put("username", account.username());
                object.put("uuid", account.uuid().toString());
                object.put("lastUsed", account.lastUsed());
                array.put(object);
            }

            JSONObject root = new JSONObject();
            root.put("activeUsername", isValidUsername(activeUsername) ? activeUsername : "");
            root.put("accounts", array);

            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(root.toString(2));
            }
        } catch (Exception ignored) {
            status = "Could not save alts";
        }
    }

    private void setSession(String username, UUID uuid) {
        Session session = new Session(
                username,
                uuid,
                "0",
                Optional.empty(),
                Optional.empty(),
                Session.AccountType.LEGACY
        );
        ((MinecraftClientSessionAccessor) MinecraftClient.getInstance()).ethereal$setSession(session);
    }
}
