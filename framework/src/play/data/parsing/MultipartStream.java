package play.data.parsing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.util.Closeable;
import org.apache.commons.fileupload.util.Streams;

/**
 * <p> Low level API for processing file uploads.
 *
 * <p> This class can be used to process data streams conforming to MIME
 * 'multipart' format as defined in
 * <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC 1867</a>. Arbitrarily
 * large amounts of data in the stream can be processed under constant
 * memory usage.
 *
 * <p> The format of the stream is defined in the following way:<br>
 *
 * <code>
 *   multipart-body := preamble 1*encapsulation close-delimiter epilogue<br>
 *   encapsulation := delimiter body CRLF<br>
 *   delimiter := "--" boundary CRLF<br>
 *   close-delimiter := "--" boundary "--"<br>
 *   preamble := &lt;ignore&gt;<br>
 *   epilogue := &lt;ignore&gt;<br>
 *   body := header-part CRLF body-part<br>
 *   header-part := 1*header CRLF<br>
 *   header := header-name ":" header-value<br>
 *   header-name := &lt;printable ascii characters except ":"&gt;<br>
 *   header-value := &lt;any ascii characters except CR &amp; LF&gt;<br>
 *   body-data := &lt;arbitrary data&gt;<br>
 * </code>
 *
 * <p>Note that body-data can contain another multipart entity.  There
 * is limited support for single pass processing of such nested
 * streams.  The nested stream is <strong>required</strong> to have a
 * boundary token of the same length as the parent stream (see {@link
 * #setBoundary(byte[])}).
 *
 * <p>Here is an example of usage of this class.<br>
 *
 * <pre>
 *    try {
 *        MultipartStream multipartStream = new MultipartStream(input,
 *                                                              boundary);
 *        boolean nextPart = multipartStream.skipPreamble();
 *        OutputStream output;
 *        while(nextPart) {
 *            header = chunks.readHeader();
 *            // process headers
 *            // create some output stream
 *            multipartStream.readBodyPart(output);
 *            nextPart = multipartStream.readBoundary();
 *        }
 *    } catch(MultipartStream.MalformedStreamException e) {
 *          // the stream failed to follow required syntax
 *    } catch(IOException) {
 *          // a read or write error occurred
 *    }
 *
 * </pre>
 *
 * @author <a href="mailto:Rafal.Krzewski@e-point.pl">Rafal Krzewski</a>
 * @author <a href="mailto:martinc@apache.org">Martin Cooper</a>
 * @author Sean C. Sullivan
 *
 * @version $Id: MultipartStream.java 174 2007-11-04 21:25:00Z leo $
 */
public class MultipartStream {

    /**
     * Internal class, which is used to invoke the
     * {@link ProgressListener}.
     */
    static class ProgressNotifier {

        /** The listener to invoke.
         */
        private final ProgressListener listener;
        /** Number of expected bytes, if known, or -1.
         */
        private final long contentLength;
        /** Number of bytes, which have been read so far.
         */
        private long bytesRead;
        /** Number of items, which have been read so far.
         */
        private int items;

        /** Creates a new instance with the given listener
         * and content length.
         * @param pListener The listener to invoke.
         * @param pContentLength The expected content length.
         */
        ProgressNotifier(ProgressListener pListener, long pContentLength) {
            listener = pListener;
            contentLength = pContentLength;
        }

        /** Called to indicate that bytes have been read.
         * @param pBytes Number of bytes, which have been read.
         */
        void noteBytesRead(int pBytes) {
            /* Indicates, that the given number of bytes have been read from
             * the input stream.
             */
            bytesRead += pBytes;
            notifyListener();
        }

        /** Called to indicate, that a new file item has been detected.
         */
        void noteItem() {
            ++items;
        }

        /** Called for notifying the listener.
         */
        private void notifyListener() {
            if (listener != null) {
                listener.update(bytesRead, contentLength, items);
            }
        }
    }

    // ----------------------------------------------------- Manifest constants
    /**
     * The Carriage Return ASCII character value.
     */
    public static final byte CR = 0x0D;
    /**
     * The Line Feed ASCII character value.
     */
    public static final byte LF = 0x0A;
    /**
     * The dash (-) ASCII character value.
     */
    public static final byte DASH = 0x2D;
    /**
     * The maximum length of <code>header-part</code> that will be
     * processed (10 kilobytes = 10240 bytes.).
     */
    public static final int HEADER_PART_SIZE_MAX = 10240;
    /**
     * The default length of the buffer used for processing a request.
     */
    protected static final int DEFAULT_BUFSIZE = 4096;
    /**
     * A byte sequence that marks the end of <code>header-part</code>
     * (<code>CRLFCRLF</code>).
     */
    protected static final byte[] HEADER_SEPARATOR = {
        CR, LF, CR, LF
    };
    /**
     * A byte sequence that that follows a delimiter that will be
     * followed by an encapsulation (<code>CRLF</code>).
     */
    protected static final byte[] FIELD_SEPARATOR = {
        CR, LF
    };
    /**
     * A byte sequence that that follows a delimiter of the last
     * encapsulation in the stream (<code>--</code>).
     */
    protected static final byte[] STREAM_TERMINATOR = {
        DASH, DASH
    };
    /**
     * A byte sequence that precedes a boundary (<code>CRLF--</code>).
     */
    protected static final byte[] BOUNDARY_PREFIX = {
        CR, LF, DASH, DASH
    };
    /**
     * The number of bytes, over and above the boundary size, to use for the
     * keep region.
     */
    private static final int KEEP_REGION_PAD = 3;
    // ----------------------------------------------------------- Data members
    /**
     * The input stream from which data is read.
     */
    private final InputStream input;
    /**
     * The length of the boundary token plus the leading <code>CRLF--</code>.
     */
    private int boundaryLength;
    /**
     * The amount of data, in bytes, that must be kept in the buffer in order
     * to detect delimiters reliably.
     */
    private int keepRegion;
    /**
     * The byte sequence that partitions the stream.
     */
    private byte[] boundary;
    /**
     * The length of the buffer used for processing the request.
     */
    private final int bufSize;
    /**
     * The buffer used for processing the request.
     */
    private final byte[] buffer;
    /**
     * The index of first valid character in the buffer.
     * <br>
     * 0 <= head < bufSize
     */
    private int head;
    /**
     * The index of last valid character in the buffer + 1.
     * <br>
     * 0 <= tail <= bufSize
     */
    private int tail;
    /**
     * The content encoding to use when reading headers.
     */
    private String headerEncoding;
    /**
     * The progress notifier, if any, or null.
     */
    private final ProgressNotifier notifier;

    // ----------------------------------------------------------- Constructors
    /**
     * Creates a new instance.
     * @deprecated Use {@link #MultipartStream(InputStream, byte[], ProgressNotifier)},
     * or {@link #MultipartStream(InputStream, byte[], int, ProgressNotifier)}
     */
    @Deprecated
    public MultipartStream() {
        this(null, null, null);
    }

    /**
     * <p> Constructs a <code>MultipartStream</code> with a custom size buffer
     * and no progress notifier.
     *
     * <p> Note that the buffer must be at least big enough to contain the
     * boundary string, plus 4 characters for CR/LF and double dash, plus at
     * least one byte of data.  Too small a buffer size setting will degrade
     * performance.
     *
     * @param input    The <code>InputStream</code> to serve as a data source.
     * @param boundary The token used for dividing the stream into
     *                 <code>encapsulations</code>.
     * @param bufSize  The size of the buffer to be used, in bytes.
     *
     * @see #MultipartStream(InputStream, byte[], ProgressNotifier)
     * @deprecated Use {@link #MultipartStream(InputStream, byte[], int, ProgressNotifier)}.
     */
    @Deprecated
    public MultipartStream(InputStream input, byte[] boundary, int bufSize) {
        this(input, boundary, bufSize, null);
    }

    /**
     * <p> Constructs a <code>MultipartStream</code> with a custom size buffer.
     *
     * <p> Note that the buffer must be at least big enough to contain the
     * boundary string, plus 4 characters for CR/LF and double dash, plus at
     * least one byte of data.  Too small a buffer size setting will degrade
     * performance.
     *
     * @param input    The <code>InputStream</code> to serve as a data source.
     * @param boundary The token used for dividing the stream into
     *                 <code>encapsulations</code>.
     * @param bufSize  The size of the buffer to be used, in bytes.
     * @param pNotifier The notifier, which is used for calling the
     *                  progress listener, if any.
     *
     * @see #MultipartStream(InputStream, byte[], ProgressNotifier)
     */
    MultipartStream(InputStream input,
            byte[] boundary,
            int bufSize,
            ProgressNotifier pNotifier) {
        this.input = input;
        this.bufSize = bufSize;
        this.buffer = new byte[bufSize];
        this.notifier = pNotifier;

        // We prepend CR/LF to the boundary to chop trailing CR/LF from
        // body-data tokens.
        this.boundary = new byte[boundary.length + BOUNDARY_PREFIX.length];
        this.boundaryLength = boundary.length + BOUNDARY_PREFIX.length;
        this.keepRegion = boundary.length + KEEP_REGION_PAD;
        System.arraycopy(BOUNDARY_PREFIX, 0, this.boundary, 0,
                BOUNDARY_PREFIX.length);
        System.arraycopy(boundary, 0, this.boundary, BOUNDARY_PREFIX.length,
                boundary.length);

        head = 0;
        tail = 0;
    }

    /**
     * <p> Constructs a <code>MultipartStream</code> with a default size buffer.
     *
     * @param input    The <code>InputStream</code> to serve as a data source.
     * @param boundary The token used for dividing the stream into
     *                 <code>encapsulations</code>.
     * @param pNotifier An object for calling the progress listener, if any.
     *
     *
     * @see #MultipartStream(InputStream, byte[], int, ProgressNotifier)
     */
    MultipartStream(InputStream input,
            byte[] boundary,
            ProgressNotifier pNotifier) {
        this(input, boundary, DEFAULT_BUFSIZE, pNotifier);
    }

    /**
     * <p> Constructs a <code>MultipartStream</code> with a default size buffer.
     *
     * @param input    The <code>InputStream</code> to serve as a data source.
     * @param boundary The token used for dividing the stream into
     *                 <code>encapsulations</code>.
     *
     * @deprecated Use {@link #MultipartStream(InputStream, byte[],
     *  ProgressNotifier)}.
     * @see #MultipartStream(InputStream, byte[], int, ProgressNotifier)
     */
    @Deprecated
    public MultipartStream(InputStream input,
            byte[] boundary) {
        this(input, boundary, DEFAULT_BUFSIZE, null);
    }

    // --------------------------------------------------------- Public methods
    /**
     * Retrieves the character encoding used when reading the headers of an
     * individual part. When not specified, or <code>null</code>, the platform
     * default encoding is used.
     *
     * @return The encoding used to read part headers.
     */
    public String getHeaderEncoding() {
        return headerEncoding;
    }

    /**
     * Specifies the character encoding to be used when reading the headers of
     * individual parts. When not specified, or <code>null</code>, the platform
     * default encoding is used.
     *
     * @param encoding The encoding used to read part headers.
     */
    public void setHeaderEncoding(String encoding) {
        headerEncoding = encoding;
    }

    /**
     * Reads a byte from the <code>buffer</code>, and refills it as
     * necessary.
     *
     * @return The next byte from the input stream.
     *
     * @throws IOException if there is no more data available.
     */
    public byte readByte()
            throws IOException {
        // Buffer depleted ?
        if (head == tail) {
            head = 0;
            // Refill.
            tail = input.read(buffer, head, bufSize);
            if (tail == -1) {
                // No more data available.
                throw new IOException("No more data is available");
            }
            if(notifier != null) {
                notifier.noteBytesRead(tail);
            }
        }
        return buffer[head++];
    }

    /**
     * Skips a <code>boundary</code> token, and checks whether more
     * <code>encapsulations</code> are contained in the stream.
     *
     * @return <code>true</code> if there are more encapsulations in
     *         this stream; <code>false</code> otherwise.
     *
     * @throws MalformedStreamException if the stream ends unexpectedly or
     *                                  fails to follow required syntax.
     */
    public boolean readBoundary()
            throws MalformedStreamException {
        byte[] marker = new byte[2];
        boolean nextChunk = false;

        head += boundaryLength;
        try {
            marker[0] = readByte();
            if (marker[0] == LF) {
                // Work around IE5 Mac bug with input type=image.
                // Because the boundary delimiter, not including the trailing
                // CRLF, must not appear within any file (RFC 2046, section
                // 5.1.1), we know the missing CR is due to a buggy browser
                // rather than a file containing something similar to a
                // boundary.
                return true;
            }

            marker[1] = readByte();
            if (arrayequals(marker, STREAM_TERMINATOR, 2)) {
                nextChunk = false;
            } else if (arrayequals(marker, FIELD_SEPARATOR, 2)) {
                nextChunk = true;
            } else {
                throw new MalformedStreamException(
                        "Unexpected characters follow a boundary");
            }
        } catch (IOException e) {
            throw new MalformedStreamException("Stream ended unexpectedly", e);
        }
        return nextChunk;
    }

    /**
     * <p>Changes the boundary token used for partitioning the stream.
     *
     * <p>This method allows single pass processing of nested multipart
     * streams.
     *
     * <p>The boundary token of the nested stream is <code>required</code>
     * to be of the same length as the boundary token in parent stream.
     *
     * <p>Restoring the parent stream boundary token after processing of a
     * nested stream is left to the application.
     *
     * @param boundary The boundary to be used for parsing of the nested
     *                 stream.
     *
     * @throws IllegalBoundaryException if the <code>boundary</code>
     *                                  has a different length than the one
     *                                  being currently parsed.
     */
    public void setBoundary(byte[] boundary)
            throws IllegalBoundaryException {
        if (boundary.length != boundaryLength - BOUNDARY_PREFIX.length) {
            throw new IllegalBoundaryException(
                    "The length of a boundary token can not be changed");
        }
        System.arraycopy(boundary, 0, this.boundary, BOUNDARY_PREFIX.length,
                boundary.length);
    }

    /**
     * <p>Reads the <code>header-part</code> of the current
     * <code>encapsulation</code>.
     *
     * <p>Headers are returned verbatim to the input stream, including the
     * trailing <code>CRLF</code> marker. Parsing is left to the
     * application.
     *
     * <p><strong>TODO</strong> allow limiting maximum header size to
     * protect against abuse.
     *
     * @return The <code>header-part</code> of the current encapsulation.
     *
     * @throws MalformedStreamException if the stream ends unexpectedly.
     */
    public String readHeaders()
            throws MalformedStreamException {
        int i = 0;
        byte[] b = new byte[1];
        // to support multi-byte characters
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int sizeMax = HEADER_PART_SIZE_MAX;
        int size = 0;
        while (i < HEADER_SEPARATOR.length) {
            try {
                b[0] = readByte();
            } catch (IOException e) {
                throw new MalformedStreamException("Stream ended unexpectedly", e);
            }
            size++;
            if (b[0] == HEADER_SEPARATOR[i]) {
                i++;
            } else {
                i = 0;
            }
            if (size <= sizeMax) {
                baos.write(b[0]);
            }
        }

        String headers = null;
        if (headerEncoding != null) {
            try {
                headers = baos.toString(headerEncoding);
            } catch (UnsupportedEncodingException e) {
                // Fall back to platform default if specified encoding is not
                // supported.
                headers = baos.toString();
            }
        } else {
            headers = baos.toString();
        }

        return headers;
    }

    /**
     * <p>Reads <code>body-data</code> from the current
     * <code>encapsulation</code> and writes its contents into the
     * output <code>Stream</code>.
     *
     * <p>Arbitrary large amounts of data can be processed by this
     * method using a constant size buffer. (see {@link
     * #MultipartStream(InputStream,byte[],int, ProgressNotifier) constructor}).
     *
     * @param output The <code>Stream</code> to write data into. May
     *               be null, in which case this method is equivalent
     *               to {@link #discardBodyData()}.
     *
     * @return the amount of data written.
     *
     * @throws MalformedStreamException if the stream ends unexpectedly.
     * @throws IOException              if an i/o error occurs.
     */
    public int readBodyData(OutputStream output)
            throws MalformedStreamException, IOException {
        InputStream istream = newInputStream();
        return (int) Streams.copy(istream, output, false);
    }

    /**
     * Creates a new {@link ItemInputStream}.
     * @return A new instance of {@link ItemInputStream}.
     */
    ItemInputStream newInputStream() {
        return new ItemInputStream();
    }

    /**
     * <p> Reads <code>body-data</code> from the current
     * <code>encapsulation</code> and discards it.
     *
     * <p>Use this method to skip encapsulations you don't need or don't
     * understand.
     *
     * @return The amount of data discarded.
     *
     * @throws MalformedStreamException if the stream ends unexpectedly.
     * @throws IOException              if an i/o error occurs.
     */
    public int discardBodyData()
            throws MalformedStreamException,
            IOException {
        return readBodyData(null);
    }

    /**
     * Finds the beginning of the first <code>encapsulation</code>.
     *
     * @return <code>true</code> if an <code>encapsulation</code> was found in
     *         the stream.
     *
     * @throws IOException if an i/o error occurs.
     */
    public boolean skipPreamble()
            throws IOException {
        // First delimiter may be not preceded with a CRLF.
        System.arraycopy(boundary, 2, boundary, 0, boundary.length - 2);
        boundaryLength = boundary.length - 2;
        try {
            // Discard all data up to the delimiter.
            discardBodyData();

            // Read boundary - if succeded, the stream contains an
            // encapsulation.
            return readBoundary();
        } catch (MalformedStreamException e) {
            return false;
        } finally {
            // Restore delimiter.
            System.arraycopy(boundary, 0, boundary, 2, boundary.length - 2);
            boundaryLength = boundary.length;
            boundary[0] = CR;
            boundary[1] = LF;
        }
    }

    /**
     * Compares <code>count</code> first bytes in the arrays
     * <code>a</code> and <code>b</code>.
     *
     * @param a     The first array to compare.
     * @param b     The second array to compare.
     * @param count How many bytes should be compared.
     *
     * @return <code>true</code> if <code>count</code> first bytes in arrays
     *         <code>a</code> and <code>b</code> are equal.
     */
    public static boolean arrayequals(byte[] a,
            byte[] b,
            int count) {
        for (int i = 0; i < count; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Searches for a byte of specified value in the <code>buffer</code>,
     * starting at the specified <code>position</code>.
     *
     * @param value The value to find.
     * @param pos   The starting position for searching.
     *
     * @return The position of byte found, counting from beginning of the
     *         <code>buffer</code>, or <code>-1</code> if not found.
     */
    protected int findByte(byte value,
            int pos) {
        for (int i = pos; i < tail; i++) {
            if (buffer[i] == value) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Searches for the <code>boundary</code> in the <code>buffer</code>
     * region delimited by <code>head</code> and <code>tail</code>.
     *
     * @return The position of the boundary found, counting from the
     *         beginning of the <code>buffer</code>, or <code>-1</code> if
     *         not found.
     */
    protected int findSeparator() {
        int first;
        int match = 0;
        int maxpos = tail - boundaryLength;
        for (first = head;
                (first <= maxpos) && (match != boundaryLength);
                first++) {
            first = findByte(boundary[0], first);
            if (first == -1 || (first > maxpos)) {
                return -1;
            }
            for (match = 1; match < boundaryLength; match++) {
                if (buffer[first + match] != boundary[match]) {
                    break;
                }
            }
        }
        if (match == boundaryLength) {
            return first - 1;
        }
        return -1;
    }

    /**
     * Thrown to indicate that the input stream fails to follow the
     * required syntax.
     */
    public static class MalformedStreamException
            extends IOException {

        /**
         * Constructs a <code>MalformedStreamException</code> with no
         * detail message.
         */
        public MalformedStreamException() {
            super();
        }

        /**
         * Constructs an <code>MalformedStreamException</code> with
         * the specified detail message.
         *
         * @param message The detail message.
         */
        public MalformedStreamException(String message) {
            super(message);
        }

        public MalformedStreamException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown upon attempt of setting an invalid boundary token.
     */
    public static class IllegalBoundaryException
            extends IOException {

        /**
         * Constructs an <code>IllegalBoundaryException</code> with no
         * detail message.
         */
        public IllegalBoundaryException() {
            super();
        }

        /**
         * Constructs an <code>IllegalBoundaryException</code> with
         * the specified detail message.
         *
         * @param message The detail message.
         */
        public IllegalBoundaryException(String message) {
            super(message);
        }
    }

    /**
     * An {@link InputStream} for reading an items contents.
     */
    public class ItemInputStream extends InputStream implements Closeable {

        /** The number of bytes, which have been read so far.
         */
        private long total;
        /** The number of bytes, which must be hold, because
         * they might be a part of the boundary.
         */
        private int pad;
        /** The current offset in the buffer.
         */
        private int pos;
        /** Whether the stream is already closed.
         */
        private boolean closed;

        /**
         * Creates a new instance.
         */
        ItemInputStream() {
            findSeparator();
        }

        /**
         * Called for finding the separator.
         */
        private void findSeparator() {
            pos = MultipartStream.this.findSeparator();
            if (pos == -1) {
                if (tail - head > keepRegion) {
                    pad = keepRegion;
                } else {
                    pad = tail - head;
                }
            }
        }

        /**
         * Returns the number of bytes, which have been read
         * by the stream.
         * @return Number of bytes, which have been read so far.
         */
        public long getBytesRead() {
            return total;
        }

        /**
         * Returns the number of bytes, which are currently
         * available, without blocking.
         * @throws IOException An I/O error occurs.
         * @return Number of bytes in the buffer.
         */
        @Override
        public int available() throws IOException {
            if (pos == -1) {
                return tail - head - pad;
            }
            return pos - head;
        }
        /** Offset when converting negative bytes to integers.
         */
        private static final int BYTE_POSITIVE_OFFSET = 256;

        /**
         * Returns the next byte in the stream.
         * @return The next byte in the stream, as a non-negative
         *   integer, or -1 for EOF.
         * @throws IOException An I/O error occurred.
         */
        @Override
        public int read() throws IOException {
            if (closed) {
                throw new FileItemStream.ItemSkippedException();
            }
            if (available() == 0) {
                if (makeAvailable() == 0) {
                    return -1;
                }
            }
            ++total;
            int b = buffer[head++];
            if (b >= 0) {
                return b;
            }
            return b + BYTE_POSITIVE_OFFSET;
        }

        /**
         * Reads bytes into the given buffer.
         * @param b The destination buffer, where to write to.
         * @param off Offset of the first byte in the buffer.
         * @param len Maximum number of bytes to read.
         * @return Number of bytes, which have been actually read,
         *   or -1 for EOF.
         * @throws IOException An I/O error occurred.
         */
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (closed) {
                throw new FileItemStream.ItemSkippedException();
            }
            if (len == 0) {
                return 0;
            }
            int res = available();
            if (res == 0) {
                res = makeAvailable();
                if (res == 0) {
                    return -1;
                }
            }
            res = Math.min(res, len);
            System.arraycopy(buffer, head, b, off, res);
            head += res;
            total += res;
            return res;
        }

        /**
         * Closes the input stream.
         * @throws IOException An I/O error occurred.
         */
        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            for (;;) {
                int av = available();
                if (av == 0) {
                    av = makeAvailable();
                    if (av == 0) {
                        break;
                    }
                }
                skip(av);
            }
            closed = true;
        }

        /**
         * Skips the given number of bytes.
         * @param bytes Number of bytes to skip.
         * @return The number of bytes, which have actually been
         *   skipped.
         * @throws IOException An I/O error occurred.
         */
        @Override
        public long skip(long bytes) throws IOException {
            if (closed) {
                throw new FileItemStream.ItemSkippedException();
            }
            int av = available();
            if (av == 0) {
                av = makeAvailable();
                if (av == 0) {
                    return 0;
                }
            }
            long res = Math.min(av, bytes);
            head += res;
            return res;
        }

        /**
         * Attempts to read more data.
         * @return Number of available bytes
         * @throws IOException An I/O error occurred.
         */
        private int makeAvailable() throws IOException {
            if (pos != -1) {
                return 0;
            }

            // Move the data to the beginning of the buffer.
            total += tail - head - pad;
            System.arraycopy(buffer, tail - pad, buffer, 0, pad);

            // Refill buffer with new data.
            head = 0;
            int bytesRead = input.read(buffer, pad, bufSize - pad);
            if (bytesRead == -1) {
                // The last pad amount is left in the buffer.
                // Boundary can't be in there so signal an error
                // condition.
                throw new MalformedStreamException(
                        "Stream ended unexpectedly");
            }
            // notifier.noteBytesRead(bytesRead);
            tail = pad + bytesRead;
            findSeparator();
            return available();
        }

        /**
         * Returns, whether the stream is closed.
         * @return True, if the stream is closed, otherwise false.
         */
        @Override
        public boolean isClosed() {
            return closed;
        }
    }

    // ------------------------------------------------------ Debugging methods
    // These are the methods that were used to debug this stuff.
    /*
    // Dump data.
    protected void dump()
    {
    System.out.println("01234567890");
    byte[] temp = new byte[buffer.length];
    for(int i=0; i<buffer.length; i++)
    {
    if (buffer[i] == 0x0D || buffer[i] == 0x0A)
    {
    temp[i] = 0x21;
    }
    else
    {
    temp[i] = buffer[i];
    }
    }
    System.out.println(new String(temp));
    int i;
    for (i=0; i<head; i++)
    System.out.print(" ");
    System.out.println("h");
    for (i=0; i<tail; i++)
    System.out.print(" ");
    System.out.println("t");
    System.out.flush();
    }
    // Main routine, for testing purposes only.
    //
    // @param args A String[] with the command line arguments.
    // @throws Exception, a generic exception.
    public static void main( String[] args )
    throws Exception
    {
    File boundaryFile = new File("boundary.dat");
    int boundarySize = (int)boundaryFile.length();
    byte[] boundary = new byte[boundarySize];
    FileInputStream input = new FileInputStream(boundaryFile);
    input.read(boundary,0,boundarySize);
    input = new FileInputStream("multipart.dat");
    MultipartStream chunks = new MultipartStream(input, boundary);
    int i = 0;
    String header;
    OutputStream output;
    boolean nextChunk = chunks.skipPreamble();
    while (nextChunk)
    {
    header = chunks.readHeaders();
    System.out.println("!"+header+"!");
    System.out.println("wrote part"+i+".dat");
    output = new FileOutputStream("part"+(i++)+".dat");
    chunks.readBodyData(output);
    nextChunk = chunks.readBoundary();
    }
    }
     */
}
