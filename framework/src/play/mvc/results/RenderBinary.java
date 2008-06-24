package play.mvc.results;

import java.io.InputStream;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

public class RenderBinary extends Result {
    
    InputStream is;
    String name;
    
    public RenderBinary(InputStream is, String name) {
        this.is = is;
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
            byte[] buffer = new byte[8092];
            int count = 0;
            while((count = is.read(buffer))>0) {
                response.out.write(buffer, 0, count);
            }
            is.close();
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }        
    }

}
