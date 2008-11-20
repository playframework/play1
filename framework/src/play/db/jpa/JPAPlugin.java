package play.db.jpa;

import java.lang.reflect.Field;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import org.hibernate.ejb.Ejb3Configuration;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.db.DB;
import play.exceptions.JPAException;

public class JPAPlugin extends PlayPlugin {

    @Override
    public void onApplicationStart() {
        if (JPA.entityManagerFactory == null) {
            List<Class> classes = Play.classloader.getAnnotatedClasses(Entity.class);
            if(classes.isEmpty()) {
                return;
            }
            if (DB.datasource == null) {
                throw new JPAException("Cannot start a JPA manager without a properly configured database", new NullPointerException("No datasource"));
            }
            Ejb3Configuration cfg = new Ejb3Configuration();
            cfg.setDataSource(DB.datasource);
            cfg.setProperty("hibernate.hbm2ddl.auto", Play.configuration.getProperty("jpa.ddl", "update"));
            cfg.setProperty("hibernate.dialect", getDefaultDialect(Play.configuration.getProperty("db.driver")));
            cfg.setProperty("javax.persistence.transaction", "RESOURCE_LOCAL");
            cfg.setProperty("hibernate.show_sql", Play.configuration.getProperty("jpa.debugSQL", "false"));
            try {
                Field field = cfg.getClass().getDeclaredField("overridenClassLoader");
                field.setAccessible(true);
                field.set(cfg, Play.classloader);
            } catch (Exception e) {
                Logger.error(e, "Error trying to override the hibernate classLoader (new hibernate version ???)");
            }
            for (Class clazz : classes) {
                if (clazz.isAnnotationPresent(Entity.class)) {
                    cfg.addAnnotatedClass(clazz);
                    Logger.debug("JPA Model : %s", clazz);
                }
            }
            Logger.debug("Initializing JPA ...");
            JPA.entityManagerFactory = cfg.buildEntityManagerFactory();
        }
    }

    static String getDefaultDialect(String driver) {
        if (driver.equals("org.hsqldb.jdbcDriver")) {
            return "org.hibernate.dialect.HSQLDialect";
        } else {
            String dialect = Play.configuration.getProperty("jpa.dialect");
            if (dialect != null) {
                return dialect;
            }
            throw new UnsupportedOperationException("I do not know which hibernate dialect to use with " +
                    driver + ", use the property jpa.dialect in config file");
        }
    }

    @Override
    public void onApplicationStop() {
        if (JPA.entityManagerFactory != null) {
            JPA.entityManagerFactory.close();
            JPA.entityManagerFactory = null;
        }
    }

    @Override
    public void beforeInvocation() {
        startTx(false);
    }

    @Override
    public void afterInvocation() {
        closeTx(false);
    }
    
    @Override
    public void afterActionInvocation() {
        closeTx(false);
    }

    @Override
    public void onInvocationException(Throwable e) {
        closeTx(true);
    }    
    
    /**
     * initialize the JPA context and starts a JPA transaction
     * 
     * @param readonly true for a readonly transaction
     */
    public static void startTx(boolean readonly) {
        if(!JPA.isEnabled()) {
            return;
        }
        EntityManager manager = JPA.entityManagerFactory.createEntityManager();
        manager.getTransaction().begin();
        JPAContext.createContext(manager, readonly);
    }

    /**
     * clear current JPA context and transaction 
     * @param rollback shall current transaction be committed (false) or cancelled (true)
     */
    public static void closeTx(boolean rollback) {
        if(!JPA.isEnabled() || JPAContext.local.get() == null) {
            return;
        }
        EntityManager manager = JPAContext.get().entityManager;
        if(manager.getTransaction().isActive()) {
            if (JPAContext.get().readonly || rollback || manager.getTransaction().getRollbackOnly()) {
                manager.getTransaction().rollback();
            } else {
                try {
                    manager.getTransaction().commit();
                } catch(Throwable e) {
                    for(int i=0; i<10; i++ ) {
                        if(e instanceof RollbackException && e.getCause() != null) {
                            e = e.getCause();
                            break;
                        }
                        e = e.getCause();
                        if(e == null) break;
                    }
                    throw new JPAException("Cannot commit", e);
                }
            }
        }
        JPAContext.clearContext();
    }
    
}
