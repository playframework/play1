package play.mvc;

public abstract class Result extends RuntimeException {
    
    public abstract void apply(Http.Request request, Http.Response response);

}
