package play.db.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceUnit;
import play.exceptions.JPAException;
import play.Play;
import play.Invoker.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import javax.persistence.*;
import play.db.DB;
import play.Logger;
/**
 * JPA Support
 */
public class JPA {

    protected static Map<String,EntityManagerFactory> emfs = new ConcurrentHashMap<String,EntityManagerFactory>();
    public static ThreadLocal<Map<String, JPAContext>> currentEntityManager = new ThreadLocal<Map<String, JPAContext>>();
    public static String DEFAULT = "default";

    public static class JPAContext {
        public EntityManager entityManager;
        public boolean readonly = true;
        public boolean autoCommit = false;
    }

    public static boolean isInitialized(){
        return (currentEntityManager.get() != null);
    }
    public static Map<String, JPAContext> get() {
        if (!isInitialized()) {
            throw new JPAException("The JPA context is not initialized. JPA Entity Manager automatically start when one or more classes annotated with the @javax.persistence.Entity annotation are found in the application.");
        }
        return currentEntityManager.get();
    }

    static void clearContext() {
         currentEntityManager.remove();
    }

    static void createContext(EntityManager entityManager, boolean readonly) {
        if (currentEntityManager.get() != null) {
            try {
                currentEntityManager.get().get(DEFAULT).entityManager.close();
            } catch (Exception e) {
                // Let's it fail
            }
            currentEntityManager.remove();
        }
       bindForCurrentThread(DEFAULT, entityManager, readonly);
    }

    public static EntityManager newEntityManager(String key) {
        JPAPlugin jpaPlugin = Play.plugin(JPAPlugin.class);
        if(jpaPlugin == null) {
            throw new RuntimeException("No JPA Plugin.");
        }

        EntityManager em = jpaPlugin.em(key);
        if(em == null) {
            throw new RuntimeException("No JPA EntityManagerFactory configured for name [" + key + "]");
        }
        return em;
    }
    /**
     * Get the EntityManager for specified persistence unit for this thread.
     */
    public static EntityManager em(String key) {
        if ( currentEntityManager.get() != null &&  currentEntityManager.get().get(key) != null)
            return  currentEntityManager.get().get(key).entityManager;
        return newEntityManager(key);
    } 

     /**
     * Bind an EntityManager to the current thread.
     */
    public static void bindForCurrentThread(String name, EntityManager em, boolean readonly) {
      
        JPAContext context = new JPAContext();
        context.entityManager = em;
        context.readonly = readonly;

        // Get all our context for our current thread
        Map<String, JPAContext> jpaContexts = currentEntityManager.get();
        if (jpaContexts == null) {
            jpaContexts = new ConcurrentHashMap<String, JPAContext>();
        }
        jpaContexts.put(name, context);

        currentEntityManager.set(jpaContexts);
    }

    public static void unbindForCurrentThread(String name) {
      
        // Get all our context for our current thread
        Map<String, JPAContext> jpaContexts = currentEntityManager.get();
        if (jpaContexts != null) {
            jpaContexts.remove(name);
            // Remove our em
            if (jpaContexts.isEmpty()) {
                currentEntityManager.remove();
            } else {
                currentEntityManager.set(jpaContexts);
            }
        }
    }

    // ~~~~~~~~~~~
    /*
     * Retrieve the current entityManager
     */
    public static EntityManager em() {
        if (currentEntityManager.get() != null &&  currentEntityManager.get().get(DEFAULT) != null)
            return  currentEntityManager.get().get(DEFAULT).entityManager;
        return newEntityManager(DEFAULT);
    }

    /*
     * Tell to JPA do not commit the current transaction
     */
    public static void setRollbackOnly() {
         setRollbackOnly(DEFAULT);
    }

    public static void setRollbackOnly(String em) {
         currentEntityManager.get().get(em).entityManager.getTransaction().setRollbackOnly();
    }

    /**
     * @return true if an entityManagerFactory has started
     */
    public static boolean isEnabled() {
        return isEnabled(DEFAULT);
    }

    public static boolean isEnabled(String em) {
        return emfs.get(em) != null;
    }

    /**
     * Execute a JPQL query
     */
    public static int execute(String query) {
        return execute(DEFAULT, query);
    }

    public static int execute(String em, String query) {
        return em(em).createQuery(query).executeUpdate();
    }

    
    //  * Build a new entityManager.
    //  * (In most case you want to use the local entityManager with em)
     
    public static EntityManager newEntityManager() {
        return createEntityManager();
    }

    public static EntityManager createEntityManager() {
      return createEntityManager(JPA.DEFAULT);
    }

    public static EntityManager createEntityManager(String name) {
        if (isEnabled(name)) {
            EntityManager manager = emfs.get(name).createEntityManager();
            return manager;
        }
        return null;
    }

    /**
     * @return true if current thread is running inside a transaction
     */
    public static boolean isInsideTransaction() {
        return isInsideTransaction(DEFAULT);
    }

    public static boolean isInsideTransaction(String name) {
        try {
            EntityManager em = (currentEntityManager.get() != null && currentEntityManager.get().get(name) != null) ?
                em = currentEntityManager.get().get(name).entityManager : null;
            if (em == null) {
                return false;
            }
            EntityTransaction transaction = em.getTransaction();
            return transaction != null;
        } catch (JPAException e) {
            return false;
        }
    }

    public static <T> T withinFilter(play.libs.F.Function0<T> block) throws Throwable {
        if(InvocationContext.current().getAnnotation(NoTransaction.class) != null ) {
            //Called method or class is annotated with @NoTransaction telling us that
            //we should not start a transaction
            return block.apply();
        }

        boolean readOnly = false;
        String name = DEFAULT;
        Transactional tx = InvocationContext.current().getAnnotation(Transactional.class);
        if (tx != null) {
            readOnly = tx.readOnly();
        }
        PersistenceUnit pu = InvocationContext.current().getAnnotation(PersistenceUnit.class);
        if (pu != null) {
            name = pu.name();
        }

        return withTransaction(name, readOnly, block);
    }


    public static String getDBName(Class clazz) {
        String name = JPA.DEFAULT;
        PersistenceUnit pu = (PersistenceUnit)clazz.getAnnotation(PersistenceUnit.class);
        if (pu != null) {
            name = pu.name();
        }
        return name;
    }


    /**
     * Run a block of code in a JPA transaction.
     *
     * @param dbName The persistence unit name
     * @param readOnly Is the transaction read-only?
     * @param block Block of code to execute.
     */
    public static <T> T withTransaction(String dbName, boolean readOnly, play.libs.F.Function0<T> block) throws Throwable {
        if (isEnabled()) {
            List<EntityManager> em = new ArrayList<EntityManager>();
            List<EntityTransaction> tx = new ArrayList<EntityTransaction>();
            boolean closeEm = true;
            // For each existing persisence unit
           
            try {
                // we are starting a transaction for all known persistent unit
                // this is probably not the best, but there is no way we can know where to go from
                // at this stage
                for (String name : emfs.keySet()) {
                    EntityManager localEm = JPA.newEntityManager(name);
                    JPA.bindForCurrentThread(name, localEm, readOnly);
                    em.add(localEm);

                    if (!readOnly) {
                        EntityTransaction localTx = localEm.getTransaction();
                    
                        localTx.begin();
                        tx.add(localTx);
                    }
                }

                T result = block.apply();
              
                boolean rollbackAll = false;
                // Get back our entity managers
                // Because people might have mess up with the current entity managers
                Map<String, JPAContext> ems = currentEntityManager.get();
                for (String db : ems.keySet()) {
                    EntityManager m = ems.get(db).entityManager;
                    EntityTransaction localTx = m.getTransaction();
                    // The resource transaction must be in progress in order to determine if it has been marked for rollback
                    if (localTx.isActive() && localTx.getRollbackOnly()) {
                        rollbackAll = true;
                    }
                }

                for (String db : ems.keySet()) {
                    EntityManager m = ems.get(db).entityManager;
                    boolean ro = ems.get(db).readonly;
                    EntityTransaction localTx = m.getTransaction();
                    // transaction must be active to make some rollback or commit
                    if (localTx.isActive()) {
                        if (rollbackAll || ro) {
                            localTx.rollback();
                        } else {
                            localTx.commit();
                        }
                    }
                }

                return result;
            } catch (Suspend e) {
                // Nothing, transaction is in progress
                closeEm = false;
                throw e;
            } catch(Throwable t) {
                if(tx != null) {
                    // Because people might have mess up with the current entity managers
                    Map<String, JPAContext> ems = currentEntityManager.get();
                    for (String db : ems.keySet()) {
                        EntityManager m = ems.get(db).entityManager;
                        EntityTransaction localTx = m.getTransaction();
                        try { 
                            // transaction must be active to make some rollback or commit
                            if (localTx.isActive()) {
                                localTx.rollback(); 
                            }
                        } catch(Throwable e) {
                            
                        }
                    }
                }
                
                throw t;
            } finally {
                if (closeEm) {
                    Map<String, JPAContext> ems = currentEntityManager.get();
                    for (String db : ems.keySet()) {
                        EntityManager localEm = ems.get(db).entityManager;
                        if (localEm.isOpen()) {
                            localEm.close();
                        }
                        JPA.clearContext();
                    }
                    for (String name : emfs.keySet()) {
                        JPA.unbindForCurrentThread(name);
                    }
               }
            }      
        } else {
            return block.apply();
        }
    }

     /**
     * initialize the JPA context and starts a JPA transaction
     *
     * @param name The persistence unit name
     * @param readOnly true for a readonly transaction
     */
    public static void startTx(String name, boolean readOnly) {
        EntityManager manager = createEntityManager(name);
        manager.setFlushMode(FlushModeType.COMMIT);
        manager.setProperty("org.hibernate.readOnly", readOnly);
        manager.getTransaction().begin();
        createContext(manager, readOnly);
    }

    public static void closeTx(String name) {
        if (JPA.isInsideTransaction()) {
             EntityManager manager = em(name);
             try {
                    // Be sure to set the connection is non-autoCommit mode as some driver will complain about COMMIT statement
                try {
                    DB.getConnection().setAutoCommit(false);
                } catch(Exception e) {
                    Logger.error(e, "Why the driver complains here?");
                }
                    // Commit the transaction
                if (manager.getTransaction().isActive()) {
                    if (JPA.get().get(name).readonly || manager.getTransaction().getRollbackOnly()) {
                        manager.getTransaction().rollback();
                    } else {
                        try {
                            manager.getTransaction().commit();
                        } catch (Throwable e) {
                            for (int i = 0; i < 10; i++) {
                                if (e instanceof PersistenceException && e.getCause() != null) {
                                    e = e.getCause();
                                    break;
                                }
                                e = e.getCause();
                                if (e == null) {
                                    break;
                                }
                            }
                            throw new JPAException("Cannot commit", e);
                        }
                    }
                }
            } finally {
                if (manager.isOpen()) {
                    manager.close();
                }
                JPA.clearContext();
            }
        }
    }

    public static void rollbackTx(String name) {
        if (JPA.isInsideTransaction()) {
             EntityManager manager = em(name);
             try {
                    // Be sure to set the connection is non-autoCommit mode as some driver will complain about COMMIT statement
                try {
                    DB.getConnection().setAutoCommit(false);
                } catch(Exception e) {
                    Logger.error(e, "Why the driver complains here?");
                }
                    // Commit the transaction
                if (manager.getTransaction().isActive()) {
                    try {
                       manager.getTransaction().rollback();
                   } catch (Throwable e) {
                        for (int i = 0; i < 10; i++) {
                            if (e instanceof PersistenceException && e.getCause() != null) {
                                e = e.getCause();
                                break;
                            }
                            e = e.getCause();
                            if (e == null) {
                                break;
                            }
                        }
                        throw new JPAException("Cannot commit", e);
                    }
                }
        
            } finally {
                if (manager.isOpen()) {
                    manager.close();
                }
                JPA.clearContext();
            }
        }
    }

}
