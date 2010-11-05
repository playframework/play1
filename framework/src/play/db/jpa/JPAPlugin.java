package play.db.jpa;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
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
import org.apache.log4j.Level;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.type.Type;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.db.DB;
import play.db.Model;
import play.exceptions.JPAException;
import play.utils.Utils;
import org.apache.commons.lang.StringUtils;
import play.data.binding.Binder;
import play.exceptions.UnexpectedException;

/**
 * JPA Plugin
 */
public class JPAPlugin extends PlayPlugin {

    public static boolean autoTxs = true;

    @Override
    @SuppressWarnings("unchecked")
    public Object bind(String name, Class clazz, java.lang.reflect.Type type, Annotation[] annotations, Map<String, String[]> params) {
        // TODO need to be more generic in order to work with JPASupport
        if (JPABase.class.isAssignableFrom(clazz)) {
            String keyName = Model.Manager.factoryFor(clazz).keyName();
            String idKey = name + "." + keyName;
            if (params.containsKey(idKey) && params.get(idKey).length > 0 && params.get(idKey)[0] != null && params.get(idKey)[0].trim().length() > 0) {
                String id = params.get(idKey)[0];
                try {
                    Query query = JPA.em().createQuery("from " + clazz.getName() + " o where o." + keyName + " = ?");
                    query.setParameter(1, play.data.binding.Binder.directBind(name, annotations, id + "", Model.Manager.factoryFor(clazz).keyType()));
                    Object o = query.getSingleResult();
                    return GenericModel.edit(o, name, params, annotations);
                } catch (NoResultException e) {
                    // ok
                } catch (Exception e) {
                    throw new UnexpectedException(e);
                }
            }
            return GenericModel.create(clazz, name, params, annotations);
        }
        return super.bind(name, clazz, type, annotations, params);
    }

    @Override
    public Object bind(String name, Object o, Map<String, String[]> params) {
        if (o instanceof JPABase) {
            return GenericModel.edit(o, name, params, null);
        }
        return null;
    }

    @Override
    public void enhance(ApplicationClass applicationClass) throws Exception {
        new JPAEnhancer().enhanceThisClass(applicationClass);
    }

    @Override
    public void onApplicationStart() {
        if (JPA.entityManagerFactory == null) {
            List<Class> classes = Play.classloader.getAnnotatedClasses(Entity.class);
            if (classes.isEmpty() && Play.configuration.getProperty("jpa.entities", "").equals("")) {
                return;
            }

            final String dataSource = Play.configuration.getProperty("hibernate.connection.datasource");
            if (StringUtils.isEmpty(dataSource) && DB.datasource == null) {
                throw new JPAException("Cannot start a JPA manager without a properly configured database", new NullPointerException("No datasource configured"));
            }

            Ejb3Configuration cfg = new Ejb3Configuration();

            if (DB.datasource != null) {
                cfg.setDataSource(DB.datasource);
            }

            if (!Play.configuration.getProperty("jpa.ddl", Play.mode.isDev() ? "update" : "none").equals("none")) {
                cfg.setProperty("hibernate.hbm2ddl.auto", Play.configuration.getProperty("jpa.ddl", "update"));
            }

            cfg.setProperty("hibernate.dialect", getDefaultDialect(Play.configuration.getProperty("db.driver")));
            cfg.setProperty("javax.persistence.transaction", "RESOURCE_LOCAL");

            // Explicit SAVE for JPABase is implemented here
            // ~~~~~~
            // We've hacked the org.hibernate.event.def.AbstractFlushingEventListener line 271, to flush collection update,remove,recreation
            // only if the owner will be saved.
            // As is:
            // if (session.getInterceptor().onCollectionUpdate(coll, ce.getLoadedKey())) {
            //      actionQueue.addAction(...);
            // }
            //
            // This is really hacky. We should move to something better than Hibernate like EBEAN
            cfg.setInterceptor(new EmptyInterceptor() {

                @Override
                public int[] findDirty(Object o, Serializable id, Object[] arg2, Object[] arg3, String[] arg4, Type[] arg5) {
                    if (o instanceof JPABase && !((JPABase) o).willBeSaved) {
                        return new int[0];
                    }
                    return null;
                }

                @Override
                public boolean onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
                    if (collection instanceof PersistentCollection) {
                        Object o = ((PersistentCollection) collection).getOwner();
                        if (o instanceof JPABase) {
                            return ((JPABase) o).willBeSaved;
                        }
                    } else {
                        System.out.println("HOO: Case not handled !!!");
                    }
                    return super.onCollectionUpdate(collection, key);
                }

                @Override
                public boolean onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
                    if (collection instanceof PersistentCollection) {
                        Object o = ((PersistentCollection) collection).getOwner();
                        if (o instanceof JPABase) {
                            return ((JPABase) o).willBeSaved;
                        }
                    } else {
                        System.out.println("HOO: Case not handled !!!");
                    }
                    return super.onCollectionRecreate(collection, key);
                }

                @Override
                public boolean onCollectionRemove(Object collection, Serializable key) throws CallbackException {
                    if (collection instanceof PersistentCollection) {
                        Object o = ((PersistentCollection) collection).getOwner();
                        if (o instanceof JPABase) {
                            return ((JPABase) o).willBeSaved;
                        }
                    } else {
                        System.out.println("HOO: Case not handled !!!");
                    }
                    return super.onCollectionRemove(collection, key);
                }
            });
            if (Play.configuration.getProperty("jpa.debugSQL", "false").equals("true")) {
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
            for (Class<?> clazz : classes) {
                if (clazz.isAnnotationPresent(Entity.class)) {
                    cfg.addAnnotatedClass(clazz);
                    Logger.trace("JPA Model : %s", clazz);
                }
            }
            String[] moreEntities = Play.configuration.getProperty("jpa.entities", "").split(", ");
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
            String mappingFile = Play.configuration.getProperty("jpa.mapping-file", "");
            if (mappingFile != null && mappingFile.length() > 0) {
                cfg.addResource(mappingFile);
            }
            Logger.trace("Initializing JPA ...");
            try {
                JPA.entityManagerFactory = cfg.buildEntityManagerFactory();
            } catch (PersistenceException e) {
                throw new JPAException(e.getMessage(), e.getCause() != null ? e.getCause() : e);
            }
            JPQL.instance = new JPQL();
        }
    }

    static String getDefaultDialect(String driver) {
        String dialect = Play.configuration.getProperty("jpa.dialect");
        if (dialect != null) {
            return dialect;
        } else if (driver.equals("org.hsqldb.jdbcDriver")) {
            return "org.hibernate.dialect.HSQLDialect";
        } else if (driver.equals("com.mysql.jdbc.Driver")) {
            return "play.db.jpa.MySQLDialect";
        } else if (driver.equals("org.postgresql.Driver")) {
            return "org.hibernate.dialect.PostgreSQLDialect";
        } else if (driver.toLowerCase().equals("com.ibm.db2.jdbc.app.DB2Driver")) {
            return "org.hibernate.dialect.DB2Dialect";
        } else if (driver.equals("com.ibm.as400.access.AS400JDBCDriver")) {
            return "org.hibernate.dialect.DB2400Dialect";
        } else if (driver.equals("com.ibm.as400.access.AS390JDBCDriver")) {
            return "org.hibernate.dialect.DB2390Dialect";
        } else if (driver.equals("oracle.jdbc.driver.OracleDriver")) {
            return "org.hibernate.dialect.Oracle9iDialect";
        } else if (driver.equals("com.sybase.jdbc2.jdbc.SybDriver")) {
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
        //if(Play.configuration.getProperty("future.bindJPAObjects", "false").equals("true")) {
        manager.setFlushMode(FlushModeType.COMMIT);
        //}
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

    @Override
    public Model.Factory modelFactory(Class<? extends Model> modelClass) {
        if (modelClass.isAnnotationPresent(Entity.class)) {
            return new JPAModelLoader(modelClass);
        }
        return null;
    }

    @Override
    public void afterFixtureLoad() {
        if (JPA.isEnabled()) {
            JPA.em().clear();
        }
    }

}
