package play.mvc.results;

import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Result;

public class RenderText extends Result {
    
    String text;
    
    public RenderText(CharSequence text) {
        this.text = text.toString();
    }

    public void apply(Request request, Response response) {
        try {
            setContentTypeIfNotSet(response, "text/html");
            response.out.write(text.getBytes("utf-8"));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

}
