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
    
    protected void setContentTypeIfNotSet(Http.Response response, String contentType) {
       response.setContentTypeIfNotSet(contentType);
    }

}
