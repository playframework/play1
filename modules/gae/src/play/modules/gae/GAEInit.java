package play.modules.gae;

import com.google.apphosting.api.ApiProxy;
import org.apache.log4j.helpers.LogLog;
import play.Logger;
import play.Play;

public class GAEInit {

    static {
        if(ApiProxy.getCurrentEnvironment() != null) {
            LogLog.setQuietMode(true);
            Logger.forceJuli = true;
            Logger.setUp("DEBUG");
            Logger.info("Play! is running in Google App Engine");
            Play.readOnlyTmp = true;
            Play.lazyLoadTemplates = true;
        } else {
            Logger.redirectJuli = true;
        }
    }
    
}
