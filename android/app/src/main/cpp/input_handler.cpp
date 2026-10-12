#include "input_handler.h"
#include <android/log.h>
#include <cmath>

#define LOG_TAG "mcJava-Input"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace mcjava {

InputHandler::InputHandler() {
    reset();
}

InputHandler::~InputHandler() {}

void InputHandler::reset() {
    for (auto& touch : touches) {
        touch.active = false;
    }
    for (auto& key : keys) {
        key = false;
    }
    mouseX = mouseY = 0;
    mouseDx = mouseDy = 0;
    scrollX = scrollY = 0;
}

void InputHandler::onTouchDown(int pointerId, float x, float y) {
    if (pointerId >= 0 && pointerId < MAX_TOUCHES) {
        touches[pointerId] = {true, x, y, x, y, 1.0f};
        mouseX = x;
        mouseY = y;
    }
}

void InputHandler::onTouchMove(int pointerId, float x, float y) {
    if (pointerId >= 0 && pointerId < MAX_TOUCHES && touches[pointerId].active) {
        touches[pointerId].prevX = touches[pointerId].x;
        touches[pointerId].prevY = touches[pointerId].y;
        touches[pointerId].x = x;
        touches[pointerId].y = y;
        mouseX = x;
        mouseY = y;
        mouseDx = x - touches[pointerId].prevX;
        mouseDy = y - touches[pointerId].prevY;
    }
}

void InputHandler::onTouchUp(int pointerId, float x, float y) {
    if (pointerId >= 0 && pointerId < MAX_TOUCHES) {
        touches[pointerId] = {false, x, y, x, y, 0.0f};
    }
}

void InputHandler::onKeyDown(int keyCode) {
    if (keyCode >= 0 && keyCode < MAX_KEYS) {
        keys[keyCode] = true;
    }
}

void InputHandler::onKeyUp(int keyCode) {
    if (keyCode >= 0 && keyCode < MAX_KEYS) {
        keys[keyCode] = false;
    }
}

void InputHandler::onScroll(float x, float y) {
    scrollX += x;
    scrollY += y;
}

bool InputHandler::isKeyDown(int keyCode) const {
    return keyCode >= 0 && keyCode < MAX_KEYS && keys[keyCode];
}

bool InputHandler::isTouchActive(int pointerId) const {
    return pointerId >= 0 && pointerId < MAX_TOUCHES && touches[pointerId].active;
}

TouchPoint InputHandler::getTouch(int pointerId) const {
    if (pointerId >= 0 && pointerId < MAX_TOUCHES) {
        return touches[pointerId];
    }
    return {false, 0, 0, 0, 0, 0};
}

void InputHandler::getMouseDelta(float& dx, float& dy) const {
    dx = mouseDx;
    dy = mouseDy;
    mouseDx = mouseDy = 0;
}

void InputHandler::getScrollDelta(float& dx, float& dy) {
    dx = scrollX;
    dy = scrollY;
    scrollX = scrollY = 0;
}

void InputHandler::setScreenSize(int width, int height) {
    screenWidth = width;
    screenHeight = height;
}

} // namespace mcjava
