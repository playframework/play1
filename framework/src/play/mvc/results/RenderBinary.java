package play.mvc.results;

import java.io.File;
import java.io.InputStream;
import org.apache.commons.codec.net.URLCodec;
import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * 200 OK with application/octet-stream
 */
public class RenderBinary extends Result {

    private static URLCodec encoder = new URLCodec();
    boolean inline = false;
    File file;
    InputStream is;
    String name;

    /**
     * send a binary stream as the response
     * @param is the stream to read from
     * @param name the name to use as Content-Diposition attachement filename
     */
    public RenderBinary(InputStream is, String name) {
        this(is, name, false);
    }

    /**
     * send a binary stream as the response
     * @param is the stream to read from
     * @param name the name to use as Content-Diposition attachement filename
     * @param inline true to set the response Content-Disposition to inline
     */
    public RenderBinary(InputStream is, String name, boolean inline) {
        this.is = is;
        this.name = name;
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
        if (file == null) {
            throw new RuntimeException("file is null");
        }
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
            if (!response.headers.containsKey("Content-Disposition")) {
                if (inline) {
                    if (name == null) {
                        response.setHeader("Content-Disposition", "inline");
                    } else {
                        response.setHeader("Content-Disposition", "inline; filename*=utf-8''" + encoder.encode(name, "utf-8") + "; filename=\"" + encoder.encode(name, "utf-8") + "\"");
                    }
                } else if (name == null) {
                    response.setHeader("Content-Disposition", "attachment");
                } else {
                    response.setHeader("Content-Disposition", "attachment; filename*=utf-8''" + encoder.encode(name, "utf-8") + "; filename=\"" + encoder.encode(name, "utf-8") + "\"");
                }
            }
            if (file != null) {
                if (!file.exists()) {
                    throw new UnexpectedException("Your file buffer does not exists");
                }
                if (!file.canRead()) {
                    throw new UnexpectedException("Can't read your file buffer");
                }
                if (!file.isFile()) {
                    throw new UnexpectedException("Your file buffer is not a file");
                }
                response.direct = file;
            } else {
                byte[] buffer = new byte[8092];
                int count = 0;
                while ((count = is.read(buffer)) > 0) {
                    response.out.write(buffer, 0, count);
                }
                is.close();
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
