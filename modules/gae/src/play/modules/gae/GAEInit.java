package play.modules.gae;

import com.google.apphosting.api.ApiProxy;
import org.apache.log4j.helpers.LogLog;
import play.Logger;

public class GAEInit {

    static {
        Logger.redirectJuli = true;
        if(ApiProxy.getCurrentEnvironment() != null) {
            LogLog.setQuietMode(true);
            Logger.forceJuli = true;
            Logger.setUp("DEBUG");
            Logger.info("Play! is running in Google App Engine");
        }
    }
    
}
