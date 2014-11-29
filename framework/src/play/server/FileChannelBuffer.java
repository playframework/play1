package play.server;


import io.netty.buffer.AbstractByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;


/**
 * Useless channel buffer only used to wrap the input stream....
 */
public class FileChannelBuffer extends AbstractByteBuf /* implements WrappedChannelBuffer */ {

    private final FileInputStream is;


    public FileChannelBuffer(File file) {
    	super(8024);
    	
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

    public ByteBuf unwrap() {
        throw new RuntimeException();
    }

//    public ChannelBufferFactory factory() {
//        throw new RuntimeException();
//    }

    public ByteOrder order() {
        throw new RuntimeException();
    }

    public boolean isDirect() {
        return true;
    }

    public boolean hasArray() {
        return false;
    }

    public byte[] array() {
        throw new RuntimeException();
    }

    public int arrayOffset() {
        throw new RuntimeException();
    }

    @Override
    public ByteBuf discardReadBytes() {
        throw new RuntimeException();
    }

    public void setByte(int index, byte value) {
        throw new RuntimeException();
    }

    public ByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        throw new RuntimeException();
    }

    public ByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        throw new RuntimeException();
    }

    public ByteBuf setBytes(int index, ByteBuffer src) {
        throw new RuntimeException();
    }

    public void setShort(int index, short value) {
        throw new RuntimeException();
    }

    public ByteBuf setMedium(int index, int value) {
        throw new RuntimeException();
    }

    public ByteBuf setInt(int index, int value) {
        throw new RuntimeException();
    }

    public ByteBuf setLong(int index, long value) {
        throw new RuntimeException();
    }

    public int setBytes(int index, InputStream in, int length)
            throws IOException {
        throw new RuntimeException();
    }

    public int setBytes(int index, ScatteringByteChannel in, int length)
            throws IOException {
        throw new RuntimeException();

    }

    public int readerIndex() {
        return 0;
    }


    public int getBytes(int index, GatheringByteChannel out, int length)
            throws IOException {
        byte[] b = new byte[length];
        is.read(b, index, length);
        ByteBuffer bb = ByteBuffer.wrap(b);
        return out.write(bb);
    }

    public ByteBuf setByte(int i, int i1) {
        throw new RuntimeException();
    }

    public ByteBuf getBytes(int index, OutputStream out, int length)
            throws IOException {
        byte[] b = new byte[length];
        is.read(b, index, length);
        out.write(b, index, length);
        return this;
    }

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

    public ByteBuf duplicate() {
        throw new RuntimeException();
    }

    public ByteBuf copy(int index, int length) {
        throw new RuntimeException();
    }

    public ByteBuf slice(int index, int length) {
        throw new RuntimeException();
    }

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

    public short getShort(int index) {
        throw new RuntimeException();
    }

    public int getUnsignedMedium(int index) {
        throw new RuntimeException();

    }

    public int getInt(int index) {
        throw new RuntimeException();

    }

    public long getLong(int index) {
        throw new RuntimeException();

    }

    public ByteBuffer toByteBuffer(int index, int length) {
        throw new RuntimeException();
    }

//    @Override
//    public ByteBuffer[] toByteBuffers(int index, int length) {
////        ByteBuffer[] bufs = buffer.toByteBuffers(index, length);
////        for (int i = 0; i < bufs.length; i++) {
////            bufs[i] = bufs[i].asReadOnlyBuffer();
////        }
////        return bufs;
//        throw new RuntimeException();
//    }

    public int capacity() {
        try {
            return is.available();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public ByteBuf readBytes(int length) {
//          ChannelBuffer buf = ChannelBuffers.buffer(length);
//          getBytes(0, buf);
//          return buf;
        throw new RuntimeException();
    }

//    public ByteBuf readBytes(ChannelBufferIndexFinder endIndexFinder) {
//        throw new RuntimeException();
//    }

    public ByteBuf readSlice(int length) {
        throw new RuntimeException();
    }

//    public ByteBuf readSlice(ChannelBufferIndexFinder endIndexFinder) {
//        throw new RuntimeException();
//    }

    public ByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        checkReadableBytes(length);
        getBytes(0, dst, dstIndex, length);
        return this;
    }

    public ByteBuf readBytes(byte[] dst) {
        readBytes(dst, 0, dst.length);
        return this;
    }

    public ByteBuf readBytes(ByteBuf dst) {
        readBytes(dst, dst.writableBytes());
        return this;
    }

    public ByteBuf readBytes(ByteBuf dst, int length) {
        if (length > dst.writableBytes()) {
            throw new IndexOutOfBoundsException();
        }
        readBytes(dst, dst.writerIndex(), length);
        dst.writerIndex(dst.writerIndex() + length);
        return this;
    }

    public ByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
        getBytes(0, dst, dstIndex, length);
        return this;
    }

    public ByteBuf readBytes(ByteBuffer dst) {
        int length = dst.remaining();
        checkReadableBytes(length);
        getBytes(0, dst);
        return this;
    }

    public int readBytes(GatheringByteChannel out, int length)
            throws IOException {
        checkReadableBytes(length);
        int readBytes = getBytes(0, out, length);
        return readBytes;
    }

    public ByteBuf readBytes(OutputStream out, int length) throws IOException {
        checkReadableBytes(length);
        getBytes(0, out, length);
        return this;
    }

    public String toString(int q, int a, java.lang.String b) {
        throw new RuntimeException();
    }

    public ByteBuf setShort(int a, int b) {
        throw new RuntimeException();
    }
    
    /*
     * 
     */

	@Override
	public int refCnt() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public boolean release() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean release(int decrement) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	protected byte _getByte(int index) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	protected short _getShort(int index) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	protected int _getUnsignedMedium(int index) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	protected int _getInt(int index) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	protected long _getLong(int index) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	protected void _setByte(int index, int value) {
		// TODO Auto-generated method stub
		
	}


	@Override
	protected void _setShort(int index, int value) {
		// TODO Auto-generated method stub
		
	}


	@Override
	protected void _setMedium(int index, int value) {
		// TODO Auto-generated method stub
		
	}


	@Override
	protected void _setInt(int index, int value) {
		// TODO Auto-generated method stub
		
	}


	@Override
	protected void _setLong(int index, long value) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public ByteBuf capacity(int newCapacity) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ByteBufAllocator alloc() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public int nioBufferCount() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public ByteBuffer nioBuffer(int index, int length) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ByteBuffer internalNioBuffer(int index, int length) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ByteBuffer[] nioBuffers(int index, int length) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean hasMemoryAddress() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public long memoryAddress() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public ByteBuf retain(int increment) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ByteBuf retain() {
		// TODO Auto-generated method stub
		return null;
	}
}
