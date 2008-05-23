package play.mvc;

public abstract class Result extends RuntimeException {
    
    public abstract void apply(Http.Request request, Http.Response response);
    
    protected void setContentTypeIfNotSet(Http.Response response, String contentType) {
        if(response.contentType == null) {
            response.contentType = contentType;
        }
    }

}
