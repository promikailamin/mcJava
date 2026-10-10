package org.lwjgl.openal;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

/** AL 1.0 functions backed by the {@link mixer_state} software mixer. */
public final class AL10 {

    public static final int AL_NONE = 0;
    public static final int AL_FALSE = 0x0;
    public static final int AL_TRUE = 0x1;
    public static final int AL_SILENCE = 0x1000;
    public static final int AL_NO_ERROR = 0;
    public static final int AL_INVALID_ENUM = 0xA002;
    public static final int AL_INVALID_OPERATION = 0xA004;
    public static final int AL_INVALID_VALUE = 0xA003;
    public static final int AL_INVALID_NAME = 0xA001;
    public static final int AL_OUT_OF_MEMORY = 0xA005;

    public static final int AL_FORMAT_MONO8 = 0x1100;
    public static final int AL_FORMAT_MONO16 = 0x1101;
    public static final int AL_FORMAT_STEREO8 = 0x1102;
    public static final int AL_FORMAT_STEREO16 = 0x1103;

    public static final int AL_POSITION = 0x1004;
    public static final int AL_VELOCITY = 0x1006;
    public static final int AL_PITCH = 0x1003;
    public static final int AL_GAIN = 0x100A;
    public static final int AL_SOURCE_RELATIVE = 0x202;
    public static final int AL_LOOPING = 0x1007;
    public static final int AL_BUFFER = 0x1009;
    public static final int AL_SOURCE_STATE = 0x1010;
    public static final int AL_INITIAL = 0x1011;
    public static final int AL_PLAYING = 0x1012;
    public static final int AL_PAUSED = 0x1013;
    public static final int AL_STOPPED = 0x1014;
    public static final int AL_BUFFERS_QUEUED = 0x1015;
    public static final int AL_BUFFERS_PROCESSED = 0x1016;
    public static final int AL_REFERENCE_DISTANCE = 0x1020;
    public static final int AL_ROLLOFF_FACTOR = 0x1021;
    public static final int AL_MAX_DISTANCE = 0x1023;
    public static final int AL_ORIENTATION = 0x100F;
    public static final int AL_DOPPLER_FACTOR = 0xC000;
    public static final int AL_LISTENER_POSITION = 0x1004;
    public static final int AL_LISTENER_VELOCITY = 0x1006;
    public static final int AL_LISTENER_GAIN = 0x100A;
    public static final int AL_DISTANCE_MODEL = 0xD000;

    private AL10() {
    }

    // ---------------------------------------------------------------- misc

    public static void alEnable(int capability) { }
    public static void alDisable(int capability) { }
    public static int alGetError() { return 0; }
    public static ByteBuffer alGetString(int param) {
        return java.nio.charset.StandardCharsets.US_ASCII.encode(
                switch (param) {
                    case 0xB000 -> "OpenAL";
                    case 0xB001 -> "1.1";
                    case 0xB003 -> "opencode-soft";
                    default -> "";
                });
    }

    // ---------------------------------------------------------------- buffers

    public static void alGenBuffers(IntBuffer buffers) {
        for (int i = 0; i < buffers.remaining(); i++) {
            buffers.put(i, mixer_state.alloc_id());
        }
    }

    public static void alGenBuffers(int[] buffers) {
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = mixer_state.alloc_id();
        }
    }

    public static void alDeleteBuffers(IntBuffer buffers) {
        for (int i = 0; i < buffers.remaining(); i++) {
            mixer_state.buffer_deletes(new int[]{buffers.get(i)});
        }
    }

    public static void alDeleteBuffers(int[] buffers) {
        mixer_state.buffer_deletes(buffers);
    }

    public static void alBufferData(int buffer, int format, ByteBuffer data, int freq) {
        mixer_state.buffer_data(buffer, format, data, freq);
    }

    public static void alBufferData(int buffer, int format, Buffer data, int freq) {
        if (data instanceof ByteBuffer) {
            ByteBuffer b = (ByteBuffer) data;
            alBufferData(buffer, format, b, freq);
        }
    }

    // ---------------------------------------------------------------- sources

    public static void alGenSources(IntBuffer sources) {
        for (int i = 0; i < sources.remaining(); i++) {
            int id = mixer_state.alloc_id();
            sources.put(i, id);
            mixer_state.source_create(id);
        }
    }

    public static void alGenSources(int[] sources) {
        for (int i = 0; i < sources.length; i++) {
            int id = mixer_state.alloc_id();
            sources[i] = id;
            mixer_state.source_create(id);
        }
    }

    public static void alDeleteSources(IntBuffer sources) {
        for (int i = 0; i < sources.remaining(); i++) {
            mixer_state.source_delete(sources.get(i));
        }
    }

    public static void alDeleteSources(int[] sources) {
        for (int id : sources) {
            mixer_state.source_delete(id);
        }
    }

    public static void alSourceQueueBuffers(int source, IntBuffer buffers) {
        int[] ids = new int[buffers.remaining()];
        for (int i = 0; i < ids.length; i++) ids[i] = buffers.get(i);
        mixer_state.source_queue(source, ids);
    }

    public static void alSourceQueueBuffers(int source, int[] buffers) {
        mixer_state.source_queue(source, buffers);
    }

    public static void alSourceUnqueueBuffers(int source, IntBuffer buffers) {
        int[] out = new int[buffers.remaining()];
        mixer_state.source_unqueue(source, out);
        for (int i = 0; i < out.length; i++) buffers.put(i, out[i]);
    }

    public static void alSourceUnqueueBuffers(int source, int[] buffers) {
        mixer_state.source_unqueue(source, buffers);
    }

    // ---------------------------------------------------------------- params

    public static void alSourcef(int source, int param, float value) {
        mixer_state.source_state s = mixer_state.source(source);
        if (s == null) return;
        switch (param) {
            case AL_PITCH -> s.pitch = value;
            case AL_GAIN -> s.gain = value;
            case AL_REFERENCE_DISTANCE -> s.refDistance = value;
            case AL_MAX_DISTANCE -> s.maxDistance = value;
            case AL_ROLLOFF_FACTOR -> { }
        }
    }

    public static void alSourcei(int source, int param, int value) {
        mixer_state.source_state s = mixer_state.source(source);
        if (s == null) return;
        switch (param) {
            case AL_LOOPING -> s.looping = value;
            case AL_SOURCE_RELATIVE -> s.relative = value != 0;
            case AL_DISTANCE_MODEL -> { }
        }
    }

    public static void alSourcefv(int source, int param, FloatBuffer values) {
        if (param == AL_POSITION && values != null && values.remaining() >= 2) {
            mixer_state.source_state s = mixer_state.source(source);
            if (s != null) {
                // positional audio is played as-is by the software mixer
            }
        }
    }

    public static int alGetSourcei(int source, int param) {
        mixer_state.source_state s = mixer_state.source(source);
        if (s == null) return 0;
        return switch (param) {
            case AL_SOURCE_STATE -> s.state;
            case AL_BUFFERS_QUEUED -> s.queue.size();
            case AL_BUFFERS_PROCESSED -> mixer_state.source_processed(source);
            default -> 0;
        };
    }

    public static float alGetSourcef(int source, int param) {
        mixer_state.source_state s = mixer_state.source(source);
        if (s == null) return 0f;
        return switch (param) {
            case AL_PITCH -> s.pitch;
            case AL_GAIN -> s.gain;
            case AL_MAX_DISTANCE -> s.maxDistance;
            default -> 0f;
        };
    }

    // ---------------------------------------------------------------- playback

    public static void alSourcePlay(int source) { mixer_state.play(source); }
    public static void alSourcePause(int source) { mixer_state.pause(source); }
    public static void alSourceStop(int source) { mixer_state.stop(source); }

    // ---------------------------------------------------------------- listener

    public static void alListener3f(int param, float a, float b, float c) { }
    public static void alListenerfv(int param, FloatBuffer values) { }
    public static void alListenerf(int param, float value) { }
}