package play.server;


import org.jboss.netty.buffer.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;


/**
 * Useless channel buffer only used to wrap the input stream....
 */
public class FileChannelBuffer extends AbstractChannelBuffer implements WrappedChannelBuffer {

    private final FileInputStream is;


    public FileChannelBuffer(File file) {
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
    public ChannelBuffer unwrap() {
        throw new RuntimeException();
    }

    @Override
    public ChannelBufferFactory factory() {
        throw new RuntimeException();
    }

    @Override
    public ByteOrder order() {
        throw new RuntimeException();
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
        throw new RuntimeException();
    }

    @Override
    public int arrayOffset() {
        throw new RuntimeException();
    }

    @Override
    public void discardReadBytes() {
        throw new RuntimeException();
    }

    public void setByte(int index, byte value) {
        throw new RuntimeException();
    }

    @Override
    public void setBytes(int index, ChannelBuffer src, int srcIndex, int length) {
        throw new RuntimeException();
    }

    @Override
    public void setBytes(int index, byte[] src, int srcIndex, int length) {
        throw new RuntimeException();
    }

    @Override
    public void setBytes(int index, ByteBuffer src) {
        throw new RuntimeException();
    }

    public void setShort(int index, short value) {
        throw new RuntimeException();
    }

    @Override
    public void setMedium(int index, int value) {
        throw new RuntimeException();
    }

    @Override
    public void setInt(int index, int value) {
        throw new RuntimeException();
    }

    @Override
    public void setLong(int index, long value) {
        throw new RuntimeException();
    }

    @Override
    public int setBytes(int index, InputStream in, int length)
            throws IOException {
        throw new RuntimeException();
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length)
            throws IOException {
        throw new RuntimeException();

    }

    @Override
    public int readerIndex() {
        return 0;
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length)
            throws IOException {
        byte[] b = new byte[length];
        is.read(b, index, length);
        ByteBuffer bb = ByteBuffer.wrap(b);
        return out.write(bb);
    }

    @Override
    public void setByte(int i, int i1) {
        throw new RuntimeException();
    }

    @Override
    public void getBytes(int index, OutputStream out, int length)
            throws IOException {
        byte[] b = new byte[length];
        is.read(b, index, length);
        out.write(b, index, length);
    }

    @Override
    public void getBytes(int index, byte[] dst, int dstIndex, int length) {
        try {
            byte[] b = new byte[length];
            is.read(b, index, length);
            System.arraycopy(b, 0, dst, dstIndex, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void getBytes(int index, ChannelBuffer dst, int dstIndex, int length) {
        try {
            byte[] b = new byte[length];
            is.read(b, index, length);
            dst.writeBytes(b, dstIndex, length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void getBytes(int index, ByteBuffer dst) {
        try {
            byte[] b = new byte[is.available() - index];
            is.read(b, index, is.available() - index);
            dst.put(b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ChannelBuffer duplicate() {
        throw new RuntimeException();
    }

    @Override
    public ChannelBuffer copy(int index, int length) {
        throw new RuntimeException();
    }

    @Override
    public ChannelBuffer slice(int index, int length) {
        throw new RuntimeException();
    }

    @Override
    public byte getByte(int index) {
//        try {
//            byte[] b = new byte[1];
//            is.read(b, index, 1);
//            return b[0];
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        throw new RuntimeException();
    }

    @Override
    public short getShort(int index) {
        throw new RuntimeException();
    }

    @Override
    public int getUnsignedMedium(int index) {
        throw new RuntimeException();

    }

    @Override
    public int getInt(int index) {
        throw new RuntimeException();

    }

    @Override
    public long getLong(int index) {
        throw new RuntimeException();

    }

    @Override
    public ByteBuffer toByteBuffer(int index, int length) {
        throw new RuntimeException();
    }

    @Override
    public ByteBuffer[] toByteBuffers(int index, int length) {
//        ByteBuffer[] bufs = buffer.toByteBuffers(index, length);
//        for (int i = 0; i < bufs.length; i++) {
//            bufs[i] = bufs[i].asReadOnlyBuffer();
//        }
//        return bufs;
        throw new RuntimeException();
    }

    @Override
    public int capacity() {
        try {
            return is.available();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public ChannelBuffer readBytes(int length) {
//          ChannelBuffer buf = ChannelBuffers.buffer(length);
//          getBytes(0, buf);
//          return buf;
        throw new RuntimeException();
    }

    public ChannelBuffer readBytes(ChannelBufferIndexFinder endIndexFinder) {
        throw new RuntimeException();
    }

    @Override
    public ChannelBuffer readSlice(int length) {
        throw new RuntimeException();
    }

    public ChannelBuffer readSlice(ChannelBufferIndexFinder endIndexFinder) {
        throw new RuntimeException();
    }

    @Override
    public void readBytes(byte[] dst, int dstIndex, int length) {
        checkReadableBytes(length);
        getBytes(0, dst, dstIndex, length);
    }

    @Override
    public void readBytes(byte[] dst) {
        readBytes(dst, 0, dst.length);
    }

    @Override
    public void readBytes(ChannelBuffer dst) {
        readBytes(dst, dst.writableBytes());
    }

    @Override
    public void readBytes(ChannelBuffer dst, int length) {
        if (length > dst.writableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        readBytes(dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
    }

    @Override
    public void readBytes(ChannelBuffer dst, int dstIndex, int length) {
        getBytes(0, dst, dstIndex, length);
    }

    @Override
    public void readBytes(ByteBuffer dst) {
        int length = dst.remaining();
        checkReadableBytes(length);
        getBytes(0, dst);
    }

    @Override
    public int readBytes(GatheringByteChannel out, int length)
            throws IOException {
        checkReadableBytes(length);
        int readBytes = getBytes(0, out, length);
        return readBytes;
    }

    @Override
    public void readBytes(OutputStream out, int length) throws IOException {
        checkReadableBytes(length);
        getBytes(0, out, length);
    }

    public String toString(int q, int a, java.lang.String b) {
        throw new RuntimeException();
    }

    @Override
    public void setShort(int a, int b) {
        throw new RuntimeException();
    }
}
