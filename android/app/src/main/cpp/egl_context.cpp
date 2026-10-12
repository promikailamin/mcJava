#include "egl_context.h"
#include <android/log.h>
#include <EGL/egl.h>
#include <GLES3/gl3.h>

#define LOG_TAG "mcJava-EGL"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace mcjava {

EGLContextManager::EGLContextManager() 
    : display(EGL_NO_DISPLAY), surface(EGL_NO_SURFACE), context(EGL_NO_CONTEXT), initialized(false) {}

EGLContextManager::~EGLContextManager() {
    destroy();
}

bool EGLContextManager::initialize(ANativeWindow* window) {
    if (initialized) return true;

    display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display == EGL_NO_DISPLAY) {
        LOGE("Failed to get EGL display");
        return false;
    }

    EGLint major, minor;
    if (!eglInitialize(display, &major, &minor)) {
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
        EGL_SAMPLE_BUFFERS, 1,
        EGL_SAMPLES, 4,
        EGL_NONE
    };

    EGLConfig config;
    EGLint numConfigs;
    if (!eglChooseConfig(display, configAttribs, &config, 1, &numConfigs)) {
        LOGE("Failed to choose EGL config");
        return false;
    }

    const EGLint contextAttribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL_CONTEXT_OPENGL_DEBUG_KHR, EGL_TRUE,
        EGL_NONE
    };

    context = eglCreateContext(display, config, EGL_NO_CONTEXT, contextAttribs);
    if (context == EGL_NO_CONTEXT) {
        LOGE("Failed to create EGL context");
        return false;
    }

    surface = eglCreateWindowSurface(display, config, window, nullptr);
    if (surface == EGL_NO_SURFACE) {
        LOGE("Failed to create EGL surface");
        return false;
    }

    if (!eglMakeCurrent(display, surface, surface, context)) {
        LOGE("Failed to make EGL context current");
        return false;
    }

    // Enable debug output if available
    if (glDebugMessageCallback) {
        glEnable(GL_DEBUG_OUTPUT);
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        glDebugMessageCallback(debugCallback, nullptr);
    }

    initialized = true;
    LOGI("EGL context initialized (OpenGL ES %d.%d)", major, minor);
    return true;
}

void EGLContextManager::destroy() {
    if (display != EGL_NO_DISPLAY) {
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (surface != EGL_NO_SURFACE) {
            eglDestroySurface(display, surface);
            surface = EGL_NO_SURFACE;
        }
        if (context != EGL_NO_CONTEXT) {
            eglDestroyContext(display, context);
            context = EGL_NO_CONTEXT;
        }
        eglTerminate(display);
        display = EGL_NO_DISPLAY;
    }
    initialized = false;
}

void EGLContextManager::swapBuffers() {
    if (initialized) {
        eglSwapBuffers(display, surface);
    }
}

void EGLContextManager::makeCurrent() {
    if (initialized) {
        eglMakeCurrent(display, surface, surface, context);
    }
}

void EGLContextManager::resize(int width, int height) {
    if (initialized) {
        glViewport(0, 0, width, height);
    }
}

void EGLContextManager::debugCallback(GLenum source, GLenum type, GLuint id, GLenum severity, 
                                       GLsizei length, const GLchar* message, const void* userParam) {
    const char* typeStr = "";
    switch (type) {
        case GL_DEBUG_TYPE_ERROR: typeStr = "ERROR"; break;
        case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR: typeStr = "DEPRECATED"; break;
        case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR: typeStr = "UNDEFINED"; break;
        case GL_DEBUG_TYPE_PORTABILITY: typeStr = "PORTABILITY"; break;
        case GL_DEBUG_TYPE_PERFORMANCE: typeStr = "PERFORMANCE"; break;
        case GL_DEBUG_TYPE_MARKER: typeStr = "MARKER"; break;
        case GL_DEBUG_TYPE_PUSH_GROUP: typeStr = "PUSH_GROUP"; break;
        case GL_DEBUG_TYPE_POP_GROUP: typeStr = "POP_GROUP"; break;
        case GL_DEBUG_TYPE_OTHER: typeStr = "OTHER"; break;
    }

    const char* severityStr = "";
    switch (severity) {
        case GL_DEBUG_SEVERITY_HIGH: severityStr = "HIGH"; break;
        case GL_DEBUG_SEVERITY_MEDIUM: severityStr = "MEDIUM"; break;
        case GL_DEBUG_SEVERITY_LOW: severityStr = "LOW"; break;
        case GL_DEBUG_SEVERITY_NOTIFICATION: severityStr = "NOTIFICATION"; break;
    }

    if (severity != GL_DEBUG_SEVERITY_NOTIFICATION) {
        LOGE("OpenGL Debug [%s/%s]: %s", typeStr, severityStr, message);
    }
}

} // namespace mcjava
