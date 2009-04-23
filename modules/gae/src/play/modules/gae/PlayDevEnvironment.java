package play.modules.gae;

import com.google.apphosting.api.ApiProxy.Environment;
import play.Play;
import play.mvc.Scope.Session;

public class PlayDevEnvironment implements Environment {

    public String getAppId() {
        return Play.applicationPath.getName();
    }

    public String getVersionId() {
        return "1.0";
    }

    public String getEmail() {
        return Session.current().get("__GAE_EMAIL");
    }

    public boolean isLoggedIn() {
        return Session.current().contains("__GAE_EMAIL");
    }

    public boolean isAdmin() {
        return Session.current().contains("__GAE_ISADMIN") && Session.current().get("__GAE_ISADMIN").equals("true");
    }

    public String getAuthDomain() {
        return "gmail.com";
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

