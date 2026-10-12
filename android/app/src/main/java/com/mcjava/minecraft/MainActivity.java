package com.mcjava.minecraft;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;

import com.mcjava.minecraft.client.MinecraftClient;

public class MainActivity extends Activity {
    private static final String TAG = "mcJava";
    
    static {
        System.loadLibrary("mcjava-native");
    }

    // Native methods for EGL management
    private native void nativeInit(Object activity, Object assetManager);
    private native void nativeResize(int width, int height);
    private native void nativeDestroy();
    private native void nativeOnTouch(int action, float x, float y, int pointerId);
    private native void nativeOnKey(int keyCode, int action);
    private native void nativeSwapBuffers();

    private boolean initialized = false;
    private MinecraftClient minecraftClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Landscape orientation for game
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        // Initialize Minecraft client
        minecraftClient = MinecraftClient.getInstance(this);
        minecraftClient.initialize();
        
        Log.i(TAG, "MainActivity onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "MainActivity onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "MainActivity onResume");
        
        if (!initialized) {
            nativeInit(this, getAssets());
            initialized = true;
        }
        
        minecraftClient.onResume();
        minecraftClient.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "MainActivity onPause");
        
        minecraftClient.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "MainActivity onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "MainActivity onDestroy");
        
        minecraftClient.stop();
        
        if (initialized) {
            nativeDestroy();
            initialized = false;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "Configuration changed: " + newConfig.orientation);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.i(TAG, "Window focus changed: " + hasFocus);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        // Pass to native for EGL handling
        nativeOnTouch(action, x, y, pointerId);
        
        // Pass to Minecraft client
        minecraftClient.onTouchEvent(action, x, y, pointerId);
        
        // Handle multi-touch
        if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP) {
            for (int i = 0; i < event.getPointerCount(); i++) {
                if (i != pointerIndex) {
                    int pid = event.getPointerId(i);
                    float px = event.getX(i);
                    float py = event.getY(i);
                    nativeOnTouch(action, px, py, pid);
                    minecraftClient.onTouchEvent(action, px, py, pid);
                }
            }
        }
        
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        nativeOnKey(keyCode, 0);
        minecraftClient.onKeyEvent(keyCode, 0);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        nativeOnKey(keyCode, 1);
        minecraftClient.onKeyEvent(keyCode, 1);
        return super.onKeyUp(keyCode, event);
    }
}