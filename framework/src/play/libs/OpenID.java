package play.libs;

import org.verisign.joid.OpenIdException;
import org.verisign.joid.consumer.JoidConsumer;
import play.mvc.results.Redirect;

public class OpenID {
    
    static JoidConsumer joidConsumer = new JoidConsumer();
    
    public static void verify(String openID, String returnAction) throws OpenIdException {
        openID = openID.trim();
        if(!openID.startsWith("http://")) {
            openID = "http://" + openID;
        }
        String url = joidConsumer.getAuthUrl(openID, "http://localhost:9000/yop", "http://localhost:9000/");
        throw new Redirect(url);
    }

}
