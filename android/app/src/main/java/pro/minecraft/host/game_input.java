package pro.minecraft.host;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * Translates Android touch/pointer input into the synthetic mouse API the game's
 * windowing layer consumes (through {@code org.lwjgl.sdl} in the shim).
 *
 * Scheme:
 *  - 1 finger : look (drag) + left click (tap, no drag)
 *  - 2 fingers: right click (start) / stored for scroll
 */
public final class game_input implements View.OnTouchListener {

    public static final int BUTTON_LEFT = 1;
    public static final int BUTTON_RIGHT = 2;
    public static final int BUTTON_MIDDLE = 3;

    public interface Sink {
        void on_pointer_move(float x, float y);
        void on_pointer_button(int button, boolean pressed);
        void on_pointer_scroll(float dx, float dy);
        void on_key(int keyCode, boolean pressed);
        void on_char(char c);
    }

    private final Sink sink;

    private int width;
    private int height;

    private float lastX;
    private float lastY;
    private long lastActionTime;
    private static final long CLICK_THRESHOLD_MS = 250;
    private float dragDistance;

    public game_input(Sink sink) {
        this.sink = sink;
    }

    public void invalidate_size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean handle_key_down(KeyEvent event) {
        sink.on_key(event.getKeyCode(), true);
        int unicode = event.getUnicodeChar(event.getMetaState());
        if (unicode != 0) {
            sink.on_char((char) unicode);
        }
        return true;
    }

    public boolean handle_key_up(KeyEvent event) {
        sink.on_key(event.getKeyCode(), false);
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        int pointerCount = event.getPointerCount();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                lastX = event.getX(0);
                lastY = event.getY(0);
                dragDistance = 0f;
                lastActionTime = System.currentTimeMillis();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (pointerCount >= 2) {
                    sink.on_pointer_button(BUTTON_LEFT, false);
                    sink.on_pointer_button(BUTTON_RIGHT, false);
                    return true;
                }
                float x = event.getX(0);
                float y = event.getY(0);
                float dx = x - lastX;
                float dy = y - lastY;
                dragDistance += Math.abs(dx) + Math.abs(dy);
                lastX = x;
                lastY = y;
                sink.on_pointer_move(dx, dy);
                return true;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (pointerCount == 2) {
                    sink.on_pointer_button(BUTTON_RIGHT, true);
                }
                return true;
            }
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP: {
                if (pointerCount <= 1) {
                    sink.on_pointer_button(BUTTON_LEFT, false);
                }
                if (action == MotionEvent.ACTION_UP && !wasDrag() && pointerCount == 1) {
                    sink.on_pointer_button(BUTTON_LEFT, true);
                    sink.on_pointer_button(BUTTON_LEFT, false);
                }
                sink.on_pointer_button(BUTTON_RIGHT, false);
                return true;
            }
            case MotionEvent.ACTION_CANCEL: {
                sink.on_pointer_button(BUTTON_LEFT, false);
                sink.on_pointer_button(BUTTON_RIGHT, false);
                return true;
            }
            case MotionEvent.ACTION_SCROLL: {
                float dy = event.getAxisValue(MotionEvent.AXIS_SCROLL);
                if (dy != 0f) {
                    sink.on_pointer_scroll(0f, dy);
                }
                return true;
            }
            default:
                return true;
        }
    }

    private boolean wasDrag() {
        return dragDistance > 24f
                || System.currentTimeMillis() - lastActionTime < CLICK_THRESHOLD_MS && dragDistance > 4f;
    }
}