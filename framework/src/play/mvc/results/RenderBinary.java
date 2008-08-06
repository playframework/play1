package play.mvc.results;

import java.io.File;
import java.io.InputStream;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class RenderBinary extends Result {
    
    File file;
    InputStream is;
    String name;
    
    public RenderBinary(InputStream is, String name) {
        this.is = is;
        this.name = name;
    }
    
    public RenderBinary(File file, String name) {
        this.file = file;
        this.name = name;
    }

    @Override
    public void apply(Request request, Response response) {
        try {
            setContentTypeIfNotSet(response, "application/octet-stream");
            if(name == null) {
                response.setHeader("Content-disposition", "attachment");
            } else {
                response.setHeader("Content-disposition", "attachment; filename="+name);
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
