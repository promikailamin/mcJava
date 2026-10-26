package pro.coreservermod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ConfigFixer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigFixer() {
    }

    public static boolean fix(Path configPath) {
        if (!Files.exists(configPath)) {
            return false;
        }

        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(configPath));
            if (!parsed.isJsonObject()) {
                CoreServerMod.LOGGER.warn("Configuration at {} is not a JSON object, skipping config fix", configPath);
                return false;
            }

            JsonObject existing = parsed.getAsJsonObject();
            JsonObject defaults = JsonParser.parseString(Config.getDefaultJson()).getAsJsonObject();

            if (addMissingValues(existing, defaults)) {
                Files.writeString(configPath, GSON.toJson(existing));
                CoreServerMod.LOGGER.info("Added missing configuration values to {}", configPath);
                return true;
            }
        } catch (Exception e) {
            CoreServerMod.LOGGER.warn("Failed to check configuration for missing values", e);
        }

        return false;
    }

    private static boolean addMissingValues(JsonObject target, JsonObject defaults) {
        boolean changed = false;

        for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
            String key = entry.getKey();
            JsonElement defaultValue = entry.getValue();

            if (!target.has(key)) {
                target.add(key, defaultValue.deepCopy());
                changed = true;
            } else if (target.get(key).isJsonObject() && defaultValue.isJsonObject()) {
                changed |= addMissingValues(target.getAsJsonObject(key), defaultValue.getAsJsonObject());
            }
        }

        return changed;
    }
}
