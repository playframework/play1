package play.db.jpa;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Level;
import org.hibernate.ejb.Ejb3Configuration;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.data.binding.Binder;
import play.data.binding.NoBinding;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;
import play.db.DB;
import play.db.Model;
import play.db.Configuration;
import play.exceptions.JPAException;
import play.exceptions.UnexpectedException;

import javax.persistence.*;

import java.beans.PropertyDescriptor;
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
        if (JPABase.class.isAssignableFrom(clazz)) {

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
                    EntityManager em = JPA.em();
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


    public void onConfigurationRead() {
        Properties configuration = Play.configuration;
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
        org.hibernate.ejb.HibernatePersistence persistence = new org.hibernate.ejb.HibernatePersistence();

        Set<String> dBNames = Configuration.getDbNames();
        for (String dbName : dBNames) {
            Configuration dbConfig = new Configuration(dbName);
            
            Ejb3Configuration cfg = new Ejb3Configuration();
            List<Class> classes = Play.classloader.getAnnotatedClasses(Entity.class);
            for (Class<?> clazz : classes) {
                if (clazz.isAnnotationPresent(Entity.class)) {
                    // Do we have a transactional annotation matching our dbname?
                    PersistenceUnit pu = clazz.getAnnotation(PersistenceUnit.class);
                    if (pu != null && pu.name().equals(dbName)) {
                      cfg.addAnnotatedClass(clazz);
                      Logger.debug("Add JPA Model : %s to db %s", clazz, dbName);
                    } else if (pu == null && JPA.DEFAULT.equals(dbName)) {
                      cfg.addAnnotatedClass(clazz);
                      Logger.debug("Add JPA Model : %s to db %s", clazz, dbName);
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
                      cfg.addAnnotatedClass(clazz);
                      Logger.debug("Add JPA Model : %s to db %s", clazz, dbName);
                    } else if (pu == null && JPA.DEFAULT.equals(dbName)) {
                      cfg.addAnnotatedClass(clazz);
                      Logger.debug("Add JPA Model : %s to db %s", clazz, dbName);
                    }         
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

            String mappingFile = dbConfig.getProperty("jpa.mapping-file", "");
            if (mappingFile != null && mappingFile.length() > 0) {
                cfg.addResource(mappingFile);
            }

            if (!dbConfig.getProperty("jpa.ddl", Play.mode.isDev() ? "update" : "none").equals("none")) {
                cfg.setProperty("hibernate.hbm2ddl.auto", dbConfig.getProperty("jpa.ddl", "update"));
            }
          
            Map<String, String> properties = dbConfig.getProperties();
            properties.put("javax.persistence.transaction", "RESOURCE_LOCAL");
            properties.put("javax.persistence.provider", "org.hibernate.ejb.HibernatePersistence");
            properties.put("hibernate.dialect", getDefaultDialect(dbConfig, dbConfig.getProperty("db.driver")));
            
             if (dbConfig.getProperty("jpa.debugSQL", "false").equals("true")) {
                org.apache.log4j.Logger.getLogger("org.hibernate.SQL").setLevel(Level.ALL);
            } else {
                org.apache.log4j.Logger.getLogger("org.hibernate.SQL").setLevel(Level.OFF);
            }

            cfg.configure(properties);
            cfg.setDataSource(DB.getDataSource(dbName));
          
            try {
                Field field = cfg.getClass().getDeclaredField("overridenClassLoader");
                field.setAccessible(true);
                field.set(cfg, Play.classloader);
            } catch (Exception e) {
                Logger.error(e, "Error trying to override the hibernate classLoader (new hibernate version ???)");
            }
            
            cfg.setInterceptor(new HibernateInterceptor());

            if (Logger.isTraceEnabled()) {
                Logger.trace("Initializing JPA for %s...", dbName);
            }

            JPA.emfs.put(dbName, cfg.buildEntityManagerFactory());
        }
        JPQL.instance = new JPQL();
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
        // Close all presistence units
        for(EntityManagerFactory emf: JPA.emfs.values()) {
            if(emf.isOpen()){
                emf.close();
            }
        }
        JPA.emfs.clear();    
    }
  
    @Override
    public void afterFixtureLoad() {
        if (JPA.isEnabled()) {
            JPA.em().clear();
        }
    } 
   
    @Override
    public void afterInvocation() {
       // In case the current Action got suspended
       closeTx(false);
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
    public Filter getFilter() {
      return txFilter;
    }

    public static EntityManager createEntityManager() {
      return JPA.createEntityManager(JPA.DEFAULT);
    }


    /**
     * initialize the JPA context and starts a JPA transaction
     *
     * @param readonly true for a readonly transaction
     * @param autoCommit true to automatically commit the DB transaction after each JPA statement
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
        JPA.createContext(manager, readonly);
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
                    DB.getConnection().setAutoCommit(false);
                } catch(Exception e) {
                    Logger.error(e, "Why the driver complains here?");
                }
                // Commit the transaction
                if (manager.getTransaction().isActive()) {
                    if (JPA.get().get("default").readonly || rollback || manager.getTransaction().getRollbackOnly()) {
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

    public static class JPAModelLoader implements Model.Factory {

        private Class<? extends Model> clazz;
        private Map<String, Model.Property> properties;


        public JPAModelLoader(Class<? extends Model> clazz) {
            this.clazz = clazz;
        }

        public Model findById(Object id) {
            try {
                if (id == null) {
                    return null;
                }
                return JPA.em().find(clazz, id);
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
            Query query = JPA.em().createQuery(q);
            if (keywords != null && !keywords.equals("") && q.indexOf("?1") != -1) {
                query.setParameter(1, "%" + keywords.toLowerCase() + "%");
            }
            query.setFirstResult(offset);
            query.setMaxResults(size);
            return query.getResultList();
        }

        public Long count(List<String> searchFields, String keywords, String where) {
            String q = "select count(*) from " + clazz.getName() + " e";
            if (keywords != null && !keywords.equals("")) {
                String searchQuery = getSearchQuery(searchFields);
                if (!searchQuery.equals("")) {
                    q += " where (" + searchQuery + ")";
                }
                q += (where != null ? " and " + where : "");
            } else {
                q += (where != null ? " where " + where : "");
            }
            Query query = JPA.em().createQuery(q);
            if (keywords != null && !keywords.equals("") && q.indexOf("?1") != -1) {
                query.setParameter(1, "%" + keywords.toLowerCase() + "%");
            }
            return Long.decode(query.getSingleResult().toString());
        }

        public void deleteAll() {
            JPA.em().createQuery("delete from " + clazz.getName()).executeUpdate();
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
                if (f.isAnnotationPresent(NoBinding.class)) {
                    NoBinding a = f.getAnnotation(NoBinding.class);
                    List<String> values = Arrays.asList(a.value());
                    if (values.contains("*")) {
                        continue;
                    }
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

        public Class<?>[] keyTypes() {
            Field[] fields = keyFields();
            Class<?>[] types = new Class<?>[fields.length];
            int i = 0;
            for (Field field : fields) {
                types[i++] = field.getType();
            }
            return types;
        }

        public String[] keyNames() {
            Field[] fields = keyFields();
            String[] names = new String[fields.length];
            int i = 0;
            for (Field field : fields) {
                names[i++] = field.getName();
            }
            return names;
        }

        private Class<?> getCompositeKeyClass() {
            Class<?> tclazz = clazz;
            while (!tclazz.equals(Object.class)) {
                // Only consider mapped types
                if (tclazz.isAnnotationPresent(Entity.class)
                        || tclazz.isAnnotationPresent(MappedSuperclass.class)) {
                    IdClass idClass = tclazz.getAnnotation(IdClass.class);
                    if (idClass != null)
                        return idClass.value();
                }
                tclazz = tclazz.getSuperclass();
            }
            throw new UnexpectedException("Invalid mapping for class " + clazz + ": multiple IDs with no @IdClass annotation");
        }


        private void initProperties() {
            synchronized(this){
                if(properties != null)
                    return;
                properties = new HashMap<String,Model.Property>();
                Set<Field> fields = getModelFields(clazz);
                for (Field f : fields) {
                    if (Modifier.isTransient(f.getModifiers())) {
                        continue;
                    }
                    if (f.isAnnotationPresent(Transient.class)) {
                        continue;
                    }
                    Model.Property mp = buildProperty(f);
                    if (mp != null) {
                        properties.put(mp.name, mp);
                    }
                }
            }
        }

        private Object makeCompositeKey(Model model) throws Exception {
            initProperties();
            Class<?> idClass = getCompositeKeyClass();
            Object id = idClass.newInstance();
            PropertyDescriptor[] idProperties = PropertyUtils.getPropertyDescriptors(idClass);
            if(idProperties == null || idProperties.length == 0)
                throw new UnexpectedException("Composite id has no properties: "+idClass.getName());
            for (PropertyDescriptor idProperty : idProperties) {
                // do we have a field for this?
                String idPropertyName = idProperty.getName();
                // skip the "class" property...
                if(idPropertyName.equals("class"))
                    continue;
                Model.Property modelProperty = this.properties.get(idPropertyName);
                if(modelProperty == null)
                    throw new UnexpectedException("Composite id property missing: "+clazz.getName()+"."+idPropertyName
                            +" (defined in IdClass "+idClass.getName()+")");
                // sanity check
                Object value = modelProperty.field.get(model);

                if(modelProperty.isMultiple)
                    throw new UnexpectedException("Composite id property cannot be multiple: "+clazz.getName()+"."+idPropertyName);
                // now is this property a relation? if yes then we must use its ID in the key (as per specs)
                    if(modelProperty.isRelation){
                    // get its id
                    if(!Model.class.isAssignableFrom(modelProperty.type))
                        throw new UnexpectedException("Composite id property entity has to be a subclass of Model: "
                                +clazz.getName()+"."+idPropertyName);
                    // we already checked that cast above
                    @SuppressWarnings("unchecked")
                    Model.Factory factory = Model.Manager.factoryFor((Class<? extends Model>) modelProperty.type);
                    if(factory == null)
                        throw new UnexpectedException("Failed to find factory for Composite id property entity: "
                                +clazz.getName()+"."+idPropertyName);
                    // we already checked that cast above
                    if(value != null)
                        value = factory.keyValue((Model) value);
                }
                // now affect the composite id with this id
                PropertyUtils.setSimpleProperty(id, idPropertyName, value);
            }
            return id;
        }



        public Object keyValue(Model m) {
            try {
                if (m == null) {
                    return null;
                }

                // Do we have a @IdClass or @Embeddable?
                if (m.getClass().isAnnotationPresent(IdClass.class)) {
                    return makeCompositeKey(m);
                }

                // Is it a composite key? If yes we need to return the matching PK
                final Field[] fields = keyFields();
                final Object[] values = new Object[fields.length];
                int i = 0;
                for (Field f : fields) {
                    final Object o = f.get(m);
                    if (o != null) {
                        values[i++] = o;
                    }
                }

                // If we have only one id return it
                if (values.length == 1) {
                    return values[0];
                }

                return values;
            } catch (Exception ex) {
                throw new UnexpectedException(ex);
            }
        }

        public static Set<Field> getModelFields(Class<?> clazz){
            Set<Field> fields = new LinkedHashSet<Field>();
            Class<?> tclazz = clazz;
            while (!tclazz.equals(Object.class)) {
                // Only add fields for mapped types
                if(tclazz.isAnnotationPresent(Entity.class)
                        || tclazz.isAnnotationPresent(MappedSuperclass.class))
                    Collections.addAll(fields, tclazz.getDeclaredFields());
                tclazz = tclazz.getSuperclass();
            }
            return fields;
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

        Field[] keyFields() {
            Class c = clazz;
            try {
                List<Field> fields = new ArrayList<Field>();
                while (!c.equals(Object.class)) {
                    for (Field field : c.getDeclaredFields()) {
                        if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                            field.setAccessible(true);
                            fields.add(field);
                        }
                    }
                    c = c.getSuperclass();
                }
                final Field[] f = fields.toArray(new Field[fields.size()]);
                if (f.length == 0) {
                    throw new UnexpectedException("Cannot get the object @Id for an object of type " + clazz);
                }
                return f;
            } catch (Exception e) {
                throw new UnexpectedException("Error while determining the object @Id for an object of type " + clazz);
            }
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
                                return JPA.em().createQuery("from " + field.getType().getName()).getResultList();
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
                            return JPA.em().createQuery("from " + field.getType().getName()).getResultList();
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
                                return JPA.em().createQuery("from " + fieldType.getName()).getResultList();
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
                                return JPA.em().createQuery("from " + fieldType.getName()).getResultList();
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
            if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                // Look if the target is an embeddable class
                if (field.getType().isAnnotationPresent(Embeddable.class) || field.getType().isAnnotationPresent(IdClass.class) ) {
                    modelProperty.isRelation = true;
                    modelProperty.relationType =  field.getType();
                }
            }
            return modelProperty;
        }
    }
}
