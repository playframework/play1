package play.mvc.results;

import com.google.gson.Gson;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.exceptions.UnexpectedException;

/**
 * 200 OK with application/json
 */
public class RenderJson extends Result {

    String json;

    public RenderJson(Object o) {
        json = new Gson().toJson(o);
    }
    
    public RenderJson(String jsonString) {
        json = jsonString;
    }

    public void apply(Request request, Response response) {
        try {
            setContentTypeIfNotSet(response, "application/json; charset=utf-8");
            response.out.write(json.getBytes("utf-8"));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
} 
