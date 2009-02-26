package play.mvc.results;

import play.mvc.Http.Request;
import play.mvc.Http.Response;
import fr.zenexity.json.JSON;
import play.exceptions.UnexpectedException;

/**
 * 200 OK with application/json
 */
public class RenderJson extends Result {

    String json;

    public RenderJson(Object o, String... includes) {
        json = JSON.toJSON(o);
    }
    
    public RenderJson(String jsonString) {
        json = jsonString;
    }

    public void apply(Request request, Response response) {
        try {
            setContentTypeIfNotSet(response, "application/json");
            response.out.write(json.getBytes("utf-8"));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
} 
