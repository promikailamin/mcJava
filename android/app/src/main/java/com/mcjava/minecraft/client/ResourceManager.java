package com.mcjava.minecraft.client;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class ResourceManager {
    private static final String TAG = "ResourceManager";
    
    private final Context context;
    private final AssetManager assetManager;
    
    // Caches
    private final Map<String, Integer> textureCache = new HashMap<>();
    private final Map<String, Bitmap> bitmapCache = new HashMap<>();
    private final Map<String, String> textCache = new HashMap<>();
    
    public ResourceManager(Context context) {
        this.context = context.getApplicationContext();
        this.assetManager = context.getAssets();
        Log.i(TAG, "ResourceManager created");
    }
    
    public int loadTexture(String path) {
        // Check cache
        Integer cached = textureCache.get(path);
        if (cached != null) {
            return cached;
        }
        
        try {
            // Load bitmap
            Bitmap bitmap = loadBitmap(path);
            if (bitmap == null) {
                return 0;
            }
            
            // Generate texture
            int[] textureId = new int[1];
            GLES30.glGenTextures(1, textureId, 0);
            
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId[0]);
            
            // Set texture parameters
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);
            
            // Upload bitmap
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
            
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
            
            // Cache
            textureCache.put(path, textureId[0]);
            
            Log.d(TAG, "Loaded texture: " + path + " (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");
            
            return textureId[0];
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to load texture: " + path, e);
            return 0;
        }
    }
    
    public Bitmap loadBitmap(String path) {
        // Check cache
        Bitmap cached = bitmapCache.get(path);
        if (cached != null) {
            return cached;
        }
        
        try (InputStream is = assetManager.open(path)) {
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                bitmapCache.put(path, bitmap);
            }
            return bitmap;
        } catch (IOException e) {
            Log.e(TAG, "Failed to load bitmap: " + path, e);
            return null;
        }
    }
    
    public String loadText(String path) {
        // Check cache
        String cached = textCache.get(path);
        if (cached != null) {
            return cached;
        }
        
        try (InputStream is = assetManager.open(path)) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            String text = new String(buffer, "UTF-8");
            textCache.put(path, text);
            return text;
        } catch (IOException e) {
            Log.e(TAG, "Failed to load text: " + path, e);
            return null;
        }
    }
    
    public ByteBuffer loadBinary(String path) {
        try (InputStream is = assetManager.open(path)) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            ByteBuffer bb = ByteBuffer.allocateDirect(buffer.length).order(ByteOrder.nativeOrder());
            bb.put(buffer);
            bb.position(0);
            return bb;
        } catch (IOException e) {
            Log.e(TAG, "Failed to load binary: " + path, e);
            return null;
        }
    }
    
    public void deleteTexture(String path) {
        Integer textureId = textureCache.remove(path);
        if (textureId != null) {
            GLES30.glDeleteTextures(1, new int[]{textureId}, 0);
        }
    }
    
    public void clearCache() {
        // Delete all textures
        for (Integer textureId : textureCache.values()) {
            GLES30.glDeleteTextures(1, new int[]{textureId}, 0);
        }
        textureCache.clear();
        bitmapCache.clear();
        textCache.clear();
        Log.i(TAG, "Resource cache cleared");
    }
    
    public boolean hasResource(String path) {
        try {
            assetManager.open(path).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    public String[] listResources(String path) {
        try {
            return assetManager.list(path);
        } catch (IOException e) {
            Log.e(TAG, "Failed to list resources: " + path, e);
            return new String[0];
        }
    }
}
