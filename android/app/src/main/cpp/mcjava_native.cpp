#include <jni.h>
#include <android/log.h>
#include <android/native_activity.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <cstring>

#define LOG_TAG "mcJava"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global state
static ANativeActivity* g_activity = nullptr;
static AAssetManager* g_asset_manager = nullptr;
static EGLDisplay g_egl_display = EGL_NO_DISPLAY;
static EGLSurface g_egl_surface = EGL_NO_SURFACE;
static EGLContext g_egl_context = EGL_NO_CONTEXT;
static bool g_initialized = false;

// Initialize EGL
bool initEGL(ANativeWindow* window) {
    g_egl_display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (g_egl_display == EGL_NO_DISPLAY) {
        LOGE("Failed to get EGL display");
        return false;
    }

    EGLint major, minor;
    if (!eglInitialize(g_egl_display, &major, &minor)) {
        LOGE("Failed to initialize EGL");
        return false;
    }

    const EGLint configAttribs[] = {
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT_KHR,
        EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 24,
        EGL_STENCIL_SIZE, 8,
        EGL_NONE
    };

    EGLConfig config;
    EGLint numConfigs;
    if (!eglChooseConfig(g_egl_display, configAttribs, &config, 1, &numConfigs)) {
        LOGE("Failed to choose EGL config");
        return false;
    }

    const EGLint contextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL_NONE
    };

    g_egl_context = eglCreateContext(g_egl_display, config, EGL_NO_CONTEXT, contextAttribs);
    if (g_egl_context == EGL_NO_CONTEXT) {
        LOGE("Failed to create EGL context");
        return false;
    }

    g_egl_surface = eglCreateWindowSurface(g_egl_display, config, window, nullptr);
    if (g_egl_surface == EGL_NO_SURFACE) {
        LOGE("Failed to create EGL surface");
        return false;
    }

    if (!eglMakeCurrent(g_egl_display, g_egl_surface, g_egl_surface, g_egl_context)) {
        LOGE("Failed to make EGL context current");
        return false;
    }

    LOGI("EGL initialized successfully (OpenGL ES %d.%d)", major, minor);
    return true;
}

void cleanupEGL() {
    if (g_egl_display != EGL_NO_DISPLAY) {
        eglMakeCurrent(g_egl_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (g_egl_surface != EGL_NO_SURFACE) {
            eglDestroySurface(g_egl_display, g_egl_surface);
            g_egl_surface = EGL_NO_SURFACE;
        }
        if (g_egl_context != EGL_NO_CONTEXT) {
            eglDestroyContext(g_egl_display, g_egl_context);
            g_egl_context = EGL_NO_CONTEXT;
        }
        eglTerminate(g_egl_display);
        g_egl_display = EGL_NO_DISPLAY;
    }
}

// JNI Functions
extern "C" JNIEXPORT void JNICALL
Java_com_mcjava_minecraft_MainActivity_nativeInit(JNIEnv* env, jobject /* this */, jobject activity, jobject assetManager) {
    g_activity = (ANativeActivity*)env->GetDirectBufferAddress(env->NewDirectByteBuffer(activity, 0));
    g_asset_manager = AAssetManager_fromJava(env, assetManager);
    
    if (!g_activity || !g_asset_manager) {
        LOGE("Failed to get activity or asset manager");
        return;
    }

    ANativeWindow* window = ANativeActivity_getWindow(g_activity);
    if (!window) {
        LOGE("Failed to get native window");
        return;
    }

    if (!initEGL(window)) {
        LOGE("Failed to initialize EGL");
        return;
    }

    // Initialize OpenGL state
    glViewport(0, 0, ANativeWindow_getWidth(window), ANativeWindow_getHeight(window));
    glClearColor(0.53f, 0.81f, 0.92f, 1.0f);
    
    g_initialized = true;
    LOGI("Native init complete");
}

extern "C" JNIEXPORT void JNICALL
Java_com_mcjava_minecraft_MainActivity_nativeResize(JNIEnv* env, jobject /* this */, jint width, jint height) {
    if (!g_initialized) return;
    
    glViewport(0, 0, width, height);
    LOGI("Resized to %dx%d", width, height);
}

extern "C" JNIEXPORT void JNICALL
Java_com_mcjava_minecraft_MainActivity_nativeSwapBuffers(JNIEnv* env, jobject /* this */) {
    if (!g_initialized) return;
    
    eglSwapBuffers(g_egl_display, g_egl_surface);
}

extern "C" JNIEXPORT void JNICALL
Java_com_mcjava_minecraft_MainActivity_nativeDestroy(JNIEnv* env, jobject /* this */) {
    cleanupEGL();
    g_initialized = false;
    LOGI("Native destroy complete");
}

extern "C" JNIEXPORT void JNICALL
Java_com_mcjava_minecraft_MainActivity_nativeOnTouch(JNIEnv* env, jobject /* this */, jint action, jfloat x, jfloat y, jint pointerId) {
    // Handle touch in native if needed
}

extern "C" JNIEXPORT void JNICALL
Java_com_mcjava_minecraft_MainActivity_nativeOnKey(JNIEnv* env, jobject /* this */, jint keyCode, jint action) {
    // Handle key in native if needed
}
