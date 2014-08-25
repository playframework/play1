package cn.bran.strings;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;

public class Strings {
    private interface UTF8Encoder {
        /** Assumes destination is large enough to hold encoded source. */
        int encode(String source);
        byte[] encodeToArray(String source);

        UTF8Encoder newInstance();
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static abstract class UTF8CharsetEncoder implements UTF8Encoder {
        private final byte[] destination;
        protected final ByteBuffer wrapper;
        protected final CharsetEncoder encoder;

        protected UTF8CharsetEncoder(byte[] destination) {
            this.destination = destination;
            wrapper = ByteBuffer.wrap(destination);
            //~ wrapper = ByteBuffer.allocateDirect(destination.length);
            encoder = UTF8.newEncoder();
        }

        
        public byte[] encodeToArray(String source) {
            int length = encode(source);
            byte[] out = new byte[length];
            System.arraycopy(destination, 0, out, 0, length);
            return out;
        }

    }

    private static final class DirectEncoder extends UTF8CharsetEncoder {
        public DirectEncoder(byte[] destination) { super(destination); }

        @Override
        public  int encode(String source) {
            wrapper.clear();
            encoder.reset();

            CharBuffer in = CharBuffer.wrap(source);
            CoderResult result = encoder.encode(in, wrapper, true);
            assert result == CoderResult.UNDERFLOW;
            result = encoder.flush(wrapper);
            assert result == CoderResult.UNDERFLOW;

            return wrapper.position();
        }

        public DirectEncoder newInstance() {
            return new DirectEncoder(wrapper.array());
        }
    }

    private static final class CharBufferCopyEncoder extends UTF8CharsetEncoder {
        private final CharBuffer tempWrapper = CharBuffer.allocate(1024);
        private final char[] tempChars = tempWrapper.array();

        public CharBufferCopyEncoder(byte[] destination) { super(destination); }

        @Override
        public int encode(String source) {
            wrapper.clear();
            encoder.reset();
            
            //~ final CharBuffer tempWrapper = CharBuffer.allocate(1024);
            //~ final char[] tempChars = tempWrapper.array();

            int readOffset = 0;
            boolean done = false;
            while (!done) {
                int readLength = source.length() - readOffset;
                if (readLength > tempChars.length) {
                    readLength = tempChars.length;
                }

                // Copy the chunk into our temporary buffer
                source.getChars(0, readLength, tempChars, 0);
                tempWrapper.clear();
                tempWrapper.limit(readLength);
                readOffset += readLength;

                done = readOffset == source.length();
                CoderResult result = encoder.encode(tempWrapper, wrapper, done);
                assert result == CoderResult.UNDERFLOW;
            }
            CoderResult result = encoder.flush(wrapper);
            assert result == CoderResult.UNDERFLOW;

            return wrapper.position();
        }

        public CharBufferCopyEncoder newInstance() {
            return new CharBufferCopyEncoder(wrapper.array());
        }
    }

    private static final class StringEncoder implements UTF8Encoder {
        private final byte[] destination;

        public StringEncoder(byte[] destination) {
            this.destination = destination;
        }

        public int encode(String source) {
            byte[] array = encodeToArray(source);
            System.arraycopy(array, 0, destination, 0, array.length);
            return array.length;
        }
        
        public byte[] encodeToArray(String source) {
            try {
                return source.getBytes("UTF-8"); 
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        public StringEncoder newInstance() {
            return new StringEncoder(destination);
        }
    }

    private static void error() {
        System.err.println("(bytebuffer|string|chars) (once|reuse) (buffer|array) (input strings)");
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            error();
            return;
        }

        byte[] destination = new byte[4096];

        UTF8Encoder encoder;
        if (args[0].equals("bytebuffer")) {
            encoder = new DirectEncoder(destination);
        } else if (args[0].equals("string")) {
            encoder = new StringEncoder(destination);
        } else if (args[0].equals("chars")) {
            encoder = new CharBufferCopyEncoder(destination);
        } else {
            error();
            return;
        }

        boolean reuseEncoder = true;
        if (args[1].equals("once")) {
            reuseEncoder = false;
        } else if (!args[1].equals("reuse")) {
            error();
            return;
        }

        boolean toBuffer = true;
        if (args[2].equals("array")) {
            toBuffer = false;
        } else if (!args[2].equals("buffer")) {
            error();
            return;
        }

        ArrayList<String> strings = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(args[3]), "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            strings.add(line);
        }

        //~ final int ITERATIONS = 5000000;
        //~ final int ITERATIONS = 1000000;
        //~ final int ITERATIONS = 10000;
        final int ITERATIONS = 400;
        for (int j = 0; j < 10; ++j) {
            long start = System.nanoTime();
            for (int i = 0; i < ITERATIONS; ++i) {
                for (String value : strings) {
                    UTF8Encoder temp = encoder;
                    if (!reuseEncoder) {
                        temp = encoder.newInstance();
                    }

                    if (toBuffer) {
                        int bytes = temp.encode(value);
                        assert new String(destination, 0, bytes, "UTF-8").equals(value);
                    } else {
                        byte[] out = temp.encodeToArray(strings.get(i));
                        assert new String(out, "UTF-8").equals(value);
                    }
                }
            }
            long end = System.nanoTime();
            System.out.println(((double) end-start)/1000000000. + " seconds");
        }
    }
    
    /**
     * show printwriter is about 15% - 20% slower than stringbuilder
     */
    @Test public void strinBuilderVsPrintWriter() {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
    	PrintWriter spw = new PrintWriter(baos);
    
    	StringBuilder sb = new StringBuilder(1000);
    	long tsb = 0, tpw = 0;
    	long t = 0;
    	
    	String s1 = "社光明的枯草满足在在晒太阳\n";
    // warm up
    	spw.print(s1);
    	spw.print(s1);
    	spw.print(s1);
    	
    	sb.append(s1);
    	sb.append(s1);
    	sb.append(s1);
    	
    	for (int i =0; i < 10000; i++) {
//    		baos.reset();
    		t = System.nanoTime();
    		spw.print(s1);
    		tpw += System.nanoTime() - t;
//    		sb.
    		t = System.nanoTime();
    		sb.append(s1);
    		tsb += System.nanoTime() - t;
    	}
    	
    	System.out.println("string builder: " + tsb);
    	System.out.println("print writer: " + tpw);
    	
    }
 
    @Test public void testArrays() {
    	Object[] oa = new String[] {"1", "2"};
    	assertEquals(String[].class, oa.getClass());
    	assertTrue(oa instanceof Object[]);
    	mymethod(oa);
    	oa = new Date[] {new Date()};
    	assertTrue(oa instanceof Object[]);
    	int[] ia = new int[] {};
    	// ia instanceof Object[] is a syntax error
//    	memethod(ia);
    }
    
    private void mymethod(Object[] oa) {}
}
