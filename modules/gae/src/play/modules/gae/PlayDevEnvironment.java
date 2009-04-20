package play.modules.gae;

import com.google.apphosting.api.ApiProxy.Environment;
import play.Play;

public class PlayDevEnvironment implements Environment {

    public String getAppId() {
        return Play.applicationPath.getName();
    }

    public String getVersionId() {
        return "1.0";
    }

    public String getEmail() {
        return null;
    }

    public boolean isLoggedIn() {
        return false;
    }

    public boolean isAdmin() {
        return false;
    }

    public String getAuthDomain() {
        return null;
    }

    public String getRequestNamespace() {
        return null;
    }

    public String getDefaultNamespace() {
        return null;
    }

    public void setDefaultNamespace(String ns) {
    }

}

