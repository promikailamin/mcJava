#pragma once

#include <EGL/egl.h>
#include <android/native_window.h>
#include <GLES3/gl3.h>

namespace mcjava {

class EGLContextManager {
public:
    EGLContextManager();
    ~EGLContextManager();

    bool initialize(ANativeWindow* window);
    void destroy();
    void swapBuffers();
    void makeCurrent();
    void resize(int width, int height);

    bool isInitialized() const { return initialized; }
    EGLDisplay getDisplay() const { return display; }
    EGLContext getContext() const { return context; }
    EGLSurface getSurface() const { return surface; }

private:
    static void APIENTRY debugCallback(GLenum source, GLenum type, GLuint id, GLenum severity,
                                       GLsizei length, const GLchar* message, const void* userParam);

    EGLDisplay display;
    EGLSurface surface;
    EGLContext context;
    bool initialized;
};

} // namespace mcjava
