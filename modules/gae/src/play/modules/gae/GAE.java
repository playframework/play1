package play.modules.gae;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;

public class GAE {
    
    public static DatastoreService getDatastore() {
        return DatastoreServiceFactory.getDatastoreService();
    }

}
