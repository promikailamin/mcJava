package pro.minecraft.host;

import android.content.Context;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.lwjgl.sdl.SDLEvents;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLVideo;
import com.android.keymap;

/**
 * The Android "window": a {@link SurfaceView} whose surface the engine thread binds an
 * OpenGL ES 3.2 context to (via EGL, owned by {@link game_renderer}). Replaces SDL's
 * window/GL-context pair while keeping the game's SDL-facing API intact.
 *
 * Rendering is driven entirely from the engine thread: one Minecraft frame per
 * {@code eglSwapBuffers}.
 */
public final class game_view extends SurfaceView implements SurfaceHolder.Callback, game_input.Sink {

    private static final String TAG = "game_view";

    private final game_renderer renderer;
    private final game_input input;

    public game_view(Context context) {
        super(context);

        getHolder().addCallback(this);

        renderer = new game_renderer(this);
        input = new game_input(this);
        setOnTouchListener(input);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public game_renderer renderer() {
        return renderer;
    }

    public game_input input() {
        return input;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        SDLVideo.set_surface(holder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        float density = getResources().getDisplayMetrics().density;
        SDLVideo.updateDisplay(width, height, density);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        SDLVideo.set_surface(null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (input != null) {
            input.invalidate_size(w, h);
        }
        SDLVideo.updateDisplay(w, h, getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        return input.handle_key_down(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
        return input.handle_key_up(event);
    }

    // ---- snake_case input forwarding (game_input.Sink -> SDL_Event queue) ----

    private float cursorX;
    private float cursorY;

    @Override
    public void on_pointer_move(float x, float y) {
        cursorX += x;
        cursorY += y;
        SDL_Event e = SDL_Event.calloc();
        e.type(SDL_Event.SDL_EVENT_MOUSE_MOTION)
                .motion().x(cursorX).y(cursorY).xrel(x).yrel(y);
        SDLEvents.push(e);
    }

    @Override
    public void on_pointer_button(int button, boolean pressed) {
        SDL_Event e = SDL_Event.calloc();
        e.type(pressed ? SDL_Event.SDL_EVENT_MOUSE_BUTTON_DOWN : SDL_Event.SDL_EVENT_MOUSE_BUTTON_UP)
                .button().button(button == game_input.BUTTON_RIGHT ? 3 : 1).clicks(1);
        SDLEvents.push(e);
    }

    @Override
    public void on_pointer_scroll(float dx, float dy) {
        SDL_Event e = SDL_Event.calloc();
        e.type(SDL_Event.SDL_EVENT_MOUSE_WHEEL).wheel().x((double) dx).y((double) dy);
        SDLEvents.push(e);
    }

    @Override
    public void on_key(int keyCode, boolean pressed) {
        int scancode = keymap.android_to_scancode(keyCode);
        if (scancode == 0) {
            return;
        }
        SDLKeyboard.setScancode(scancode, pressed);
        SDL_Event e = SDL_Event.calloc();
        e.type(pressed ? SDL_Event.SDL_EVENT_KEY_DOWN : SDL_Event.SDL_EVENT_KEY_UP)
                .key().scancode(scancode).key(scancode).mod(SDLKeyboard.SDL_GetModState()).repeat(false);
        SDLEvents.push(e);
    }

    @Override
    public void on_char(char c) {
        SDL_Event e = SDL_Event.calloc();
        e.type(SDL_Event.SDL_EVENT_TEXT_INPUT).text().text(Character.toString(c));
        SDLEvents.push(e);
    }
}