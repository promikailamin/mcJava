# ProGuard rules for mcJava

# Keep Minecraft classes
-keep class net.minecraft.** { *; }
-keep class net.fabricmc.** { *; }
-keep class org.lwjgl.** { *; }
-keep class org.slf4j.** { *; }
-keep class com.google.gson.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JNI classes
-keep class com.mcjava.minecraft.MainActivity { *; }

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Optimize
-optimizationpasses 5
-allowaccessmodification
-mergelocalsaggressively

# Don't warn about missing classes
-dontwarn net.minecraft.**
-dontwarn net.fabricmc.**
-dontwarn org.lwjgl.**
-dontwarn org.slf4j.**
