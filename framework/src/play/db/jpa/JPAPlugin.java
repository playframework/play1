package play.db.jpa;

import org.apache.log4j.Level;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;
import play.db.Configuration;
import play.db.DB;
import play.db.Model;
import play.exceptions.JPAException;
import play.exceptions.UnexpectedException;

import javax.persistence.*;
import javax.persistence.spi.PersistenceUnitInfo;

import java.lang.annotation.Annotation;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;


public class JPAPlugin extends PlayPlugin {
    public static boolean autoTxs = true;
  
    @Override
    public Object bind(RootParamNode rootParamNode, String name, Class clazz, java.lang.reflect.Type type, Annotation[] annotations) {
        // TODO need to be more generic in order to work with JPASupport
        if (JPABase.class.isAssignableFrom(clazz)) {

            ParamNode paramNode = rootParamNode.getChild(name, true);

            String[] keyNames = new JPAModelLoader(clazz).keyNames();
            ParamNode[] ids = new ParamNode[keyNames.length];
            
            String dbName = JPA.getDBName(clazz);
            // Collect the matching ids
            int i = 0;
            for (String keyName : keyNames) {
                ids[i++] = paramNode.getChild(keyName, true);
            }
            if (ids != null && ids.length > 0) {
                try {
                    EntityManager em = JPA.em(dbName);
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
                    Class<?>[] pk = new JPAModelLoader(clazz).keyTypes();
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
     
    public EntityManager em(String key) {
        EntityManagerFactory emf = JPA.emfs.get(key);
        if(emf == null) {
            return null;
        }
        return emf.createEntityManager();
    }

    /**
     * Reads the configuration file and initialises required JPA EntityManagerFactories.
     */
    @Override
    public void onApplicationStart() {  
        org.apache.log4j.Logger.getLogger("org.hibernate.SQL").setLevel(Level.OFF);

        Set<String> dBNames = Configuration.getDbNames();
        for (String dbName : dBNames) {
            Configuration dbConfig = new Configuration(dbName);
            
            if (dbConfig.getProperty("jpa.debugSQL", "false").equals("true")) {
                org.apache.log4j.Logger.getLogger("org.hibernate.SQL").setLevel(Level.ALL);
            }

            Thread thread = Thread.currentThread();
            ClassLoader contextClassLoader = thread.getContextClassLoader();
            thread.setContextClassLoader(Play.classloader);
            try {

                if (Logger.isTraceEnabled()) {
                    Logger.trace("Initializing JPA for %s...", dbName);
                }
                
                JPA.emfs.put(dbName, newEntityManagerFactory(dbName, dbConfig));
            } finally {
                thread.setContextClassLoader(contextClassLoader);
            }
        }
        JPQL.instance = new JPQL();
    }
    
    private List<Class> entityClasses(String dbName) {
        List<Class> entityClasses = new ArrayList<>();
        
        List<Class> classes = Play.classloader.getAnnotatedClasses(Entity.class);
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Entity.class)) {
                // Do we have a transactional annotation matching our dbname?
                PersistenceUnit pu = clazz.getAnnotation(PersistenceUnit.class);
                if (pu != null && pu.name().equals(dbName)) {
                    entityClasses.add(clazz);
                } else if (pu == null && JPA.DEFAULT.equals(dbName)) {
                    entityClasses.add(clazz);
                }                    
            }
        }

        // Add entities
        String[] moreEntities = Play.configuration.getProperty("jpa.entities", "").split(", ");
        for (String entity : moreEntities) {
            if (entity.trim().equals("")) {
                continue;
            }
            try {
                Class<?> clazz = Play.classloader.loadClass(entity);  
                // Do we have a transactional annotation matching our dbname?
                PersistenceUnit pu = clazz.getAnnotation(PersistenceUnit.class);
                if (pu != null && pu.name().equals(dbName)) {
                    entityClasses.add(clazz);
                } else if (pu == null && JPA.DEFAULT.equals(dbName)) {
                    entityClasses.add(clazz);
                }         
            } catch (Exception e) {
                Logger.warn(e, "JPA -> Entity not found: %s", entity);
            }
        }
        return entityClasses;
    }

    protected EntityManagerFactory newEntityManagerFactory(String dbName, Configuration dbConfig) {
        PersistenceUnitInfo persistenceUnitInfo = persistenceUnitInfo(dbName, dbConfig);
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(AvailableSettings.INTERCEPTOR, new HibernateInterceptor());

        return new EntityManagerFactoryBuilderImpl(
                new PersistenceUnitInfoDescriptor(persistenceUnitInfo), configuration
        ).build();
    }

    protected PersistenceUnitInfoImpl persistenceUnitInfo(String dbName, Configuration dbConfig) {
        final List<Class> managedClasses = entityClasses(dbName);
        final Properties properties = properties(dbName, dbConfig);
        properties.put(org.hibernate.jpa.AvailableSettings.LOADED_CLASSES,managedClasses);
        return new PersistenceUnitInfoImpl(dbName,
                managedClasses, mappingFiles(dbConfig), properties);
    }

    private List<String> mappingFiles(Configuration dbConfig) {
        String mappingFile = dbConfig.getProperty("jpa.mapping-file", "");
        return mappingFile != null && mappingFile.length() > 0 ? singletonList(mappingFile) : emptyList();

    }

    protected Properties properties(String dbName, Configuration dbConfig) {
        Properties properties = new Properties();
        properties.putAll(dbConfig.getProperties());
        properties.put("javax.persistence.transaction", "RESOURCE_LOCAL");
        properties.put("javax.persistence.provider", "org.hibernate.ejb.HibernatePersistence");
        properties.put("hibernate.dialect", getDefaultDialect(dbConfig, dbConfig.getProperty("db.driver")));

        if (!dbConfig.getProperty("jpa.ddl", Play.mode.isDev() ? "update" : "none").equals("none")) {
            properties.setProperty("hibernate.hbm2ddl.auto", dbConfig.getProperty("jpa.ddl", "update"));
        }

        properties.put("hibernate.connection.datasource", DB.getDataSource(dbName));
        return properties;
    }

    public static String getDefaultDialect(String driver) {
        return getDefaultDialect(new Configuration("default"), driver);
    }

    public static String getDefaultDialect(Configuration dbConfig, String driver) {
        String dialect = dbConfig.getProperty("jpa.dialect");
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
        closeAllPersistenceUnits();    
    }

    private void closeAllPersistenceUnits() {
        for (EntityManagerFactory emf : JPA.emfs.values()) {
            if (emf.isOpen()) {
                emf.close();
            }
        }
        JPA.emfs.clear();
    }

    @Override
    public void afterFixtureLoad() {
        if (JPA.isEnabled()) {
            for(String emfKey: JPA.emfs.keySet()) {
                JPA.em(emfKey).clear();
            }
        }
    } 
   
    @Override
    public void afterInvocation() {
       // In case the current Action got suspended
       for(String emfKey: JPA.emfs.keySet()) {
           JPA.closeTx(emfKey);
       }
    }

    public class TransactionalFilter extends Filter<Object> {
      public TransactionalFilter(String name) {
        super(name);
      }
      @Override
      public Object withinFilter(play.libs.F.Function0<Object> fct) throws Throwable {
        return JPA.withinFilter(fct);
      }
    }

    private TransactionalFilter txFilter = new TransactionalFilter("TransactionalFilter");

    @Override
    public Filter<Object> getFilter() {
      return txFilter;
    }

    public static EntityManager createEntityManager() {
      return JPA.createEntityManager(JPA.DEFAULT);
    }


    /**
     * initialize the JPA context and starts a JPA transaction
     *
     * @param readonly true for a readonly transaction
     * @deprecated see JPA startTx() method
     */
    @Deprecated
    public static void startTx(boolean readonly) {
        if (!JPA.isEnabled()) {
             return;
        }
        EntityManager manager = JPA.createEntityManager();
        manager.setFlushMode(FlushModeType.COMMIT);
        manager.setProperty("org.hibernate.readOnly", readonly);
        if (autoTxs) {
            manager.getTransaction().begin();
        }
        JPA.createContext(JPA.DEFAULT, manager, readonly);
    }

   
    /**
     * clear current JPA context and transaction 
     * @param rollback shall current transaction be committed (false) or cancelled (true)
     * @deprecated see {@link JPA#rollbackTx} and {@link JPA#closeTx} method
     */
    @Deprecated
    public static void closeTx(boolean rollback) {
        if (!JPA.isEnabled() || JPA.currentEntityManager.get() == null || JPA.currentEntityManager.get().get(JPA.DEFAULT) == null || JPA.currentEntityManager.get().get(JPA.DEFAULT).entityManager == null) {
            return;
        }
        EntityManager manager = JPA.currentEntityManager.get().get(JPA.DEFAULT).entityManager;
        try {
            if (autoTxs) {
                // Be sure to set the connection is non-autoCommit mode as some driver will complain about COMMIT statement
                try {
                    DB.getConnection(JPA.DEFAULT).setAutoCommit(false);
                } catch(Exception e) {
                    Logger.error(e, "Why the driver complains here?");
                }
                // Commit the transaction
                if (manager.getTransaction().isActive()) {
                    if (JPA.get(JPA.DEFAULT).readonly || rollback || manager.getTransaction().getRollbackOnly()) {
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
            JPA.clearContext(JPA.DEFAULT);
        }
    }

    @Override
    public Model.Factory modelFactory(Class<? extends Model> modelClass) {
        if (modelClass.isAnnotationPresent(Entity.class)) {
            return new JPAModelLoader(modelClass);
        }
        return null;
    }  
}