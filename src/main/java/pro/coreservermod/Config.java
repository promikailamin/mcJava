package pro.coreservermod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Config {
    public boolean sugar_cane_soul_sand_enabled = true;
    public int sugar_cane_growth_interval_seconds = 1;
    public int sapling_growth_time_seconds = 15;
    public int crop_growth_time_seconds = 15;
    public boolean iron_golem_spawn_enabled = true;
    public int iron_golem_spawn_interval_seconds = 5;
    public int max_automatic_golems_per_area = 10;
    public boolean animal_breeding_cooldown_disabled = true;
    public int baby_growth_time_seconds = 15;
    public boolean instant_growth_when_fed = true;
    public int sheep_wool_regrowth_seconds = 5;
    public boolean villager_emerald_drop_enabled = true;
    public double villager_emerald_drop_chance = 0.4;
    public boolean guaranteed_chicken_from_egg = true;
    public int chicken_egg_interval_seconds = 20;
    public boolean destroyer_creeper_spawn_enabled = true;
    public int destroyer_creeper_spawn_interval_seconds = 5;
    public int max_automatic_creepers_per_area = 10;
    public boolean slime_summoner_spawn_enabled = true;
    public int slime_summoner_spawn_interval_seconds = 2;
    public int slime_summoner_slimes_per_summon = 4;
    public int max_automatic_slimes_per_area = 24;
    public boolean create_flowing_water_washing_enabled = true;
    public int create_flowing_water_washing_interval_ticks = 10;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config getDefaults() {
        return new Config();
    }

    public static String getDefaultJson() {
        return GSON.toJson(getDefaults());
    }

    public static Config fromJson(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        Config config = new Config();

        config.sugar_cane_soul_sand_enabled = getBoolean(obj, "sugar_cane_soul_sand_enabled", true);
        config.sugar_cane_growth_interval_seconds = clampInt(getInt(obj, "sugar_cane_growth_interval_seconds", 1), 1, 3600);
        config.sapling_growth_time_seconds = clampInt(getInt(obj, "sapling_growth_time_seconds", 15), 1, 3600);
        config.crop_growth_time_seconds = clampInt(getInt(obj, "crop_growth_time_seconds", 15), 1, 3600);
        config.iron_golem_spawn_enabled = getBoolean(obj, "iron_golem_spawn_enabled", true);
        config.iron_golem_spawn_interval_seconds = clampInt(getInt(obj, "iron_golem_spawn_interval_seconds", 5), 1, 3600);
        config.max_automatic_golems_per_area = clampInt(getInt(obj, "max_automatic_golems_per_area", 10), 0, 100);
        config.animal_breeding_cooldown_disabled = getBoolean(obj, "animal_breeding_cooldown_disabled", true);
        config.baby_growth_time_seconds = clampInt(getInt(obj, "baby_growth_time_seconds", 15), 1, 3600);
        config.instant_growth_when_fed = getBoolean(obj, "instant_growth_when_fed", true);
        config.sheep_wool_regrowth_seconds = clampInt(getInt(obj, "sheep_wool_regrowth_seconds", 5), 1, 3600);
        config.villager_emerald_drop_enabled = getBoolean(obj, "villager_emerald_drop_enabled", true);
        config.villager_emerald_drop_chance = clampDouble(getDouble(obj, "villager_emerald_drop_chance", 0.4), 0.0, 1.0);
        config.guaranteed_chicken_from_egg = getBoolean(obj, "guaranteed_chicken_from_egg", true);
        config.chicken_egg_interval_seconds = clampInt(getInt(obj, "chicken_egg_interval_seconds", 20), 1, 3600);
        config.destroyer_creeper_spawn_enabled = getBoolean(obj, "destroyer_creeper_spawn_enabled", true);
        config.destroyer_creeper_spawn_interval_seconds = clampInt(getInt(obj, "destroyer_creeper_spawn_interval_seconds", 5), 1, 3600);
        config.max_automatic_creepers_per_area = clampInt(getInt(obj, "max_automatic_creepers_per_area", 10), 0, 100);
        config.slime_summoner_spawn_enabled = getBoolean(obj, "slime_summoner_spawn_enabled", true);
        config.slime_summoner_spawn_interval_seconds = clampInt(getInt(obj, "slime_summoner_spawn_interval_seconds", 2), 1, 3600);
        config.slime_summoner_slimes_per_summon = clampInt(getInt(obj, "slime_summoner_slimes_per_summon", 4), 1, 32);
        config.max_automatic_slimes_per_area = clampInt(getInt(obj, "max_automatic_slimes_per_area", 24), 0, 200);
        config.create_flowing_water_washing_enabled = getBoolean(obj, "create_flowing_water_washing_enabled", true);
        config.create_flowing_water_washing_interval_ticks = clampInt(
                getInt(obj, "create_flowing_water_washing_interval_ticks", 10), 1, 1200);

        return config;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean defaultValue) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsBoolean() : defaultValue;
    }

    private static int getInt(JsonObject obj, String key, int defaultValue) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : defaultValue;
    }

    private static double getDouble(JsonObject obj, String key, double defaultValue) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsDouble() : defaultValue;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public void save() {
        try {
            java.nio.file.Files.writeString(CoreServerMod.CONFIG_PATH, toJson());
        } catch (Exception e) {
            CoreServerMod.LOGGER.error("Failed to save configuration", e);
        }
    }
}
