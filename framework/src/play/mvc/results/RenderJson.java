package play.mvc.results;

import play.mvc.Http.Request;
import play.mvc.Http.Response;
import fr.zenexity.json.JSON;
import play.exceptions.UnexpectedException;

public class RenderJson extends Result {

    Object o;
    String[] includes;

    public RenderJson(Object o, String... includes) {
        this.o = o;
        this.includes = includes;
    }

//    public void apply(Request request, Response response) {
//        try {
//            String json;
//            if (includes == null || includes.length == 0) {
//                json = new JSONSerializer().serialize(o);
//            } else {
//                json = new JSONSerializer().include(includes).exclude("*").serialize(o);
//            }
//            setContentTypeIfNotSet(response, "text/html");
//            response.out.write(json.getBytes("utf-8"));
//        } catch (Exception e) {
//            throw new UnexpectedException(e);
//        }
//    }

    public void apply(Request request, Response response) {
        try {
            String json = JSON.toJSON(o);
            setContentTypeIfNotSet(response, "application/json");
            response.out.write(json.getBytes("utf-8"));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
