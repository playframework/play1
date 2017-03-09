package play.mvc.results;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.IOUtils;

import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * 200 OK with application/octet-stream
 */
public class RenderBinary extends Result {

    private static final String INLINE_DISPOSITION_TYPE = "inline";
    private static final String ATTACHMENT_DISPOSITION_TYPE = "attachment";

    private static final URLCodec encoder = new URLCodec();
    private final boolean inline;
    private long length;
    private File file;
    private InputStream is;
    private final String name;
    private String contentType;

    /**
     * Send a binary stream as the response
     * 
     * @param is
     *            the stream to read from
     * @param name
     *            the name to use as Content-Disposition attachment filename
     */
    public RenderBinary(InputStream is, String name) {
        this(is, name, false);
    }

    public RenderBinary(InputStream is, String name, long length) {
        this(is, name, length, false);
    }

    /**
     * Send a binary stream as the response
     * 
     * @param is
     *            the stream to read from
     * @param name
     *            the name to use as Content-Disposition attachment filename
     * @param inline
     *            true to set the response Content-Disposition to inline
     */
    public RenderBinary(InputStream is, String name, boolean inline) {
        this(is, name, null, inline);
    }

    /**
     * Send a binary stream as the response
     * 
     * @param is
     *            the stream to read from
     * @param name
     *            the name to use as Content-Disposition attachment filename
     * @param contentType
     *            The content type of the stream
     * @param inline
     *            true to set the response Content-Disposition to inline
     */
    public RenderBinary(InputStream is, String name, String contentType, boolean inline) {
        this.is = is;
        this.name = name;
        this.contentType = contentType;
        this.inline = inline;
    }

    public RenderBinary(InputStream is, String name, long length, String contentType, boolean inline) {
        this.is = is;
        this.name = name;
        this.contentType = contentType;
        this.inline = inline;
        this.length = length;
    }

    public RenderBinary(InputStream is, String name, long length, boolean inline) {
        this.is = is;
        this.name = name;
        this.length = length;
        this.inline = inline;
    }

    /**
     * Send a file as the response. Content-disposition is set to attachment.
     * 
     * @param file
     *            readable file to send back
     * @param name
     *            a name to use as Content-disposition's filename
     */
    public RenderBinary(File file, String name) {
        this(file, name, false);
    }

    /**
     * Send a file as the response. Content-disposition is set to attachment, name is taken from file's name
     * 
     * @param file
     *            readable file to send back
     */
    public RenderBinary(File file) {
        this(file, file.getName(), true);
    }

    /**
     * Send a file as the response. Content-disposition is set to attachment, name is taken from file's name
     * 
     * @param file
     *            readable file to send back
     * @param name
     *            a name to use as Content-disposition's filename
     * @param inline
     *            true to set the response Content-Disposition to inline
     */
    public RenderBinary(File file, String name, boolean inline) {
        if (file == null) {
            throw new RuntimeException("file is null");
        }
        this.file = file;
        this.name = name;
        this.inline = inline;
    }

    @Override
    public void apply(Request request, Response response) {
        if (name != null) {
            setContentTypeIfNotSet(response, MimeTypes.getContentType(name));
        }
        if (contentType != null) {
            response.contentType = contentType;
        }
        try {
            if (!response.headers.containsKey("Content-Disposition")) {
                addContentDispositionHeader(response);
            }
            if (file != null) {
                renderFile(file, response);
            } else {
                renderInputStream(is, length, response);
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    private void addContentDispositionHeader(Response response) throws UnsupportedEncodingException {
        if (name == null) {
            response.setHeader("Content-Disposition", dispositionType());
        } else if (canAsciiEncode(name)) {
            String contentDisposition = "%s; filename=\"%s\"";
            response.setHeader("Content-Disposition", String.format(contentDisposition, dispositionType(), name));
        } else {
            String encoding = getEncoding();
            String contentDisposition = "%1$s; filename*=" + encoding + "''%2$s; filename=\"%2$s\"";
            response.setHeader("Content-Disposition", String.format(contentDisposition, dispositionType(), encoder.encode(name, encoding)));
        }
    }

    private static void renderFile(File file, Response response) {
        if (!file.exists()) {
            throw new UnexpectedException("Your file does not exists (" + file + ")");
        }
        if (!file.canRead()) {
            throw new UnexpectedException("Can't read your file (" + file + ")");
        }
        if (!file.isFile()) {
            throw new UnexpectedException("Your file is not a real file (" + file + ")");
        }
        response.direct = file;
    }

    private static void renderInputStream(InputStream is, long length, Response response) throws IOException {
        if (response.getHeader("Content-Length") != null) {
            response.direct = is;
        } else if (length != 0) {
            response.setHeader("Content-Length", String.valueOf(length));
            response.direct = is;
        } else {
            copyInputStreamAndClose(is, response.out);
        }
    }

    private String dispositionType() {
        return inline ? INLINE_DISPOSITION_TYPE : ATTACHMENT_DISPOSITION_TYPE;
    }

    private static void copyInputStreamAndClose(InputStream is, OutputStream out) throws IOException {
        try {
            IOUtils.copyLarge(is, out);
        } finally {
            closeQuietly(is);
        }
    }

    private boolean canAsciiEncode(String string) {
        CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();
        return asciiEncoder.canEncode(string);
    }

    public boolean isInline() {
        return inline;
    }

    public long getLength() {
        return length;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }
}
