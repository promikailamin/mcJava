package com.mcjava.minecraft.client;

import android.content.Context;
import android.util.Log;

public class MinecraftClient {
    private static final String TAG = "MinecraftClient";
    
    static {
        System.loadLibrary("mcjava-native");
    }
    
    private static MinecraftClient instance;
    private final Context context;
    private boolean running = false;
    private Thread gameThread;
    
    // Game components
    private WorldRenderer worldRenderer;
    private InputManager inputManager;
    private ResourceManager resourceManager;
    private GameLoop gameLoop;
    
    private MinecraftClient(Context context) {
        this.context = context.getApplicationContext();
        Log.i(TAG, "Creating MinecraftClient instance");
    }
    
    public static synchronized MinecraftClient getInstance(Context context) {
        if (instance == null) {
            instance = new MinecraftClient(context);
        }
        return instance;
    }
    
    public static MinecraftClient getInstance() {
        return instance;
    }
    
    public void initialize() {
        Log.i(TAG, "Initializing Minecraft client...");
        
        // Initialize resource manager
        resourceManager = new ResourceManager(context);
        
        // Initialize input manager
        inputManager = new InputManager();
        
        // Initialize world renderer
        worldRenderer = new WorldRenderer();
        
        // Initialize game loop
        gameLoop = new GameLoop(this);
        
        Log.i(TAG, "Minecraft client initialized");
    }
    
    public void start() {
        if (running) return;
        
        running = true;
        gameThread = new Thread(gameLoop, "Minecraft Game Loop");
        gameThread.start();
        
        Log.i(TAG, "Game loop started");
    }
    
    public void stop() {
        running = false;
        
        if (gameThread != null) {
            gameThread.interrupt();
            try {
                gameThread.join(2000);
            } catch (InterruptedException e) {
                Log.w(TAG, "Game thread join interrupted", e);
            }
        }
        
        if (worldRenderer != null) {
            worldRenderer.dispose();
        }
        
        Log.i(TAG, "Game loop stopped");
    }
    
    public void onPause() {
        running = false;
        Log.i(TAG, "Game paused");
    }
    
    public void onResume() {
        if (!running && gameThread != null && !gameThread.isAlive()) {
            start();
        }
        Log.i(TAG, "Game resumed");
    }
    
    public void onSurfaceCreated() {
        if (worldRenderer != null) {
            worldRenderer.onSurfaceCreated();
        }
    }
    
    public void onSurfaceChanged(int width, int height) {
        if (worldRenderer != null) {
            worldRenderer.onSurfaceChanged(width, height);
        }
    }
    
    // Native method for buffer swap
    private native void nativeSwapBuffers();
    
    public void onDrawFrame() {
        if (worldRenderer != null) {
            worldRenderer.render();
        }
        nativeSwapBuffers();
    }
    
    public void onTouchEvent(int action, float x, float y, int pointerId) {
        if (inputManager != null) {
            inputManager.onTouchEvent(action, x, y, pointerId);
        }
    }
    
    public void onKeyEvent(int keyCode, int action) {
        if (inputManager != null) {
            inputManager.onKeyEvent(keyCode, action);
        }
    }
    
    // Getters
    public Context getContext() { return context; }
    public WorldRenderer getWorldRenderer() { return worldRenderer; }
    public InputManager getInputManager() { return inputManager; }
    public ResourceManager getResourceManager() { return resourceManager; }
    public boolean isRunning() { return running; }
}
