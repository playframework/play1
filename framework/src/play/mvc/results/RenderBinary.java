package play.mvc.results;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.apache.commons.codec.net.URLCodec;
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

    private static URLCodec encoder = new URLCodec();
    boolean inline = false;
    long length = 0;
    File file;
    InputStream is;
    String name;
    String contentType;

    /**
     * send a binary stream as the response
     * @param is the stream to read from
     * @param name the name to use as Content-Diposition attachement filename
     */
    public RenderBinary(InputStream is, String name) {
        this(is, name, false);
    }

    public RenderBinary(InputStream is, String name, long length) {
        this(is, name, length, false);
    }

    /**
     * send a binary stream as the response
     * @param is the stream to read from
     * @param name the name to use as Content-Diposition attachement filename
     * @param inline true to set the response Content-Disposition to inline
     */
    public RenderBinary(InputStream is, String name, boolean inline) {
        this(is, name, null, inline);
    }

    /**
     * send a binary stream as the response
     * @param is the stream to read from
     * @param name the name to use as Content-Diposition attachement filename
     * @param inline true to set the response Content-Disposition to inline
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
     * Send a file as the response. Content-disposion is set to attachment.
     * 
     * @param file readable file to send back
     * @param name a name to use as Content-disposion's filename
     */
    public RenderBinary(File file, String name) {
        this(file, name, false);
        if (file == null) {
            throw new RuntimeException("file is null");
        }
    }

    /**
     * Send a file as the response. 
     * Content-disposion is set to attachment, name is taken from file's name
     * @param file readable file to send back
     */
    public RenderBinary(File file) {
        this(file, file.getName(), true);
    }

    /**
     * Send a file as the response. 
     * Content-disposion is set to attachment, name is taken from file's name
     * @param file readable file to send back
     */
    public RenderBinary(File file, String name, boolean inline) {
        this.file = file;
        this.name = name;
        this.inline = inline;
        if (file == null) {
            throw new RuntimeException("file is null");
        }
    }

    @Override
    public void apply(Request request, Response response) {
        try {
            if (name != null) {
                setContentTypeIfNotSet(response, MimeTypes.getContentType(name));
            }
            if (contentType != null) {
                response.contentType = contentType;
            }
            String dispositionType;
            if(inline) {
                dispositionType = INLINE_DISPOSITION_TYPE;
            } else {
                dispositionType = ATTACHMENT_DISPOSITION_TYPE;
            }
            if (!response.headers.containsKey("Content-Disposition")) {
                if(name == null) {
                    response.setHeader("Content-Disposition", dispositionType);
                } else {
                    if(canAsciiEncode(name)) {
                        String contentDisposition = "%s; filename=\"%s\"";
                        response.setHeader("Content-Disposition", String.format(contentDisposition, dispositionType, name));
                    } else {
                        final String encoding = getEncoding();
                        String contentDisposition = "%1$s; filename*="+encoding+"''%2$s; filename=\"%2$s\"";
                        response.setHeader("Content-Disposition", String.format(contentDisposition, dispositionType, encoder.encode(name, encoding)));
                    }
                }
            }
            if (file != null) {
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
            } else {
                if (response.getHeader("Content-Length") != null) {
                    response.direct = is;
                } else {
                    if (length != 0) {
                        response.setHeader("Content-Length", length + "");
                        response.direct = is;
                    } else {
                        try {
                            byte[] buffer = new byte[8192];
                            int count = 0;
                            while ((count = is.read(buffer)) > -1) {
                                response.out.write(buffer, 0, count);
                            }
                        }
                        finally {
                            try {
                                is.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    private boolean canAsciiEncode(String string) {
        CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();
        return asciiEncoder.canEncode(string);
    }
}
