package org.lwjgl.openal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

/**
 * The port's OpenAL replacement: a small JVM sample-mixer that plays the PCM buffers
 * the game uploads (via {@code alBufferData}) straight to an {@link AudioTrack}.
 *
 * Sources/buffers mirror AL semantics; a daemon thread mixes all playing sources into
 * a 48 kHz stereo stream, resampling each source (and pitch) on the fly.
 */
final class mixer_state {

    private static final String TAG = "openal";

    static final int FORMAT_MONO8 = 0x1100;
    static final int FORMAT_MONO16 = 0x1101;
    static final int FORMAT_STEREO8 = 0x1102;
    static final int FORMAT_STEREO16 = 0x1103;

    static final int MASTER_RATE = 48000;

    private static final class buffer_state {
        float[] left;
        float[] right;
        int rate;
        int frameCount;
        int format;
    }

    static final class source_state {
        int state = 0x1011; // AL_INITIAL
        float pitch = 1.0f;
        float gain = 1.0f;
        float refDistance = 1.0f;
        float maxDistance = Float.MAX_VALUE;
        int looping = 0x1011; // 0 = AL_FALSE in old AL: 0x0
        boolean relative;
        final ArrayDeque<Integer> queue = new ArrayDeque<>();
        long framePos;
        buffer_state current;
        float subPos;
    }

    private static final Map<Integer, buffer_state> BUFFERS = new HashMap<>();
    private static final Map<Integer, source_state> SOURCES = new HashMap<>();
    private static final AtomicLong NEXT = new AtomicLong(1L);

    private static volatile boolean running;
    private static volatile AudioTrack track;
    private static final Object MIX_LOCK = new Object();

    private mixer_state() {
    }

    static synchronized void ensure_started() {
        if (running) {
            return;
        }
        running = true;
        Thread mixer = new Thread(mixer_state::mixLoop, "openal-mixer");
        mixer.setDaemon(true);
        mixer.start();
    }

    static synchronized void shutdown() {
        running = false;
    }

    static int alloc_id() {
        return (int) NEXT.getAndIncrement();
    }

    // ---------------------------------------------------------------- buffers

    static void buffer_data(int buffer, int format, ByteBuffer data, int freq) {
        buffer_state b = new buffer_state();
        b.format = format;
        b.rate = Math.max(8000, freq);
        int bytesPerFrame = frame_bytes(format);
        b.frameCount = bytesPerFrame == 0 ? 0 : data.remaining() / bytesPerFrame;
        b.left = new float[b.frameCount];
        b.right = b.left;
        boolean stereo = format == FORMAT_STEREO8 || format == FORMAT_STEREO16;
        if (stereo) {
            b.right = new float[b.frameCount];
        }
        data.order(ByteOrder.LITTLE_ENDIAN);
        data.rewind();
        for (int i = 0; i < b.frameCount; i++) {
            float l;
            float r;
            switch (format) {
                case FORMAT_MONO8 -> { l = r = (data.get() - 128) / 128f; }
                case FORMAT_STEREO8 -> { l = (data.get() - 128) / 128f; r = (data.get() - 128) / 128f; }
                case FORMAT_MONO16 -> { l = r = data.getShort() / 32768f; }
                default -> { l = data.getShort() / 32768f; r = data.getShort() / 32768f; }
            }
            b.left[i] = l;
            b.right[i] = r;
        }
        data.rewind();
        synchronized (BUFFERS) {
            BUFFERS.put(buffer, b);
        }
    }

    static void buffer_deletes(int[] ids) {
        synchronized (BUFFERS) {
            for (int id : ids) BUFFERS.remove(id);
        }
    }

    private static int frame_bytes(int format) {
        return switch (format) {
            case FORMAT_MONO8, FORMAT_MONO16 -> 1;
            case FORMAT_STEREO8, FORMAT_STEREO16 -> 2;
            default -> 0;
        };
    }

    // ---------------------------------------------------------------- sources

    static source_state source(int id) {
        return SOURCES.get(id);
    }

    static void source_create(int id) {
        synchronized (SOURCES) {
            SOURCES.put(id, new source_state());
        }
    }

    static void source_delete(int id) {
        synchronized (SOURCES) {
            SOURCES.remove(id);
        }
    }

    static void source_queue(int source, int[] buffers) {
        source_state s = SOURCES.get(source);
        synchronized (s.queue) {
            for (int b : buffers) s.queue.add(b);
            int head = s.queue.peek();
            if (head != 0) {
                synchronized (BUFFERS) {
                    s.current = BUFFERS.get(head);
                }
                s.framePos = 0;
                s.subPos = 0;
            }
        }
    }

    static void source_unqueue(int source, int[] out) {
        source_state s = SOURCES.get(source);
        synchronized (s.queue) {
            int n = Math.min(out.length, s.queue.size());
            for (int i = 0; i < n; i++) out[i] = s.queue.poll();
            int head = s.queue.peek();
            if (head != 0 && head != (out.length > 0 ? out[0] : 0)) {
                synchronized (BUFFERS) {
                    s.current = BUFFERS.get(head);
                }
                s.framePos = 0;
                s.subPos = 0;
            }
        }
    }

    static int source_processed(int source) {
        source_state s = SOURCES.get(source);
        if (s == null || s.state != 0x1014) return 0;
        synchronized (s.queue) {
            return s.queue.isEmpty() ? 0 : 1;
        }
    }

    static void play(int source) {
        source_state s = SOURCES.get(source);
        if (s == null) return;
        synchronized (s.queue) {
            if (s.queue.isEmpty()) {
                Log.w(TAG, "play with no queued buffers");
                s.state = 0x1011;
                return;
            }
            if (s.state == 0x1012) return;
            if (s.current == null) {
                synchronized (BUFFERS) {
                    s.current = BUFFERS.get(s.queue.peek());
                }
            }
            s.state = 0x1012; // AL_PLAYING
            ensure_started();
        }
    }

    static void stop(int source) {
        source_state s = SOURCES.get(source);
        if (s == null) return;
        s.state = 0x1014; // AL_STOPPED
        s.framePos = 0;
        s.subPos = 0;
    }

    static void pause(int source) {
        source_state s = SOURCES.get(source);
        if (s == null) return;
        if (s.state == 0x1012) {
            s.state = 0x1013; // AL_PAUSED
        }
    }

    // ---------------------------------------------------------------- mixer

    private static void mixLoop() {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        AudioFormat fmt = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(MASTER_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build();
        try {
            track = new AudioTrack(attrs, fmt, MASTER_RATE * 4, AudioTrack.MODE_STREAM,
                    AudioTrack.getMinBufferSize(MASTER_RATE, AudioFormat.CHANNEL_OUT_STEREO,
                            AudioFormat.ENCODING_PCM_16BIT));
            track.play();
        } catch (RuntimeException e) {
            Log.w(TAG, "AudioTrack init failed", e);
            running = false;
            return;
        }

        float[] mix = new float[MASTER_RATE / 30 * 2];
        short[] out = new short[mix.length];
        while (running) {
            synchronized (MIX_LOCK) {
                int produced = render(mix, mix.length / 2);
                for (int i = 0; i < produced; i++) {
                    float v = (float) Math.max(-1.0, Math.min(1.0, mix[i]));
                    out[i] = (short) (v * 32767);
                }
                if (produced > 0) {
                    track.write(out, 0, produced);
                }
            }
            try {
                Thread.sleep(8);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        track.release();
    }

    private static int render(float[] mix, int frames) {
        int slots = frames * 2;
        java.util.Arrays.fill(mix, 0, Math.min(slots, mix.length), 0f);
        int produced = 0;
        float master = 0.9f;
        source_state[] snapshot;
        synchronized (SOURCES) {
            snapshot = SOURCES.values().toArray(new source_state[0]);
        }
        for (source_state s : snapshot) {
            if (s.state != 0x1012) continue;
            float[] srcL;
            float[] srcR;
            int rate;
            synchronized (s.queue) {
                if (s.current == null) {
                    s.state = 0x1014;
                    continue;
                }
                srcL = s.current.left;
                srcR = s.current.right;
                rate = s.current.rate;
            }
            float step = (float) rate * s.pitch / MASTER_RATE;
            int n = srcL.length;
            for (int i = 0; i < frames; i++) {
                if (s.subPos >= n) {
                    if (s.looping == 1) {
                        s.subPos -= n;
                    } else {
                        s.state = 0x1014;
                        break;
                    }
                }
                int idx = (int) s.subPos;
                int idx2 = Math.min(idx + 1, n - 1);
                float frac = s.subPos - idx;
                float l = srcL[idx] + (srcL[idx2] - srcL[idx]) * frac;
                float r = (srcR == srcL) ? l : srcR[idx] + (srcR[idx2] - srcR[idx]) * frac;
                mix[i * 2] += l * s.gain * master;
                mix[i * 2 + 1] += r * s.gain * master;
                s.subPos += step;
            }
            produced = frames;
        }
        return frames * 2;
    }
}