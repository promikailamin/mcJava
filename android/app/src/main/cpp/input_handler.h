#pragma once

#include <cstdint>

namespace mcjava {

struct TouchPoint {
    bool active;
    float x, y;
    float prevX, prevY;
    float pressure;
};

class InputHandler {
public:
    static constexpr int MAX_TOUCHES = 10;
    static constexpr int MAX_KEYS = 256;

    InputHandler();
    ~InputHandler();

    void reset();

    // Touch events
    void onTouchDown(int pointerId, float x, float y);
    void onTouchMove(int pointerId, float x, float y);
    void onTouchUp(int pointerId, float x, float y);

    // Key events
    void onKeyDown(int keyCode);
    void onKeyUp(int keyCode);

    // Scroll
    void onScroll(float x, float y);

    // Queries
    bool isKeyDown(int keyCode) const;
    bool isTouchActive(int pointerId) const;
    TouchPoint getTouch(int pointerId) const;
    void getMouseDelta(float& dx, float& dy) const;
    void getScrollDelta(float& dx, float& dy);

    void setScreenSize(int width, int height);

private:
    TouchPoint touches[MAX_TOUCHES];
    bool keys[MAX_KEYS];
    float mouseX, mouseY;
    float mouseDx, mouseDy;
    float scrollX, scrollY;
    int screenWidth, screenHeight;
};

} // namespace mcjava
