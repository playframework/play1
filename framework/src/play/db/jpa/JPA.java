package play.db.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class JPA {

    public static EntityManagerFactory entityManagerFactory = null;
 
    public static boolean isEnabled() {
        return entityManagerFactory != null;
    }

    public static EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }    

    
}
