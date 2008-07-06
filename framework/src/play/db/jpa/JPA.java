package play.db.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import play.Play;

public class JPA {

    public static EntityManagerFactory entityManagerFactory = null;
 
    public static boolean isEnabled() {
        return Play.configuration.getProperty("jpa", "disabled").equals("enabled");
    }

    public static EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }    

    
}
