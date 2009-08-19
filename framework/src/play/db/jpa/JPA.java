package play.db.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import play.exceptions.JPAException;

/**
 * JPA Support
 */
public class JPA {

    public static EntityManagerFactory entityManagerFactory = null;
    public static ThreadLocal<JPA> local = new ThreadLocal<JPA>();
    public EntityManager entityManager;
    boolean readonly = true;

    static JPA get() {
        if (local.get() == null) {
            throw new JPAException("The JPA context is not initialized. JPA Entity Manager automatically start when one or more classes annotated with the @javax.persistence.Entity annotation are found in the application.");
        }
        return local.get();
    }

    static void clearContext() {
        local.remove();
    }

    static void createContext(EntityManager entityManager, boolean readonly) {
        if (local.get() != null) {
            local.remove();
        }
        JPA context = new JPA();
        context.entityManager = entityManager;
        context.readonly = readonly;
        local.set(context);
    }
    
    // ~~~~~~~~~~~
    
    /*
     * Retrieve the current entityManager
     */ 
    @Deprecated
    public static EntityManager getEntityManager() {
        return get().entityManager;
    }
    
    /*
     * Retrieve the current entityManager
     */ 
    public static EntityManager em() {
        return get().entityManager;
    }
    
    /*
     * Tell to JPA do not commit the current transaction
     */ 
    @Deprecated
    public static void abort() {
        em().getTransaction().setRollbackOnly();
    }
    
    /*
     * Tell to JPA do not commit the current transaction
     */ 
    public static void setRollbackOnly() {
        em().getTransaction().setRollbackOnly();
    }

    /**
     * @return true if an entityManagerFactory has started
     */
    public static boolean isEnabled() {
        return entityManagerFactory != null;
    }
    
    /**
     * Execute a JPQL query
     */
    public static int execute(String query) {
        return em().createQuery(query).executeUpdate();
    }

    /*
     * Build a new entityManager.
     * (In most case you want to use the local entityManager with em)
     */ 
    public static EntityManager newEntityManager() {
        return entityManagerFactory.createEntityManager();
    }
}
