package play.mvc.results;

import play.mvc.Http;

/**
 * 403 Forbidden
 */
public class Forbidden extends Error {

    public Forbidden(String reason) {
        super(Http.StatusCode.FORBIDDEN, reason);
    }
}
