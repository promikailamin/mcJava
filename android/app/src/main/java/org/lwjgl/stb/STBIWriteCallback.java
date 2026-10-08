package org.lwjgl.stb;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import org.lwjgl.system.MemoryUtil;

/** Callback sink for PNG writers ({@code stbi_write_png_to_func}). */
public abstract class STBIWriteCallback {

    private static final Map<Long, STBIWriteCallback> REGISTRY = new ConcurrentHashMap<>();

    private final long address;

    protected STBIWriteCallback() {
        this.address = 0x80000L + System.nanoTime() % 0x7FFFFF;
        REGISTRY.put(this.address, this);
    }

    public abstract void invoke(long context, long data, int size);

    public static STBIWriteCallback create(STBIWriteCallback callback) {
        return callback;
    }

    public static ByteBuffer getData(long data, int size) {
        return MemoryUtil.memByteBuffer(data, Math.max(0, size));
    }

    public long address() {
        return address;
    }

    static STBIWriteCallback byAddress(long address) {
        return REGISTRY.get(address);
    }

    /** Pure-Java PNG encoder feeding chunks through the write callback. */
    public void writePng(int width, int height, int comp, long data, int stride) {
        int bpp = comp;
        byte[] raw = new byte[(width * bpp + 1) * height];
        ByteBuffer src = MemoryUtil.memByteBuffer(data, stride * height);
        int o = 0;
        for (int y = 0; y < height; y++) {
            raw[o++] = 0; // filter: none
            for (int x = 0; x < width * bpp; x++) {
                raw[o++] = src.get(y * stride + x);
            }
        }

        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        deflater.setInput(raw);
        deflater.finish();
        byte[] compressed = new byte[raw.length + (raw.length >> 3) + 512];
        int compLen = deflater.deflate(compressed);
        deflater.end();

        ByteBuffer ihdr = ByteBuffer.allocate(13);
        ihdr.putInt(width);
        ihdr.putInt(height);
        ihdr.put((byte) 8);           // bit depth
        ihdr.put(png_color_type(comp));
        ihdr.put((byte) 0);           // compression
        ihdr.put((byte) 0);           // filter
        ihdr.put((byte) 0);           // interlace
        ihdr.flip();

        emit("\u0089PNG\r\n\u001a\n".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        chunk("IHDR", ihdr);
        chunk("IDAT", java.nio.ByteBuffer.wrap(compressed, 0, compLen));
        chunk("IEND", ByteBuffer.allocate(0));
    }

    private void emit(byte[] bytes) {
        long ptr = MemoryUtil.nalloc(bytes.length);
        MemoryUtil.memPutBytes(ptr, bytes, 0, bytes.length);
        invoke(0L, ptr, bytes.length);
        MemoryUtil.nfree(ptr);
    }

    private void chunk(String type, ByteBuffer payload) {
        byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] payloadBytes = new byte[payload.remaining()];
        payload.duplicate().get(payloadBytes);

        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(payloadBytes);

        ByteBuffer header = ByteBuffer.allocate(payloadBytes.length + 8);
        header.putInt(payloadBytes.length);
        header.put(typeBytes);
        header.put(payloadBytes);
        header.flip();
        emitN(header.array());

        ByteBuffer crcBuf = ByteBuffer.allocate(4);
        crcBuf.putInt((int) crc.getValue());
        crcBuf.flip();
        emitN(crcBuf.array());
    }

    private void emitN(byte[] bytes) {
        long ptr = MemoryUtil.nalloc(bytes.length);
        MemoryUtil.memPutBytes(ptr, bytes, 0, bytes.length);
        invoke(0L, ptr, bytes.length);
        MemoryUtil.nfree(ptr);
    }

    private byte png_color_type(int comp) {
        return switch (comp) {
            case 1 -> 0;  // grayscale
            case 2 -> 4;  // gray+alpha
            case 3 -> 2;  // rgb
            default -> 6; // rgba
        };
    }
}