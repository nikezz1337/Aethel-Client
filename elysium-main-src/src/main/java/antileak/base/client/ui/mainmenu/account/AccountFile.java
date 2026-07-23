package antileak.base.client.ui.mainmenu.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import antileak.base.api.QClient;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public record AccountFile(File file) implements QClient
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public AccountFile {
        Objects.requireNonNull(file, "file");
    }

    public boolean read(AccountManager accounts) {
        if (!this.file.exists()) {
            return false;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(this.file))) {
            JsonObject jsonObject = GSON.fromJson((Reader)reader, JsonObject.class);
            if (jsonObject == null) {
                return false;
            }
            JsonArray array = jsonObject.has("accounts") ? jsonObject.getAsJsonArray("accounts") : null;
            if (array != null) {
                for (JsonElement element : array) {
                    try {
                        JsonObject accountObject = element.getAsJsonObject();
                        if (!accountObject.has("name") || !accountObject.has("creationDate")) {
                            continue;
                        }
                        String name = accountObject.get("name").getAsString();
                        LocalDateTime creationDate = LocalDateTime.parse(accountObject.get("creationDate").getAsString());
                        boolean favorite = accountObject.has("favorite") && accountObject.get("favorite").getAsBoolean();
                        Account account = new Account(creationDate, name);
                        account.favorite(favorite);
                        if (!accounts.isAccount(name)) {
                            accounts.add(account);
                        }
                    }
                    catch (IllegalStateException | DateTimeParseException | NullPointerException ignored) {}
                }
            }
            return true;
        }
        catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    public boolean write(AccountManager accounts) {
        return this.writeJson(this.buildJson(accounts, mc.getSession() == null ? "" : mc.getSession().getUsername()));
    }

    public boolean writeLastSelected(AccountManager accounts, String lastName) {
        return this.writeJson(this.buildJson(accounts, lastName == null ? "" : lastName));
    }

    private boolean writeJson(JsonObject json) {
        File parent = this.file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return false;
        }
        try (FileWriter writer = new FileWriter(this.file)) {
            GSON.toJson((JsonElement)json, (Appendable)writer);
            return true;
        }
        catch (IOException ignored) {
            return false;
        }
    }

    private JsonObject buildJson(AccountManager accounts, String lastName) {
        JsonObject json = new JsonObject();
        JsonArray array = new JsonArray();
        for (Account account : accounts) {
            JsonObject accountObject = new JsonObject();
            accountObject.addProperty("name", account.name());
            accountObject.addProperty("creationDate", account.creationDate().toString());
            accountObject.addProperty("favorite", Boolean.valueOf(account.favorite()));
            array.add((JsonElement)accountObject);
        }
        json.add("accounts", (JsonElement)array);
        json.addProperty("last", lastName);
        return json;
    }

    public String getLast() {
        if (!this.file.exists()) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(this.file))) {
            JsonObject jsonObject = GSON.fromJson((Reader)reader, JsonObject.class);
            if (jsonObject == null || !jsonObject.has("last")) {
                return "";
            }
            return jsonObject.get("last").getAsString();
        }
        catch (Exception ignored) {
            return "";
        }
    }
}
