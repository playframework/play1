package play.db.jpa;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.Validator;
import org.apache.log4j.Level;
import org.hibernate.EmptyInterceptor;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.type.Type;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.db.DB;
import play.exceptions.JPAException;
import play.libs.Utils;

/**
 * JPA Plugin
 */
public class JPAPlugin extends PlayPlugin {

    public static boolean autoTxs = true;

    @Override
    public void onApplicationStart() {
        if (JPA.entityManagerFactory == null) {
            List<Class> classes = Play.classloader.getAnnotatedClasses(Entity.class);
            if (classes.isEmpty()) {
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
            cfg.setInterceptor(new EmptyInterceptor() {

                /**
                 * Discard change when an entity is not valid
                 */
                @Override
                public int[] findDirty(Object o, Serializable id, Object[] arg2, Object[] arg3, String[] arg4, Type[] arg5) {
                    if(o instanceof JPASupport && !((JPASupport)o).willBeSaved) {
                        return new int[0];
                    }
                    return null;
                }
                
                
            });
            if(Play.configuration.getProperty("jpa.debugSQL", "false").equals("true")) {
                org.apache.log4j.Logger.getLogger("org.hibernate.SQL").setLevel(Level.ALL);
            } else {
                org.apache.log4j.Logger.getLogger("org.hibernate.SQL").setLevel(Level.OFF);
            }
            // inject additional  hibernate.* settings declared in Play! configuration
            cfg.addProperties((Properties) Utils.Maps.filterMap(Play.configuration, "^hibernate\\..*"));

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
                    Logger.trace("JPA Model : %s", clazz);
                }
            }
            Logger.trace("Initializing JPA ...");
            try {
                JPA.entityManagerFactory = cfg.buildEntityManagerFactory();
            } catch (PersistenceException e) {
                throw new JPAException(e.getMessage(), e.getCause() != null ? e.getCause() : e);
            }
            JPQLDialect.instance = new JPQLDialect();
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

    @Override
    public void invocationFinally() {
        closeTx(true);
    }    

    /**
     * initialize the JPA context and starts a JPA transaction
     * 
     * @param readonly true for a readonly transaction
     */
    public static void startTx(boolean readonly) {
        if (!JPA.isEnabled()) {
            return;
        }
        EntityManager manager = JPA.entityManagerFactory.createEntityManager();
        if (autoTxs) {
            manager.getTransaction().begin();
        }
        JPA.createContext(manager, readonly);
    }

    /**
     * clear current JPA context and transaction 
     * @param rollback shall current transaction be committed (false) or cancelled (true)
     */
    public static void closeTx(boolean rollback) {
        if (!JPA.isEnabled() || JPA.local.get() == null) {
            return;
        }
        EntityManager manager = JPA.get().entityManager;
        try {
            if (autoTxs) {
                if (manager.getTransaction().isActive()) {
                    if (JPA.get().readonly || rollback || manager.getTransaction().getRollbackOnly()) {
                        manager.getTransaction().rollback();
                    } else {
                        try {
                            if (autoTxs) {
                                manager.getTransaction().commit();
                            }
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
            }
        } finally {
            manager.close();
            JPA.clearContext();
        }
    }
}
