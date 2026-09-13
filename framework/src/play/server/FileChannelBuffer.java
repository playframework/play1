package play.server;

import io.netty.buffer.AbstractByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;

/**
 * Useless channel buffer only used to wrap the input stream....
 */
public class FileChannelBuffer extends AbstractByteBuf {

    private final FileInputStream is;

    public FileChannelBuffer(File file) {
        super(Integer.MAX_VALUE);
        if (file == null) {
            throw new NullPointerException("file");
        }
        try {
            this.is = new FileInputStream(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream getInputStream() {
        return is;
    }

    @Override
    public ByteBuf unwrap() {
        return null;
    }

    @Override
    public boolean hasMemoryAddress() {
        return false;
    }

    @Override
    public long memoryAddress() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBufAllocator alloc() {
        return UnpooledByteBufAllocator.DEFAULT;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ByteOrder order() {
        return ByteOrder.BIG_ENDIAN;
    }

    @Override
    public boolean isDirect() {
        return true;
    }

    @Override
    public boolean hasArray() {
        return false;
    }

    @Override
    public byte[] array() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int arrayOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int capacity() {
        try {
            return is.available();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ByteBuf capacity(int newCapacity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        byte[] b = new byte[length];
        is.read(b, index, length);
        ByteBuffer bb = ByteBuffer.wrap(b);
        return out.write(bb);
    }

    @Override
    public int getBytes(int index, FileChannel out, long position, int length) throws IOException {
        byte[] b = new byte[length];
        is.read(b, index, length);
        ByteBuffer bb = ByteBuffer.wrap(b);
        return out.write(bb, position);
    }

    @Override
    public ByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        byte[] b = new byte[length];
        is.read(b, index, length);
        out.write(b, index, length);
        return this;
    }

    @Override
    public ByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        try {
            byte[] b = new byte[length];
            is.read(b, index, length);
            System.arraycopy(b, 0, dst, dstIndex, length);
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        try {
            byte[] b = new byte[length];
            is.read(b, index, length);
            dst.writeBytes(b, dstIndex, length);
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ByteBuf getBytes(int index, ByteBuffer dst) {
        try {
            byte[] b = new byte[is.available() - index];
            is.read(b, index, is.available() - index);
            dst.put(b);
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ByteBuf duplicate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuf copy(int index, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuf slice(int index, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected byte _getByte(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected short _getShort(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected short _getShortLE(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int _getUnsignedMedium(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int _getUnsignedMediumLE(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int _getInt(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int _getIntLE(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected long _getLong(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected long _getLongLE(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _setByte(int index, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _setShort(int index, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _setShortLE(int index, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _setMedium(int index, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _setMediumLE(int index, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _setInt(int index, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _setIntLE(int index, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _setLong(int index, long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _setLongLE(int index, long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int refCnt() {
        return 1;
    }

    @Override
    public ByteBuf retain() {
        return this;
    }

    @Override
    public ByteBuf retain(int increment) {
        return this;
    }

    @Override
    public ByteBuf touch() {
        return this;
    }

    @Override
    public ByteBuf touch(Object hint) {
        return this;
    }

    @Override
    public boolean release() {
        return false;
    }

    @Override
    public boolean release(int decrement) {
        return false;
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setBytes(int index, FileChannel in, long position, int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuf setBytes(int index, ByteBuffer src) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuf setByte(int index, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int nioBufferCount() {
        return 0;
    }

    @Override
    public ByteBuffer nioBuffer(int index, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer[] nioBuffers(int index, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer internalNioBuffer(int index, int length) {
        throw new UnsupportedOperationException();
    }
}