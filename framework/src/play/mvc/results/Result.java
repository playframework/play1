package play.mvc.results;

import play.mvc.Http;

/**
 * Result support
 */
public abstract class Result extends RuntimeException {
    
    public Result() {
    } 
    
    public Result(String description) {
        super(description);
    }
    
    public abstract void apply(Http.Request request, Http.Response response);
    
    public static void setContentTypeIfNotSet(Http.Response response, String contentType) {
        if(response.contentType == null) {
            response.contentType = contentType;
        }
    }

}
