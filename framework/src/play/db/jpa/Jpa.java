package play.db.jpa;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hibernate.ejb.Ejb3Configuration;
import play.Play;
import play.db.Db;

public class Jpa {
    public static EntityManagerFactory entityManagerFactory= null;
    
    public static void init () {
        if (Play.configuration.getProperty("jpa.start","false").equals("true") && (entityManagerFactory==null)) {
            List<Class> classes = Play.classloader.getAllClasses();
            init(classes,Play.configuration);
        }
    }
    
    public static boolean isEnabled () {
        return Play.configuration.getProperty("jpa.start","false").equals("true");
    }
    
    public static void init (List<Class> classes, Properties p) {
        Ejb3Configuration cfg = new Ejb3Configuration();
        cfg.setDataSource(Db.datasource);
        cfg.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        cfg.setProperty("hibernate.dialect", getDefaultDialect(p.getProperty("db.driver")));
        cfg.setProperty ("javax.persistence.transaction","RESOURCE_LOCAL");
        try {
            Field field = cfg.getClass().getDeclaredField("overridenClassLoader");
            field.setAccessible(true);
            field.set(cfg, Play.classloader);
        } catch(Exception e) {
            e.printStackTrace();
        }
        for (Class clazz : classes) {
            if (clazz.isAnnotationPresent(Entity.class)){
                cfg.addAnnotatedClass(clazz);
            }
        }
        entityManagerFactory = cfg.buildEntityManagerFactory();
    }
    
    public static void shutdown () {
        entityManagerFactory.close();
        entityManagerFactory=null;
    }
    
    public static EntityManager getEntityManager () {
        return entityManagerFactory.createEntityManager();
    }
    
    public static String getDefaultDialect (String driver) {
        if (driver.equals("org.hsqldb.jdbcDriver"))
            return "org.hibernate.dialect.HSQLDialect";
        else
            throw new UnsupportedOperationException ("I do not know which hibernate dialect tu user with "+
                    driver+", use the property jpa.dialect in config file");
    }
    
    public static void startTx (boolean readonly) {
        EntityManager manager = entityManagerFactory.createEntityManager();
        manager.getTransaction().begin();
        JpaContext.createContext(manager, readonly);
    }
    
    public static void closeTx (boolean rollback) {
        EntityManager manager = JpaContext.get().entityManager;
        if (JpaContext.get().readonly || rollback)
            manager.getTransaction().rollback();
        else
            manager.getTransaction().commit();
        JpaContext.clearContext();
    }
}
