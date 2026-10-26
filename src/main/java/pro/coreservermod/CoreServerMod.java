package pro.coreservermod;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.coreservermod.handler.AnimalBreedingHandler;
import pro.coreservermod.handler.BabyGrowthHandler;
import pro.coreservermod.handler.BadVillagerHandler;
import pro.coreservermod.handler.ChickenEggHandler;
import pro.coreservermod.handler.ChickenSpawnHandler;
import pro.coreservermod.handler.CreateFlowingWaterWashingHandler;
import pro.coreservermod.handler.CropGrowthHandler;
import pro.coreservermod.handler.IronGolemHandler;
import pro.coreservermod.handler.SaplingHandler;
import pro.coreservermod.handler.SheepWoolHandler;
import pro.coreservermod.handler.SugarCaneHandler;
import pro.coreservermod.handler.VillagerEmeraldHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CoreServerMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "coreservermod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Config CONFIG;
    public static Path CONFIG_PATH;

    private static boolean createLoaded;

    @Override
    public void onInitializeServer() {
        LOGGER.info("Initializing CoreServerMod...");

        CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("coreservermod.json");
        loadConfig();

        createLoaded = FabricLoader.getInstance().isModLoaded("create");
        if (createLoaded) {
            LOGGER.info("Create detected, enabling Create Fly integration");
        }

        registerEventHandlers();

        LOGGER.info("CoreServerMod initialized successfully!");
    }

    private void loadConfig() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                saveDefaultConfig();
            }
            ConfigFixer.fix(CONFIG_PATH);
            String json = Files.readString(CONFIG_PATH);
            CONFIG = Config.fromJson(json);
            LOGGER.info("Configuration loaded from {}", CONFIG_PATH);
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration, using defaults", e);
            CONFIG = Config.getDefaults();
        }
    }

    private void saveDefaultConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, Config.getDefaultJson());
            LOGGER.info("Created default configuration at {}", CONFIG_PATH);
        } catch (IOException e) {
            LOGGER.error("Failed to create default configuration", e);
        }
    }

    private void registerEventHandlers() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk, generated) -> {
            SugarCaneHandler.scanChunk(world, chunk);
            SaplingHandler.scanChunk(world, chunk);
            CropGrowthHandler.scanChunk(world, chunk);
        });

        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            SugarCaneHandler.untrackChunk(world, chunk);
            SaplingHandler.untrackChunk(world, chunk);
            CropGrowthHandler.untrackChunk(world, chunk);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("CoreServerMod: Server started, registering tick handlers");
            registerTickHandlers(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("CoreServerMod: Server stopping, cleaning up");
            cleanup();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (CONFIG == null) return;

            for (ServerLevel level : server.getAllLevels()) {
                SugarCaneHandler.tick(level);
                SaplingHandler.tick(level);
                IronGolemHandler.tick(level);
                BadVillagerHandler.tick(level);
                CropGrowthHandler.tick(level);
                AnimalBreedingHandler.tick(level);
                BabyGrowthHandler.tick(level);
                SheepWoolHandler.tick(level);
                VillagerEmeraldHandler.tick(level);
                ChickenEggHandler.tick(level);
                if (createLoaded) {
                    CreateFlowingWaterWashingHandler.tick(level);
                }
            }
        });
    }

    private void registerTickHandlers(MinecraftServer server) {
        SugarCaneHandler.register(server);
        SaplingHandler.register(server);
        IronGolemHandler.register(server);
        BadVillagerHandler.register(server);
        CropGrowthHandler.register(server);
        AnimalBreedingHandler.register(server);
        BabyGrowthHandler.register(server);
        SheepWoolHandler.register(server);
        VillagerEmeraldHandler.register(server);
        ChickenEggHandler.register(server);
        ChickenSpawnHandler.register(server);
    }

    private void cleanup() {
        SugarCaneHandler.cleanup();
        SaplingHandler.cleanup();
        IronGolemHandler.cleanup();
        BadVillagerHandler.cleanup();
        CropGrowthHandler.cleanup();
        AnimalBreedingHandler.cleanup();
        BabyGrowthHandler.cleanup();
        SheepWoolHandler.cleanup();
        VillagerEmeraldHandler.cleanup();
        ChickenEggHandler.cleanup();
        ChickenSpawnHandler.cleanup();
        if (createLoaded) {
            CreateFlowingWaterWashingHandler.cleanup();
        }
    }
}
