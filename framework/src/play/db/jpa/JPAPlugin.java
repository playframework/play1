package play.db.jpa;

import java.lang.reflect.Field;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import org.hibernate.ejb.Ejb3Configuration;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.db.DB;

public class JPAPlugin extends PlayPlugin {

    @Override
    public void onApplicationStart() {
        if (Play.configuration.getProperty("jpa", "disabled").equals("enabled") && (JPA.entityManagerFactory == null)) {
            List<Class> classes = Play.classloader.getAllClasses();
            if (DB.datasource == null) {
                Logger.fatal("Cannot enable JPA without a valid database");
                Play.configuration.setProperty("jpa", "disabled");
                return;
            }
            Ejb3Configuration cfg = new Ejb3Configuration();
            cfg.setDataSource(DB.datasource);
            cfg.setProperty("hibernate.hbm2ddl.auto", Play.configuration.getProperty("jpa.ddl", "update"));
            cfg.setProperty("hibernate.dialect", getDefaultDialect(Play.configuration.getProperty("db.driver")));
            cfg.setProperty("javax.persistence.transaction", "RESOURCE_LOCAL");
            try {
                Field field = cfg.getClass().getDeclaredField("overridenClassLoader");
                field.setAccessible(true);
                field.set(cfg, Play.classloader);
            } catch (Exception e) {
                e.printStackTrace();
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
    public void onInvocationException(Throwable e) {
        closeTx(true);
    }    
    
    public static void startTx(boolean readonly) {
        if(!JPA.isEnabled()) {
            return;
        }
        EntityManager manager = JPA.entityManagerFactory.createEntityManager();
        manager.getTransaction().begin();
        JPAContext.createContext(manager, readonly);
    }

    public static void closeTx(boolean rollback) {
        if(!JPA.isEnabled() || JPAContext.local.get() == null) {
            return;
        }
        EntityManager manager = JPAContext.get().entityManager;
        if (JPAContext.get().readonly || rollback) {
            manager.getTransaction().rollback();
        } else {
            manager.getTransaction().commit();
        }
        JPAContext.clearContext();
    }
    
}
