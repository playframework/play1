package play.mvc.results;

import play.mvc.Http;

/**
 * 400 Bad Request
 */
public class BadRequest extends Error {

    public BadRequest(String msg) {
      super(Http.StatusCode.BAD_REQUEST, msg);
    }
}
