package play.db.jpa;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Transient;

import org.apache.commons.beanutils.PropertyUtils;

import play.Logger;
import play.data.binding.Binder;
import play.db.Model;
import play.db.Model.Factory;
import play.db.Model.Property;
import play.exceptions.UnexpectedException;

public class JPAModelLoader implements Model.Factory {

    private Class<? extends Model> clazz;
    private Map<String, Model.Property> properties;
    private List<Model.Property> keyProperties;
    private static WeakHashMap<Class<?>, JPAModelLoader> cache = new WeakHashMap<Class<?>, JPAModelLoader>();

    public JPAModelLoader(Class<? extends Model> clazz) {
        this.clazz = clazz;
    }

    public static Factory instance(Class<? extends Model> modelClass) {
    	synchronized (cache) {
    		JPAModelLoader factory = cache.get(modelClass);
			if(factory == null){
				Logger.debug("Cache miss for %s factory", modelClass);
				factory = new JPAModelLoader(modelClass);
				cache.put(modelClass, factory);
			}else
				Logger.debug("Cache hit for %s factory", modelClass);
			return factory;
		}
	}

    public Model findById(Object id) {
        if (id == null) {
            return null;
        }
        try {
            return JPA.em().find(clazz, Binder.directBind(id.toString(), Model.Manager.factoryFor(clazz).keyType()));
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
    	initProperties();
    	return new ArrayList<Model.Property>(this.properties.values());
    }

    private void initProperties() {
    	synchronized(this){
    		if(properties != null)
    			return;
    		properties = new HashMap<String,Model.Property>();
    		keyProperties = new ArrayList<Model.Property>();
    		Set<Field> fields = JPAPlugin.getModelFields(clazz);
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
    				if(mp.isKey)
    					keyProperties.add(mp);
    			}
    		}
        }
    }

	@Deprecated
	@Override
	public String keyName() {
		List<Property> keys = listKeys();
		if(keys.size() > 1)
			throw new UnexpectedException("Property.keyName() does not work for composite keys. Use listKeys() instead.");
		if(keys.get(0).isRelation)
			throw new UnexpectedException("Property.keyName() does not work for relation keys. Use listKeys() instead.");
		return keys.get(0).name;
	}

	public Class<?> keyType() {
		List<Property> keys = listKeys();
		if(keys.size() == 1)
			return keys.get(0).type;
		// if we have more than one it's an idClass
		return getCompositeKeyClass();
    }

	private Class<?> getCompositeKeyClass(){
		Class<?> tclazz = clazz;
		while (!tclazz.equals(Object.class)) {
			// Only consider mapped types
			if(tclazz.isAnnotationPresent(Entity.class)
					|| tclazz.isAnnotationPresent(MappedSuperclass.class)){
				IdClass idClass = tclazz.getAnnotation(IdClass.class);
				if(idClass != null)
					return idClass.value();
			}
			tclazz = tclazz.getSuperclass();
		}
		throw new UnexpectedException("Invalid mapping for class " + clazz + ": multiple IDs with no @IdClass annotation");
	}

    public Object keyValue(Model m) {
		List<Property> keys = listKeys();
		try {
			// FIXME: this might have to check whether the ID is a relation, in which case we should use its ID
			// single ID
			if(keys.size() == 1)
				return keys.get(0).field.get(m);
			// if we have more than one it's an idClass
			// make one and bind it
			return makeCompositeKey(m);
        } catch (UnexpectedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
    }

    interface PropertyGetter {
    	Object getProperty(Property p) throws Exception;
    }

    private Object makeCompositeKey(final Model m) throws Exception {
    	return makeCompositeKey(new PropertyGetter(){
			@Override
			public Object getProperty(Property p) throws Exception {
				return p.field.get(m);
			}});
    }

    private Object makeCompositeKey(PropertyGetter propertyGetter) throws Exception {
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
			Property modelProperty = this.properties.get(idPropertyName);
			if(modelProperty == null)
				throw new UnexpectedException("Composite id propery missing: "+clazz.getName()+"."+idPropertyName
						+" (defined in IdClass "+idClass.getName()+")");
			// sanity check
			if(modelProperty.isMultiple)
				throw new UnexpectedException("Composite id property cannot be multiple: "+clazz.getName()+"."+idPropertyName);
			// now is this property a relation? if yes then we must use its ID in the key (as per specs)
			Object value = propertyGetter.getProperty(modelProperty);
			if(modelProperty.isRelation){
				// get its id
				if(!Model.class.isAssignableFrom(modelProperty.type))
					throw new UnexpectedException("Composite id property entity has to be a subclass of Model: "
							+clazz.getName()+"."+idPropertyName);
				// we already checked that cast above
				@SuppressWarnings("unchecked")
				Factory factory = Model.Manager.factoryFor((Class<? extends Model>) modelProperty.type);
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


	@Override
	public Object makeKey(Map<String, Object> ids) {
		List<Property> keys = listKeys();
		try {
			// FIXME: this might have to check whether the ID is a relation, in which case we should use its ID
			// single ID
			if(keys.size() == 1){
				Property key = keys.get(0);
				Object id = ids.get(key.name);
				if(id == null)
					throw new UnexpectedException("Missing key from mapping: "+key.name);
				return id;
			}
			// if we have more than one it's an idClass
			// make one and bind it
			return makeCompositeKey(ids);
        } catch (UnexpectedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
	}

    private Object makeCompositeKey(final Map<String, Object> ids) throws Exception {
    	return makeCompositeKey(new PropertyGetter(){
			@Override
			public Object getProperty(Property p) throws Exception {
				return ids.get(p.name);
			}});
    }

	//
    public List<Model.Property> listKeys() {
    	initProperties();
    	if(keyProperties.isEmpty())
    		throw new UnexpectedException("Cannot get the object @Id for an object of type " + clazz);
    	return keyProperties;
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
        if (field.isAnnotationPresent(Id.class)
        		|| field.isAnnotationPresent(EmbeddedId.class)) {
        	modelProperty.isKey = true;
        }
        return modelProperty;
    }
}
