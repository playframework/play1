package play.mvc.results;

import play.mvc.Http;
import play.utils.FastRuntimeException;

/**
 * Result support
 */
public abstract class Result extends FastRuntimeException {

    public Result() {
        super();
    }

    public Result(String description) {
        super(description);
    }

    public abstract void apply(Http.Request request, Http.Response response);

    protected void setContentTypeIfNotSet(Http.Response response, String contentType) {
        response.setContentTypeIfNotSet(contentType);
    }

    /**
     * The encoding that should be used when writing this response to the client
     */
    protected String getEncoding() {
        return Http.Response.current().encoding;
    }

}
