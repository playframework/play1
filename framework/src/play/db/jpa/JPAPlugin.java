package play.db.jpa;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.type.Type;
import play.Invoker.InvocationContext;
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
import play.exceptions.JPAException;
import play.exceptions.UnexpectedException;
import play.utils.Utils;

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
            // only if the owner will be saved or if the targeted entity will be saved (avoid the org.hibernate.HibernateException: Found two representations of same collection)
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
							if (entities.get() != null) {
	                           	return ((JPABase) o).willBeSaved || ((JPABase) entities.get()).willBeSaved;
							} else {
								return ((JPABase) o).willBeSaved;
							}
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
							if (entities.get() != null) {
	                           	return ((JPABase) o).willBeSaved || ((JPABase) entities.get()).willBeSaved;
							} else {
								return ((JPABase) o).willBeSaved;
							}
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
							if (entities.get() != null) {
                            	return ((JPABase) o).willBeSaved || ((JPABase) entities.get()).willBeSaved;
							} else {
								return ((JPABase) o).willBeSaved;
							}
                        }
                    } else {
                        System.out.println("HOO: Case not handled !!!");
                    }
                    return super.onCollectionRemove(collection, key);
                }

				protected ThreadLocal<Object> entities = new ThreadLocal<Object>();
				
				@Override
			 	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)  {
					entities.set(entity);
					return super.onSave(entity, id, state, propertyNames, types);
				}
						
				@Override
				public void afterTransactionCompletion(org.hibernate.Transaction tx) {
					entities.remove();
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
                    if (Logger.isTraceEnabled()) {
                        Logger.trace("JPA Model : %s", clazz);
                    }
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
            if (Logger.isTraceEnabled()) {
                Logger.trace("Initializing JPA ...");
            }
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
        } else if (driver.equals("oracle.jdbc.OracleDriver")) {
            return "org.hibernate.dialect.Oracle10gDialect";
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

        if(InvocationContext.current().getAnnotation(NoTransaction.class) != null ) {
            //Called method or class is annotated with @NoTransaction telling us that
            //we should not start a transaction
            return ;
        }

        boolean readOnly = false;
        Transactional tx = InvocationContext.current().getAnnotation(Transactional.class);
        if (tx != null) {
            readOnly = tx.readOnly();
        }
        startTx(readOnly);
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
     * @param autoCommit true to automatically commit the DB transaction after each JPA statement
     */
    public static void startTx(boolean readonly) {
        if (!JPA.isEnabled()) {
            return;
        }
        EntityManager manager = JPA.entityManagerFactory.createEntityManager();
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
     */
    public static void closeTx(boolean rollback) {
        if (!JPA.isEnabled() || JPA.local.get() == null) {
            return;
        }
        EntityManager manager = JPA.get().entityManager;
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
