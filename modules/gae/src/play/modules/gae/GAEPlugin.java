package play.modules.gae;

import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;
import java.io.File;
import play.Logger;
import play.Play;
import play.PlayPlugin;

public class GAEPlugin extends PlayPlugin {
    
    public ApiProxy.Environment devEnvironment = null;

    @Override
    public void onApplicationStart() {

        if(ApiProxy.getCurrentEnvironment() == null) {
            Logger.warn("No Google App Engine environment found. Setting up a development environement.");
            devEnvironment = new PlayDevEnvironment();
            ApiProxy.setDelegate(new ApiProxyLocalImpl(new File(Play.applicationPath, "gae-dev")){});
        }
        
    }

    @Override
    public void beforeInvocation() {
        if(devEnvironment != null) {
            ApiProxy.setEnvironmentForCurrentThread(new PlayDevEnvironment());
        }
    }    
    

    @Override
    public void onConfigurationRead() {
        Play.configuration.setProperty("play.tmp", "none");
    }

}
