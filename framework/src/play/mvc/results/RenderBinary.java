package play.mvc.results;

import java.io.InputStream;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Result;

public class RenderBinary extends Result {
    
    InputStream is;
    
    public RenderBinary(InputStream is) {
        this.is = is;
    }

    @Override
    public void apply(Request request, Response response) {
        try {
            byte[] buffer = new byte[8092];
            int count = 0;
            while((count = is.read(buffer))>0) {
                response.out.write(buffer, 0, count);
            }
            is.close();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }        
    }

}
