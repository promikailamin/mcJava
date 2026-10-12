package com.mcjava.minecraft;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class McJavaApplication extends Application {
    private static final String TAG = "mcJava";
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        Log.i(TAG, "McJavaApplication onCreate");
        
        // Initialize any global state here
        System.setProperty("java.awt.headless", "true");
    }

    public static Context getContext() {
        return context;
    }
}
