package pro.codeservermod;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import pro.codeservermod.handler.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CoreServerMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "coreservermod";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public static Config CONFIG;
    public static Path CONFIG_PATH;

    @Override
    public void onInitializeServer() {
        LOGGER.info("Initializing CoreServerMod...");
        
        CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("coreservermod.json");
        loadConfig();
        
        registerEventHandlers();
        
        LOGGER.info("CoreServerMod initialized successfully!");
    }
    
    private void loadConfig() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                saveDefaultConfig();
            }
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
            
            ServerWorld overworld = server.getOverworld();
            if (overworld == null) return;
            
            SugarCaneHandler.tick(overworld);
            SaplingHandler.tick(overworld);
            IronGolemHandler.tick(overworld);
            CropGrowthHandler.tick(overworld);
            AnimalBreedingHandler.tick(overworld);
            BabyGrowthHandler.tick(overworld);
            SheepWoolHandler.tick(overworld);
            VillagerEmeraldHandler.tick(overworld);
            ChickenEggHandler.tick(overworld);
        });
    }
    
    private void registerTickHandlers(net.minecraft.server.MinecraftServer server) {
        SugarCaneHandler.register(server);
        SaplingHandler.register(server);
        IronGolemHandler.register(server);
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
        CropGrowthHandler.cleanup();
        AnimalBreedingHandler.cleanup();
        BabyGrowthHandler.cleanup();
        SheepWoolHandler.cleanup();
        VillagerEmeraldHandler.cleanup();
        ChickenEggHandler.cleanup();
        ChickenSpawnHandler.cleanup();
    }
}