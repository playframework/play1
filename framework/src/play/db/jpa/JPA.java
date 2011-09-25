package play.db.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.ejb.Ejb3Configuration;

import play.Logger;
import play.db.DBConfig;
import play.exceptions.JPAException;


/**
 * JPA Support
 *
 * This class holds reference to all JPA configurations.
 * Each configuration has its own instance of JPAConfig.
 *
 * dbConfigName corresponds to properties-names in application.conf.
 *
 * The default DBConfig is the one configured using 'db.' in application.conf
 *
 * dbConfigName = 'other' is configured like this:
 *
 * db_other = mem
 * db_other.user = batman
 *
 * This class also preserves backward compatibility by
 * directing static methods to the default JPAConfig-instance
 *
 * A particular JPAConfig-instance uses the DBConfig with the same configName
 */
public class JPA {

    /**
     * Holds ref to the default jpa config named defaultJPAConfigName.
     * Don't use this property directly in the code.
     * use getDefaultJPAConfig() - which does some checking
     */
    private static JPAConfig _defaultJPAConfig = null;
    private final static Map<String, JPAConfig> jpaConfigs = new HashMap<String, JPAConfig>(1);

    protected static void addConfiguration(String configName, Ejb3Configuration cfg) {
        JPAConfig jpaConfig = new JPAConfig(cfg, configName);
        jpaConfigs.put(configName, jpaConfig);
        if( DBConfig.defaultDbConfigName.equals(configName)) {
            _defaultJPAConfig = jpaConfig;
            JPQL.createSingleton();
        }
    }

    public static JPAConfig getJPAConfig(String jpaConfigName) {
        return getJPAConfig(jpaConfigName, false);
    }

    public static JPAConfig getJPAConfig(String jpaConfigName, boolean ignoreError) {
        JPAConfig jpaConfig = jpaConfigs.get(jpaConfigName);
        if (jpaConfig==null && !ignoreError) {
            if (!isEnabled()) {
                // Show simpler error message if JPA is not enabled
                throw new JPAException("The JPA context is not initialized. JPA Entity Manager automatically start " +
                        "when one or more classes annotated with the @javax.persistence.Entity annotation " +
                        "are found in the application.");
            } else {
                throw new JPAException("No JPAConfig is found with the name " + jpaConfigName);
            }
        }
        return jpaConfig;
    }

    protected static void close() {
        for( JPAConfig jpaConfig : jpaConfigs.values()) {
            // do our best to close the JPA config
            try {
                jpaConfig.close();
            } catch (Exception e) {
                Logger.error("Error closing JPA config "+jpaConfig.getConfigName(), e);
            }
        }
        jpaConfigs.clear();
        _defaultJPAConfig = null;
    }

    /**
     * clear current JPA context and transaction
     * @param rollback shall current transaction be committed (false) or cancelled (true)
     */
    public static void closeTx(boolean rollback) {
        boolean error = false;
        for (JPAConfig jpaConfig : jpaConfigs.values()) {
            if (jpaConfig.threadHasJPAContext()) {
                // do our best to take care of this transaction
                try {
                    jpaConfig.getJPAContext().closeTx(rollback);
                } catch (Exception e) {
                    Logger.error("Error closing transaction "+jpaConfig.getConfigName(), e);
                    error=true;
                }
            }
        }

        if (error) {
            throw new JPAException("Error closing one or more transactions");
        }
    }

    private static JPAConfig getDefaultJPAConfig() {
        if (_defaultJPAConfig==null) {
            throw new JPAException("The JPA context is not initialized. JPA Entity Manager automatically start " +
                    "when one or more classes annotated with the @javax.persistence.Entity annotation " +
                    "are found in the application.");
        }
        return _defaultJPAConfig;
    }



    /*
     * Retrieve the current entityManager
     */
    public static EntityManager em() {
        return getDefaultJPAConfig().getJPAContext().em();
    }

    /*
    * Tell to JPA do not commit the current transaction
    */
    public static void setRollbackOnly() {
        getDefaultJPAConfig().getJPAContext().em().getTransaction().setRollbackOnly();
    }

    /**
     * @return true if an entityManagerFactory has started
     */
    public static boolean isEnabled() {
        return !jpaConfigs.isEmpty();
    }

    /**
     * Execute a JPQL query
     */
    public static int execute(String query) {
        return getDefaultJPAConfig().getJPAContext().em().createQuery(query).executeUpdate();
    }

    /*
     * Build a new entityManager.
     * (In most case you want to use the local entityManager with em)
     */
    public static EntityManager newEntityManager() {
        return getDefaultJPAConfig().newEntityManager();
    }

    /**
     * @return true if current thread is running inside a transaction
     */
    public static boolean isInsideTransaction() {
        return getDefaultJPAConfig().isInsideTransaction();
    }

    protected static void clear() {
        for (JPAConfig jpaConfig : jpaConfigs.values()) {
            if (jpaConfig.threadHasJPAContext()) {
                jpaConfig.getJPAContext().em().clear();
            }
        }
    }

    protected static void clearJPAContext() {
        for (JPAConfig jpaConfig : jpaConfigs.values()) {
            jpaConfig.clearJPAContext();
        }
    }
}
