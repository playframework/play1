package play.mvc.results;

import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * 401 Unauthorized
 */
public class Unauthorized extends Result {
    
    private final String realm;
    
    public Unauthorized(String realm) {
        super(realm);
        this.realm = realm;
    }

    @Override
    public void apply(Request request, Response response) {
        response.status = Http.StatusCode.UNAUTHORIZED;
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
    }

    public String getRealm() {
        return realm;
    }
}
