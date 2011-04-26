package play.db.jpa;

import org.apache.commons.lang.StringUtils;
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
import play.data.binding.Binder;
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
                    EntityManager em = JPABase.getJPAConfig(clazz).getJPAContext().em();
                    Query query = em.createQuery("from " + clazz.getName() + " o where o." + keyName + " = ?");
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


    /**
     * returns empty string if default config.
     * returns descriptive string about config name if not default config
     */
    protected String getConfigInfoString(String configName) {
        if (DBConfig.defaultDbConfigName.equals(configName)) {
            return "";
        } else {
            return " (jpa config name: "+configName+")";
        }
    }


    @Override
    public void onApplicationStart() {

        // must check and configure JPA for each DBConfig
        for (DBConfig dbConfig : DB.getDBConfigs()) {
            // check and enable JPA on this config

            // is JPA already configured?
            String configName = dbConfig.getDBConfigName();

            if (JPA.getJPAConfig(configName, true)==null) {
                //must configure it

                // resolve prefix for hibernate config..
                // should be nothing for default, and db_<name> for others
                String propPrefix = "";
                if (!DBConfig.defaultDbConfigName.equalsIgnoreCase(configName)) {
                   propPrefix = "db_"+configName+".";
                }
                List<Class> classes = findEntityClassesForThisConfig(configName, propPrefix);
                if (classes == null) continue;


                // we're ready to configure this instance of JPA
                final String hibernateDataSource = Play.configuration.getProperty(propPrefix+"hibernate.connection.datasource");

                if (StringUtils.isEmpty(hibernateDataSource) && dbConfig == null) {
                    throw new JPAException("Cannot start a JPA manager without a properly configured database"+getConfigInfoString(configName),
                            new NullPointerException("No datasource configured"));
                }

                Ejb3Configuration cfg = new Ejb3Configuration();

                if (dbConfig.getDatasource() != null) {
                    cfg.setDataSource(dbConfig.getDatasource());
                }

                if (!Play.configuration.getProperty(propPrefix+"jpa.ddl", Play.mode.isDev() ? "update" : "none").equals("none")) {
                    cfg.setProperty("hibernate.hbm2ddl.auto", Play.configuration.getProperty(propPrefix+"jpa.ddl", "update"));
                }

                String driver = null;
                if (StringUtils.isEmpty(propPrefix)) {
                    driver = Play.configuration.getProperty("db.driver");
                } else {
                    driver = Play.configuration.getProperty(propPrefix+"driver");
                }
                cfg.setProperty("hibernate.dialect", getDefaultDialect(propPrefix, driver));
                cfg.setProperty("javax.persistence.transaction", "RESOURCE_LOCAL");


                cfg.setInterceptor(new PlayInterceptor());

                // This setting is global for all JPAs - only configure if configuring default JPA
                if (StringUtils.isEmpty(propPrefix)) {
                    if (Play.configuration.getProperty(propPrefix+"jpa.debugSQL", "false").equals("true")) {
                        org.apache.log4j.Logger.getLogger("org.hibernate.SQL").setLevel(Level.ALL);
                    } else {
                        org.apache.log4j.Logger.getLogger("org.hibernate.SQL").setLevel(Level.OFF);
                    }
                }
                // inject additional  hibernate.* settings declared in Play! configuration
                Properties additionalProperties = (Properties)Utils.Maps.filterMap(Play.configuration, "^"+propPrefix+"hibernate\\..*");
                // We must remove prefix from names
                Properties transformedAdditionalProperties = new Properties();
                for (Map.Entry<Object, Object> entry : additionalProperties.entrySet()) {
                    Object key = entry.getKey();
                    if (!StringUtils.isEmpty(propPrefix)) {
                        key = ((String)key).substring(propPrefix.length()); // chop off the prefix
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
                    Logger.trace("JPA Model : %s", clazz);
                }
                String[] moreEntities = Play.configuration.getProperty(propPrefix+"jpa.entities", "").split(", ");
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

                String mappingFile = Play.configuration.getProperty(propPrefix+"jpa.mapping-file", "");
                if (mappingFile != null && mappingFile.length() > 0) {
                    cfg.addResource(mappingFile);
                }
                Logger.trace("Initializing JPA"+getConfigInfoString(configName)+" ...");
                try {
                    JPA.addConfiguration(configName, cfg);
                } catch (PersistenceException e) {
                    throw new JPAException(e.getMessage()+getConfigInfoString(configName), e.getCause() != null ? e.getCause() : e);
                }

            }

        }

        // must look for Entity-objects referring to none-existing JPAConfig
        List<Class> allEntityClasses = Play.classloader.getAnnotatedClasses(Entity.class);
        for (Class clazz : allEntityClasses) {
            String configName = Entity2JPAConfigResolver.getJPAConfigNameForEntityClass(clazz);
            if (JPA.getJPAConfig(configName, true)==null) {
                throw new JPAException("Found Entity-class ("+clazz.getName()+") referring to none-existing JPAConfig ("+configName+")");
            }
        }
    }

    private List<Class> findEntityClassesForThisConfig(String configName, String propPrefix) {
        //look and see if we have any Entity-objects for this db config
        List<Class> classes = Play.classloader.getAnnotatedClasses(Entity.class);

        // filter list on Entities meant for us..
        List<Class> filteredClasses = new ArrayList<Class>(classes.size());
        for (Class clazz : classes) {
            if ( configName.equals(Entity2JPAConfigResolver.getJPAConfigNameForEntityClass(clazz))) {
                filteredClasses.add(clazz);
            }
        }


        if (!Play.configuration.getProperty(propPrefix+"jpa.entities", "").equals("")) {
            return filteredClasses;
        }

        if (filteredClasses.isEmpty()) {
            return null;
        }

        return filteredClasses;
    }



    static String getDefaultDialect(String propPrefix, String driver) {
        String dialect = Play.configuration.getProperty(propPrefix+"jpa.dialect");
        if (dialect != null) {
            return dialect;
        } else if (driver.equals("org.h2.Driver")) {
            return "org.hibernate.dialect.H2Dialect";
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
     *
     * This method is not needed since transaction is created
     * automatically on first use.
     *
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
     * @param rollback shall current transaction be committed (false) or cancelled (true)
     */
    protected static void closeTx(boolean rollback) {
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

    public static class JPAModelLoader implements Model.Factory {

        private final Class<? extends Model> clazz;
        private final String jpaConfigName;
        private JPAConfig _jpaConfig;

        public JPAModelLoader(Class<? extends Model> clazz) {
            this.clazz = clazz;

            // must detect correct JPAConfig for this model
            this.jpaConfigName = Entity2JPAConfigResolver.getJPAConfigNameForEntityClass(clazz);
        }

        protected JPAContext getJPAContext() {
            if (_jpaConfig==null) {
                _jpaConfig = JPA.getJPAConfig(jpaConfigName);
            }
            return _jpaConfig.getJPAContext();
        }

        public Model findById(Object id) {
            if (id == null) {
                return null;
            }
            try {
                return getJPAContext().em().find(clazz, Binder.directBind(id.toString(), Model.Manager.factoryFor(clazz).keyType()));
            } catch (Exception e) {
                // Key is invalid, thus nothing was found
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        public List<Model> fetch(int offset, int size, String orderBy, String order, List<String> searchFields, String keywords, String where) {
            String q = "from " + clazz.getName();
            if (keywords != null && !keywords.equals("")) {
                String searchQuery = getSearchQuery(searchFields);
                if (!searchQuery.equals("")) {
                    q += " where (" + searchQuery + ")";
                }
                q += (where != null ? " and " + where : "");
            } else {
                q += (where != null ? " where " + where : "");
            }
            if (orderBy == null && order == null) {
                orderBy = "id";
                order = "ASC";
            }
            if (orderBy == null && order != null) {
                orderBy = "id";
            }
            if (order == null || (!order.equals("ASC") && !order.equals("DESC"))) {
                order = "ASC";
            }
            q += " order by " + orderBy + " " + order;
            Query query = getJPAContext().em().createQuery(q);
            if (keywords != null && !keywords.equals("") && q.indexOf("?1") != -1) {
                query.setParameter(1, "%" + keywords.toLowerCase() + "%");
            }
            query.setFirstResult(offset);
            query.setMaxResults(size);
            return query.getResultList();
        }

        public Long count(List<String> searchFields, String keywords, String where) {
            String q = "select count(e) from " + clazz.getName() + " e";
            if (keywords != null && !keywords.equals("")) {
                String searchQuery = getSearchQuery(searchFields);
                if (!searchQuery.equals("")) {
                    q += " where (" + searchQuery + ")";
                }
                q += (where != null ? " and " + where : "");
            } else {
                q += (where != null ? " where " + where : "");
            }
            Query query = getJPAContext().em().createQuery(q);
            if (keywords != null && !keywords.equals("") && q.indexOf("?1") != -1) {
                query.setParameter(1, "%" + keywords.toLowerCase() + "%");
            }
            return Long.decode(query.getSingleResult().toString());
        }

        public void deleteAll() {
            getJPAContext().em().createQuery("delete from " + clazz.getName()).executeUpdate();
        }

        public List<Model.Property> listProperties() {
            List<Model.Property> properties = new ArrayList<Model.Property>();
            Set<Field> fields = new LinkedHashSet<Field>();
            Class<?> tclazz = clazz;
            while (!tclazz.equals(Object.class)) {
                Collections.addAll(fields, tclazz.getDeclaredFields());
                tclazz = tclazz.getSuperclass();
            }
            for (Field f : fields) {
                if (Modifier.isTransient(f.getModifiers())) {
                    continue;
                }
                if (f.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                Model.Property mp = buildProperty(f);
                if (mp != null) {
                    properties.add(mp);
                }
            }
            return properties;
        }

        public String keyName() {
            return keyField().getName();
        }

        public Class<?> keyType() {
            return keyField().getType();
        }

        public Object keyValue(Model m) {
            try {
                return keyField().get(m);
            } catch (Exception ex) {
                throw new UnexpectedException(ex);
            }
        }

        //
        Field keyField() {
            Class c = clazz;
            try {
                while (!c.equals(Object.class)) {
                    for (Field field : c.getDeclaredFields()) {
                        if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                            field.setAccessible(true);
                            return field;
                        }
                    }
                    c = c.getSuperclass();
                }
            } catch (Exception e) {
                throw new UnexpectedException("Error while determining the object @Id for an object of type " + clazz);
            }
            throw new UnexpectedException("Cannot get the object @Id for an object of type " + clazz);
        }

        String getSearchQuery(List<String> searchFields) {
            String q = "";
            for (Model.Property property : listProperties()) {
                if (property.isSearchable && (searchFields == null || searchFields.isEmpty() ? true : searchFields.contains(property.name))) {
                    if (!q.equals("")) {
                        q += " or ";
                    }
                    q += "lower(" + property.name + ") like ?1";
                }
            }
            return q;
        }

        Model.Property buildProperty(final Field field) {
            Model.Property modelProperty = new Model.Property();
            modelProperty.type = field.getType();
            modelProperty.field = field;
            if (Model.class.isAssignableFrom(field.getType())) {
                if (field.isAnnotationPresent(OneToOne.class)) {
                    if (field.getAnnotation(OneToOne.class).mappedBy().equals("")) {
                        modelProperty.isRelation = true;
                        modelProperty.relationType = field.getType();
                        modelProperty.choices = new Model.Choices() {

                            @SuppressWarnings("unchecked")
                            public List<Object> list() {
                                return getJPAContext().em().createQuery("from " + field.getType().getName()).getResultList();
                            }
                        };
                    }
                }
                if (field.isAnnotationPresent(ManyToOne.class)) {
                    modelProperty.isRelation = true;
                    modelProperty.relationType = field.getType();
                    modelProperty.choices = new Model.Choices() {

                        @SuppressWarnings("unchecked")
                        public List<Object> list() {
                            return getJPAContext().em().createQuery("from " + field.getType().getName()).getResultList();
                        }
                    };
                }
            }
            if (Collection.class.isAssignableFrom(field.getType())) {
                final Class<?> fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                if (field.isAnnotationPresent(OneToMany.class)) {
                    if (field.getAnnotation(OneToMany.class).mappedBy().equals("")) {
                        modelProperty.isRelation = true;
                        modelProperty.isMultiple = true;
                        modelProperty.relationType = fieldType;
                        modelProperty.choices = new Model.Choices() {

                            @SuppressWarnings("unchecked")
                            public List<Object> list() {
                                return getJPAContext().em().createQuery("from " + fieldType.getName()).getResultList();
                            }
                        };
                    }
                }
                if (field.isAnnotationPresent(ManyToMany.class)) {
                    if (field.getAnnotation(ManyToMany.class).mappedBy().equals("")) {
                        modelProperty.isRelation = true;
                        modelProperty.isMultiple = true;
                        modelProperty.relationType = fieldType;
                        modelProperty.choices = new Model.Choices() {

                            @SuppressWarnings("unchecked")
                            public List<Object> list() {
                                return getJPAContext().em().createQuery("from " + fieldType.getName()).getResultList();
                            }
                        };
                    }
                }
            }
            if (field.getType().isEnum()) {
                modelProperty.choices = new Model.Choices() {

                    @SuppressWarnings("unchecked")
                    public List<Object> list() {
                        return (List<Object>) Arrays.asList(field.getType().getEnumConstants());
                    }
                };
            }
            modelProperty.name = field.getName();
            if (field.getType().equals(String.class)) {
                modelProperty.isSearchable = true;
            }
            if (field.isAnnotationPresent(GeneratedValue.class)) {
                modelProperty.isGenerated = true;
            }
            return modelProperty;
        }
    }

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
    private static class PlayInterceptor extends EmptyInterceptor {

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
    }
}