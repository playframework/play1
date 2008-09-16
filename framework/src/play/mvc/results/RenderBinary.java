package play.mvc.results;

import java.io.File;
import java.io.InputStream;
import org.apache.commons.codec.net.URLCodec;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

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
        this( is, name, false );
    }

    /**
     * send a binary stream as the response
     * @param is the stream to read from
     * @param name the name to use as Content-Diposition attachement filename
     * @param inline true to set the response Content-Disposition to inline
     */
    public RenderBinary(InputStream is, String name, boolean inline ) {
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
        this( file, name, false );
    }

    /**
     * Send a file as the response. 
     * Content-disposion is set to attachment, name is taken from file's name
     * @param file readable file to send back
     */
    public RenderBinary(File file) {
        this( file, file.getName(), false );
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
    }

    @Override
    public void apply(Request request, Response response) {
        try {
            setContentTypeIfNotSet(response, "application/octet-stream");
            if( inline ) {
                response.setHeader("Content-Disposition", "inline");
            } else if(name == null) {
                response.setHeader("Content-Disposition", "attachment");
            } else {
                // filename must be quoted and encoded (space, etc)
                name = encoder.encode(name);
                // see RFC2231 we dont support locale for now
                response.setHeader("Content-Disposition", "attachment; filename*=utf-8'en-us'"+name);
            }
            if(file != null) {
            	if (!file.exists())
            		throw new UnexpectedException ("Your file buffer does not exists");
            	if (!file.canRead())
            		throw new UnexpectedException ("Can't read your file buffer");
            	if (!file.isFile())
            		throw new UnexpectedException ("Your file buffer is not a file");
                response.direct = file;
            } else {
                byte[] buffer = new byte[8092];
                int count = 0;
                while((count = is.read(buffer))>0) {
                    response.out.write(buffer, 0, count);
                }
                is.close();
            }
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }        
    }

}
