package play.modules.gae;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import play.mvc.Http.Request;
import play.mvc.Router;
import play.mvc.results.Redirect;

public class GAE {
    
    public static DatastoreService getDatastore() {
        return DatastoreServiceFactory.getDatastoreService();
    }

    public static UserService getUserService() {
        return UserServiceFactory.getUserService();
    }

    public static URLFetchService getURLFetchService() {
        return URLFetchServiceFactory.getURLFetchService();
    }
    
    public static void login(String returnAction) {
        String returnURL = Router.getFullUrl(returnAction);
        String url = getUserService().createLoginURL(returnURL);
        throw new Redirect(url);
    }
    
    public static void login() {
        login(Request.current().action);
    }
    
    public static User getUser() {
        return getUserService().getCurrentUser();
    }
    
    public static boolean isLoggedIn() {
        return getUserService().isUserLoggedIn();
    }
    
    public static boolean isAdmin() {
        return getUserService().isUserAdmin();
    }
    
    public static void logout(String returnAction) {
        String returnURL = Router.getFullUrl(returnAction);
        String url = getUserService().createLogoutURL(returnURL);
        throw new Redirect(url);
    }
    
    public static void logout() {
        logout(Request.current().action);
    }

}
