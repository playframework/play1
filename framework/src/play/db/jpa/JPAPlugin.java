package play.db.jpa;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.type.Type;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.data.binding.Binder;
import play.data.binding.NoBinding;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;
import play.db.DB;
import play.db.DBConfig;
import play.db.Model;
import play.exceptions.JPAException;
import play.exceptions.UnexpectedException;
import play.utils.Utils;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.Transient;
import javax.persistence.*;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * JPA Plugin
 */
public class JPAPlugin extends PlayPlugin {

    public static boolean autoTxs = true;

    @Override
    public Object bind(RootParamNode rootParamNode, String name, Class clazz, java.lang.reflect.Type type, Annotation[] annotations) {
        // TODO need to be more generic in order to work with JPASupport
    	if(clazz.isAnnotationPresent(Entity.class)) {

            ParamNode paramNode = rootParamNode.getChild(name, true);

            String[] keyNames = new JPAModelLoader(clazz).keyNames();
            ParamNode[] ids = new ParamNode[keyNames.length];
            // Collect the matching ids
            int i = 0;
            for (String keyName : keyNames) {
                ids[i++] = paramNode.getChild(keyName, true);
            }
            if (ids != null && ids.length > 0) {
                try {
                    EntityManager em = JPABase.getJPAConfig(clazz).getJPAContext().em();
                    StringBuilder q = new StringBuilder().append("from ").append(clazz.getName()).append(" o where");
                    int keyIdx = 1;
                    for (String keyName : keyNames) {
                            q.append(" o.").append(keyName).append(" = ?").append(keyIdx++).append(" and ");
                    }
                    if (q.length() > 4) {
                        q = q.delete(q.length() - 4, q.length());
                    }
                    Query query = em.createQuery(q.toString());
                    // The primary key can be a composite.
                    Class[] pk = new JPAModelLoader(clazz).keyTypes();
                    int j = 0;
                    for (ParamNode id : ids) {
                        if (id.getValues() == null || id.getValues().length == 0 || id.getFirstValue(null)== null || id.getFirstValue(null).trim().length() <= 0 ) {
                             // We have no ids, it is a new entity
                            return GenericModel.create(rootParamNode, name, clazz, annotations);
                        }
                        query.setParameter(j + 1, Binder.directBind(id.getOriginalKey(), annotations, id.getValues()[0], pk[j++], null));

                    }
                    Object o = query.getSingleResult();
                    return GenericModel.edit(rootParamNode, name, o, annotations);
                } catch (NoResultException e) {
                    // ok
                } catch (Exception e) {
                    throw new UnexpectedException(e);
                }
            }
            return GenericModel.create(rootParamNode, name, clazz, annotations);
        }
        return null;
    }

    @Override
    public Object bindBean(RootParamNode rootParamNode, String name, Object bean) {
        if (bean instanceof JPABase) {
            return GenericModel.edit(rootParamNode, name, bean, null);
        }
        return null;
    }

    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        new JPAEnhancer().enhanceThisClass(applicationClass);
    }


    /**
     * returns empty string if default config.
     * returns descriptive string about config name if not default config
     */
    protected String getConfigInfoString(String configName) {
        if (DBConfig.defaultDbConfigName.equals(configName)) {
            return "";
        } else {
            return " (jpa config name: " + configName + ")";
        }
    }


    @Override
    public void onApplicationStart() {

        // must check and configure JPA for each DBConfig
        for (DBConfig dbConfig : DB.getDBConfigs()) {
            // check and enable JPA on this config

            // is JPA already configured?
            String configName = dbConfig.getDBConfigName();

            if (JPA.getJPAConfig(configName, true) == null) {
                //must configure it

                // resolve prefix for hibernate config..
                // should be nothing for default, and db_<name> for others
                String propPrefix = "";
                if (!DBConfig.defaultDbConfigName.equalsIgnoreCase(configName)) {
                    propPrefix = "db_" + configName + ".";
                }
                List<Class> classes = findEntityClassesForThisConfig(configName, propPrefix);
                if (classes == null) continue;

                // we're ready to configure this instance of JPA
                final String hibernateDataSource = Play.configuration.getProperty(propPrefix + "hibernate.connection.datasource");

                if (StringUtils.isEmpty(hibernateDataSource) && dbConfig == null) {
                    throw new JPAException("Cannot start a JPA manager without a properly configured database" + getConfigInfoString(configName),
                            new NullPointerException("No datasource configured"));
                }

                Ejb3Configuration cfg = new Ejb3Configuration();

                if (dbConfig.getDatasource() != null) {
                    cfg.setDataSource(dbConfig.getDatasource());
                }

                if (!Play.configuration.getProperty(propPrefix + "jpa.ddl", Play.mode.isDev() ? "update" : "none").equals("none")) {
                    cfg.setProperty("hibernate.hbm2ddl.auto", Play.configuration.getProperty(propPrefix + "jpa.ddl", "update"));
                }

                String driver = null;
                if (StringUtils.isEmpty(propPrefix)) {
                    driver = Play.configuration.getProperty("db.driver");
                } else {
                    driver = Play.configuration.getProperty(propPrefix + "driver");
                }
                cfg.setProperty("hibernate.dialect", getDefaultDialect(propPrefix, driver));
                cfg.setProperty("javax.persistence.transaction", "RESOURCE_LOCAL");


                cfg.setInterceptor(new HibernateInterceptor());

                // This setting is global for all JPAs - only configure if configuring default JPA
                if (StringUtils.isEmpty(propPrefix)) {
                    if (Play.configuration.getProperty(propPrefix + "jpa.debugSQL", "false").equals("true")) {
                        org.apache.log4j.Logger.getLogger("org.hibernate.SQL").setLevel(Level.ALL);
                    } else {
                        org.apache.log4j.Logger.getLogger("org.hibernate.SQL").setLevel(Level.OFF);
                    }
                }
                // inject additional  hibernate.* settings declared in Play! configuration
                Properties additionalProperties = (Properties) Utils.Maps.filterMap(Play.configuration, "^" + propPrefix + "hibernate\\..*");
                // We must remove prefix from names
                Properties transformedAdditionalProperties = new Properties();
                for (Map.Entry<Object, Object> entry : additionalProperties.entrySet()) {
                    Object key = entry.getKey();
                    if (!StringUtils.isEmpty(propPrefix)) {
                        key = ((String) key).substring(propPrefix.length()); // chop off the prefix
                    }
                    transformedAdditionalProperties.put(key, entry.getValue());
                }
                cfg.addProperties(transformedAdditionalProperties);


                try {
                    // nice hacking :) I like it..
                    Field field = cfg.getClass().getDeclaredField("overridenClassLoader");
                    field.setAccessible(true);
                    field.set(cfg, Play.classloader);
                } catch (Exception e) {
                    Logger.error(e, "Error trying to override the hibernate classLoader (new hibernate version ???)");
                }

                for (Class<?> clazz : classes) {
                    cfg.addAnnotatedClass(clazz);
                    if (Logger.isTraceEnabled()) {
                        Logger.trace("JPA Model : %s", clazz);
                    }
                }
                String[] moreEntities = Play.configuration.getProperty(propPrefix + "jpa.entities", "").split(", ");
                for (String entity : moreEntities) {
                    if (entity.trim().equals("")) {
                        continue;
                    }
                    try {
                        cfg.addAnnotatedClass(Play.classloader.loadClass(entity));
                    } catch (Exception e) {
                        Logger.warn("JPA -> Entity not found: %s", entity);
                    }
                }

                for (ApplicationClass applicationClass : Play.classes.all()) {
                    if (applicationClass.isClass() || applicationClass.javaPackage == null) {
                        continue;
                    }
                    Package p = applicationClass.javaPackage;
                    Logger.info("JPA -> Adding package: %s", p.getName());
                    cfg.addPackage(p.getName());
                }

                String mappingFile = Play.configuration.getProperty(propPrefix + "jpa.mapping-file", "");
                if (mappingFile != null && mappingFile.length() > 0) {
                    cfg.addResource(mappingFile);
                }

                if (Logger.isTraceEnabled()) {
                    Logger.trace("Initializing JPA" + getConfigInfoString(configName) + " ...");
                }

                try {
                    JPA.addConfiguration(configName, cfg);
                } catch (PersistenceException e) {
                    throw new JPAException(e.getMessage() + getConfigInfoString(configName), e.getCause() != null ? e.getCause() : e);
                }

            }

        }

        // must look for Entity-objects referring to none-existing JPAConfig
        List<Class> allEntityClasses = Play.classloader.getAnnotatedClasses(Entity.class);
        for (Class clazz : allEntityClasses) {
            String configName = Entity2JPAConfigResolver.getJPAConfigNameForEntityClass(clazz);
            if (JPA.getJPAConfig(configName, true) == null) {
                throw new JPAException("Found Entity-class (" + clazz.getName() + ") referring to none-existing JPAConfig" + getConfigInfoString(configName) + ". " +
                        "Is JPA properly configured?");
            }
        }
    }

    private List<Class> findEntityClassesForThisConfig(String configName, String propPrefix) {
        //look and see if we have any Entity-objects for this db config
        List<Class> classes = Play.classloader.getAnnotatedClasses(Entity.class);

        // filter list on Entities meant for us..
        List<Class> filteredClasses = new ArrayList<Class>(classes.size());
        for (Class clazz : classes) {
            if (configName.equals(Entity2JPAConfigResolver.getJPAConfigNameForEntityClass(clazz))) {
                filteredClasses.add(clazz);
            }
        }


        if (!Play.configuration.getProperty(propPrefix + "jpa.entities", "").equals("")) {
            return filteredClasses;
        }

        if (filteredClasses.isEmpty()) {
            return null;
        }

        return filteredClasses;
    }


    public static String getDefaultDialect(String propPrefix, String driver) {
        String dialect = Play.configuration.getProperty(propPrefix + "jpa.dialect");
        if (dialect != null) {
            return dialect;
        } else if ("org.h2.Driver".equals(driver)) {
            return "org.hibernate.dialect.H2Dialect";
        } else if ("org.hsqldb.jdbcDriver".equals(driver)) {
            return "org.hibernate.dialect.HSQLDialect";
        } else if ("com.mysql.jdbc.Driver".equals(driver)) {
            return "play.db.jpa.MySQLDialect";
        } else if ("org.postgresql.Driver".equals(driver)) {
            return "org.hibernate.dialect.PostgreSQLDialect";
        } else if ("com.ibm.db2.jdbc.app.DB2Driver".equals(driver)) {
            return "org.hibernate.dialect.DB2Dialect";
        } else if ("com.ibm.as400.access.AS400JDBCDriver".equals(driver)) {
            return "org.hibernate.dialect.DB2400Dialect";
        } else if ("com.ibm.as400.access.AS390JDBCDriver".equals(driver)) {
            return "org.hibernate.dialect.DB2390Dialect";
        } else if ("oracle.jdbc.OracleDriver".equals(driver)) {
            return "org.hibernate.dialect.Oracle10gDialect";
        } else if ("com.sybase.jdbc2.jdbc.SybDriver".equals(driver)) {
            return "org.hibernate.dialect.SybaseAnywhereDialect";
        } else if ("com.microsoft.jdbc.sqlserver.SQLServerDriver".equals(driver)) {
            return "org.hibernate.dialect.SQLServerDialect";
        } else if ("com.sap.dbtech.jdbc.DriverSapDB".equals(driver)) {
            return "org.hibernate.dialect.SAPDBDialect";
        } else if ("com.informix.jdbc.IfxDriver".equals(driver)) {
            return "org.hibernate.dialect.InformixDialect";
        } else if ("com.ingres.jdbc.IngresDriver".equals(driver)) {
            return "org.hibernate.dialect.IngresDialect";
        } else if ("progress.sql.jdbc.JdbcProgressDriver".equals(driver)) {
            return "org.hibernate.dialect.ProgressDialect";
        } else if ("com.mckoi.JDBCDriver".equals(driver)) {
            return "org.hibernate.dialect.MckoiDialect";
        } else if ("InterBase.interclient.Driver".equals(driver)) {
            return "org.hibernate.dialect.InterbaseDialect";
        } else if ("com.pointbase.jdbc.jdbcUniversalDriver".equals(driver)) {
            return "org.hibernate.dialect.PointbaseDialect";
        } else if ("com.frontbase.jdbc.FBJDriver".equals(driver)) {
            return "org.hibernate.dialect.FrontbaseDialect";
        } else if ("org.firebirdsql.jdbc.FBDriver".equals(driver)) {
            return "org.hibernate.dialect.FirebirdDialect";
        } else {
            throw new UnsupportedOperationException("I do not know which hibernate dialect to use with "
                    + driver + " and I cannot guess it, use the property jpa.dialect in config file");
        }
    }

    @Override
    public void onApplicationStop() {
        JPA.close();
    }

    @Override
    public void beforeInvocation() {
        // just to be safe we must clear all possible previous
        // JPAContexts in this thread
        JPA.clearJPAContext();
    }

    @Override
    public void afterInvocation() {
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
     * if not already started.
     * <p/>
     * This method is not needed since transaction is created
     * automatically on first use.
     * <p/>
     * It is better to specify readonly like this: @Transactional(readOnly=true)
     *
     * @param readonly true for a readonly transaction
     * @deprecated use @Transactional with readOnly-property instead
     */
    @Deprecated
    public static void startTx(boolean readonly) {
        // Create new transaction by getting the JPAContext
        JPA.getJPAConfig(DBConfig.defaultDbConfigName).getJPAContext(readonly);
    }

    /**
     * clear current JPA context and transaction if JPAPlugin.autoTxs is true
     * When using multiple databases in the same request this method
     * tries to commit/rollback as many transactions as possible,
     * but there is not guaranteed that all transactions are committed.
     *
     * @param rollback shall current transaction be committed (false) or cancelled (true)
     */
    public static void closeTx(boolean rollback) {
        if (autoTxs) {
            JPA.closeTx(rollback);
        }
    }

    @Override
    public Model.Factory modelFactory(Class<? extends Model> modelClass) {
        if (modelClass.isAnnotationPresent(Entity.class)) {
            return new JPAModelLoader(modelClass);
        }
        return null;
    }

    @Override
    public void afterFixtureLoad() {
        JPA.clear();
    }

    

    
}
