package play.modules.siena;

import play.Play;
import play.PlayPlugin;
import siena.Model;
import siena.PersistenceManager;
import siena.PersistenceManagerFactory;
import siena.gae.GaePersistenceManager;
import siena.jdbc.JdbcPersistenceManager;

public class SienaPlugin extends PlayPlugin {
    
    PersistenceManager persistenceManager;

    @Override
    public void onApplicationStart() {
        // GAE ?
        boolean gae = false;       
        for(PlayPlugin plugin : Play.plugins) {
            if(plugin.getClass().getSimpleName().equals("GAEPlugin")) {
                gae = true;
                break;
            }
        }
        
        // The peristence manager
        if(!gae) {
            persistenceManager = new JdbcPersistenceManager(new PlayConnectionManager(), null);
        } else {
            persistenceManager = new GaePersistenceManager();
            persistenceManager.init(null);
        }
        
        // Install all classes
        for(Class c : Play.classloader.getAssignableClasses(Model.class)) {
            PersistenceManagerFactory.install(persistenceManager, c);
        }
    }
    
}
