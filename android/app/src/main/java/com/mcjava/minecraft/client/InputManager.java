package com.mcjava.minecraft.client;

import android.util.Log;
import android.view.KeyEvent;

public class InputManager {
    private static final String TAG = "InputManager";
    
    // Key states
    private final boolean[] keys = new boolean[256];
    private final boolean[] keysPrev = new boolean[256];
    
    // Touch states
    private final TouchPoint[] touches = new TouchPoint[10];
    private final TouchPoint[] touchesPrev = new TouchPoint[10];
    
    // Mouse/look
    private float mouseX, mouseY;
    private float mouseDx, mouseDy;
    private float sensitivity = 0.5f;
    
    // Movement
    private boolean forward, backward, left, right, jump, sprint, sneak;
    
    public InputManager() {
        for (int i = 0; i < 10; i++) {
            touches[i] = new TouchPoint();
            touchesPrev[i] = new TouchPoint();
        }
        Log.i(TAG, "InputManager created");
    }
    
    public void update(float deltaTime) {
        // Copy current to previous
        System.arraycopy(keys, 0, keysPrev, 0, keys.length);
        for (int i = 0; i < 10; i++) {
            touchesPrev[i].set(touches[i]);
        }
        
        // Reset deltas
        mouseDx = mouseDy = 0;
    }
    
    public void onTouchEvent(int action, float x, float y, int pointerId) {
        if (pointerId < 0 || pointerId >= 10) return;
        
        switch (action) {
            case 0: // ACTION_DOWN
            case 5: // ACTION_POINTER_DOWN
                touches[pointerId].active = true;
                touches[pointerId].x = x;
                touches[pointerId].y = y;
                touches[pointerId].prevX = x;
                touches[pointerId].prevY = y;
                break;
            case 2: // ACTION_MOVE
                if (touches[pointerId].active) {
                    touches[pointerId].prevX = touches[pointerId].x;
                    touches[pointerId].prevY = touches[pointerId].y;
                    touches[pointerId].x = x;
                    touches[pointerId].y = y;
                    
                    // Calculate mouse delta for camera control
                    if (pointerId == 0) {
                        mouseDx += (x - touches[pointerId].prevX) * sensitivity;
                        mouseDy += (y - touches[pointerId].prevY) * sensitivity;
                    }
                }
                break;
            case 1: // ACTION_UP
            case 6: // ACTION_POINTER_UP
                touches[pointerId].active = false;
                break;
        }
    }
    
    public void onKeyEvent(int keyCode, int action) {
        if (keyCode >= 0 && keyCode < 256) {
            boolean pressed = (action == 0); // 0 = down
            keys[keyCode] = pressed;
            
            // Map keys to game actions
            switch (keyCode) {
                case KeyEvent.KEYCODE_W: forward = pressed; break;
                case KeyEvent.KEYCODE_S: backward = pressed; break;
                case KeyEvent.KEYCODE_A: left = pressed; break;
                case KeyEvent.KEYCODE_D: right = pressed; break;
                case KeyEvent.KEYCODE_SPACE: jump = pressed; break;
                case KeyEvent.KEYCODE_SHIFT_LEFT: sprint = pressed; break;
                case KeyEvent.KEYCODE_CTRL_LEFT: sneak = pressed; break;
            }
        }
    }
    
    // Queries
    public boolean isKeyDown(int keyCode) {
        return keyCode >= 0 && keyCode < 256 && keys[keyCode];
    }
    
    public boolean isKeyPressed(int keyCode) {
        return keyCode >= 0 && keyCode < 256 && keys[keyCode] && !keysPrev[keyCode];
    }
    
    public boolean isKeyReleased(int keyCode) {
        return keyCode >= 0 && keyCode < 256 && !keys[keyCode] && keysPrev[keyCode];
    }
    
    public boolean isTouchActive(int pointerId) {
        return pointerId >= 0 && pointerId < 10 && touches[pointerId].active;
    }
    
    public TouchPoint getTouch(int pointerId) {
        if (pointerId >= 0 && pointerId < 10) {
            return touches[pointerId];
        }
        return new TouchPoint();
    }
    
    public void getMouseDelta(float[] out) {
        out[0] = mouseDx;
        out[1] = mouseDy;
        mouseDx = mouseDy = 0;
    }
    
    // Movement queries
    public boolean isForward() { return forward; }
    public boolean isBackward() { return backward; }
    public boolean isLeft() { return left; }
    public boolean isRight() { return right; }
    public boolean isJump() { return jump; }
    public boolean isSprint() { return sprint; }
    public boolean isSneak() { return sneak; }
    
    public void setSensitivity(float sensitivity) {
        this.sensitivity = sensitivity;
    }
    
    public static class TouchPoint {
        public boolean active;
        public float x, y;
        public float prevX, prevY;
        public float pressure;
        
        public TouchPoint() {}
        
        public void set(TouchPoint other) {
            this.active = other.active;
            this.x = other.x;
            this.y = other.y;
            this.prevX = other.prevX;
            this.prevY = other.prevY;
            this.pressure = other.pressure;
        }
    }
}
