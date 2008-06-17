package play.libs;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.verisign.joid.OpenIdException;
import org.verisign.joid.consumer.AuthenticationResult;
import org.verisign.joid.consumer.JoidConsumer;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.Scope.Params;
import play.mvc.results.Redirect;

public class OpenID {
    
    static JoidConsumer joidConsumer = new JoidConsumer();
    
    public static void verify(String openID, String returnAction) throws OpenIdException {
        openID = openID.trim();
        if(!openID.startsWith("http://")) {
            openID = "http://" + openID;
        }
        String rootUrl = Request.current().getBase() + "/";
        String returnTo =  Request.current().getBase() + Router.reverse(returnAction);
        String url = joidConsumer.getAuthUrl(openID, returnTo, rootUrl);
        throw new Redirect(url);
    }
    
    public static void verify(String openID) throws OpenIdException {
        verify(openID, Request.current().action);
    }
    
    public static String getVerifiedID() throws OpenIdException {
        try {
            AuthenticationResult result = joidConsumer.authenticate(Params.current().allSimple());
            return result.getIdentity();
        } catch(IOException e) {
            throw new UnexpectedException(e);
        } catch(NoSuchAlgorithmException e) {
            throw new UnexpectedException(e);
        }
    }

}
