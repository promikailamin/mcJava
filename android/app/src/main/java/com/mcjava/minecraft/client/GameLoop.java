package com.mcjava.minecraft.client;

import android.util.Log;

public class GameLoop implements Runnable {
    private static final String TAG = "GameLoop";
    
    private final MinecraftClient client;
    private volatile boolean running = true;
    
    // Timing
    private static final double TARGET_FPS = 60.0;
    private static final double TARGET_TIME_PER_FRAME = 1_000_000_000.0 / TARGET_FPS; // nanoseconds
    private static final int MAX_FRAME_SKIPS = 5;
    
    public GameLoop(MinecraftClient client) {
        this.client = client;
    }
    
    @Override
    public void run() {
        Log.i(TAG, "Game loop starting");
        
        long lastTime = System.nanoTime();
        double unprocessedTime = 0;
        int frames = 0;
        long lastFpsTime = System.currentTimeMillis();
        
        while (running && !Thread.currentThread().isInterrupted()) {
            boolean render = false;
            long startTime = System.nanoTime();
            long passedTime = startTime - lastTime;
            lastTime = startTime;
            unprocessedTime += passedTime;
            
            int frameSkips = 0;
            while (unprocessedTime > TARGET_TIME_PER_FRAME && frameSkips < MAX_FRAME_SKIPS) {
                update((float) (TARGET_TIME_PER_FRAME / 1_000_000_000.0));
                unprocessedTime -= TARGET_TIME_PER_FRAME;
                frameSkips++;
                render = true;
            }
            
            if (render) {
                render();
                frames++;
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            // FPS counter
            if (System.currentTimeMillis() - lastFpsTime >= 1000) {
                Log.d(TAG, "FPS: " + frames);
                frames = 0;
                lastFpsTime = System.currentTimeMillis();
            }
        }
        
        Log.i(TAG, "Game loop ended");
    }
    
    private void update(float deltaTime) {
        // Update game logic
        if (client.getInputManager() != null) {
            client.getInputManager().update(deltaTime);
        }
        
        // Update world, entities, etc.
        // TODO: Implement game logic updates
    }
    
    private void render() {
        // Render frame
        client.onDrawFrame();
    }
    
    public void stop() {
        running = false;
    }
}
