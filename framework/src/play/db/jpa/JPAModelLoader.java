package play.db.jpa;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Embeddable;
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

import play.data.binding.NoBinding;
import play.db.Model;
import play.exceptions.UnexpectedException;

public class JPAModelLoader implements Model.Factory {

    private String dbName;
    private Class<? extends Model> clazz;
    private Map<String, Model.Property> properties;

    public JPAModelLoader(Class<? extends Model> clazz) {
        this.clazz = clazz;
        this.dbName = JPA.getDBName(clazz);
    }

    /**
     * Find object by ID
     * @param id : the id of the entity
     */
    @Override
    public Model findById(Object id) {
        try {
            if (id == null) {
                return null;
            }
            return JPA.em(this.dbName).find(this.clazz, id);
        } catch (Exception e) {
            // Key is invalid, thus nothing was found
            return null;
        }
    }

    /**
     * Retrieve a list of result
     * 
     * @param offset
     *            position of the first result, numbered from 0
     * @param size
     *            maximum number of results to retrieve
     * @param orderBy
     *            Order by field
     * @param order
     *            Sorting order
     * @param searchFields
     *            (page length)
     * @param keywords
     *            (page length)
     * @param where
     *            (page length)
     * @return a list of results
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Model> fetch(int offset, int size, String orderBy, String order, List<String> searchFields,
            String keywords, String where) {
        StringBuilder q = new StringBuilder("from ").append(this.clazz.getName());
        if (keywords != null && !keywords.isEmpty()) {
            String searchQuery = this.getSearchQuery(searchFields);
            if (!searchQuery.equals("")) {
                q.append(" where (").append(searchQuery).append(")");
            }
            q.append((where != null ? " and " + where : ""));
        } else {
            q.append((where != null ? " where " + where : ""));
        }
        if (orderBy == null && order == null) {
            orderBy = "id";
            order = "ASC";
        } else if (orderBy == null && order != null) {
            orderBy = "id";
        }
        if (order == null || (!order.equals("ASC") && !order.equals("DESC"))) {
            order = "ASC";
        }
        q.append( " order by ").append(orderBy).append(" ").append(order);
        String jpql = q.toString();
        Query query = JPA.em(this.dbName).createQuery(jpql);
        if (keywords != null && !keywords.equals("") && jpql.indexOf("?1") != -1) {
            query.setParameter(1, "%" + keywords.toLowerCase() + "%");
        }
        query.setFirstResult(offset);
        query.setMaxResults(size);
        return query.getResultList();
    }

    @Override
    public Long count(List<String> searchFields, String keywords, String where) {
        String q = "select count(*) from " + this.clazz.getName() + " e";
        if (keywords != null && !keywords.equals("")) {
            String searchQuery = getSearchQuery(searchFields);
            if (!searchQuery.equals("")) {
                q += " where (" + searchQuery + ")";
            }
            q += (where != null ? " and " + where : "");
        } else {
            q += (where != null ? " where " + where : "");
        }
        Query query = JPA.em(this.dbName).createQuery(q);
        if (keywords != null && !keywords.equals("") && q.indexOf("?1") != -1) {
            query.setParameter(1, "%" + keywords.toLowerCase() + "%");
        }
        return Long.decode(query.getSingleResult().toString());
    }

    @Override
    public void deleteAll() {
        JPA.em(this.dbName).createQuery("delete from " + this.clazz.getName()).executeUpdate();
    }

    /**
     * List of all properties
     */
    @Override
    public List<Model.Property> listProperties() {
        List<Model.Property> properties = new ArrayList<>();
        Set<Field> fields = new LinkedHashSet<>();
        Class<?> tclazz = this.clazz;
        while (!tclazz.equals(Object.class)) {
            Collections.addAll(fields, tclazz.getDeclaredFields());
            tclazz = tclazz.getSuperclass();
        }
        for (Field f : fields) {
            int mod = f.getModifiers();
            if (Modifier.isTransient(mod) || Modifier.isStatic(mod)) {
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

    @Override
    public String keyName() {
        return keyField().getName();
    }

    @Override
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
        Class<?> tclazz = this.clazz;
        while (!tclazz.equals(Object.class)) {
            // Only consider mapped types
            if (tclazz.isAnnotationPresent(Entity.class) || tclazz.isAnnotationPresent(MappedSuperclass.class)) {
                IdClass idClass = tclazz.getAnnotation(IdClass.class);
                if (idClass != null)
                    return idClass.value();
            }
            tclazz = tclazz.getSuperclass();
        }
        throw new UnexpectedException("Invalid mapping for class " + clazz
                + ": multiple IDs with no @IdClass annotation");
    }

    /**
 * 
 */
    private void initProperties() {
        synchronized (this) {
            if (this.properties != null){
                return;
            }
            this.properties = new HashMap<>();
            Set<Field> fields = getModelFields(this.clazz);
            for (Field f : fields) {
                int mod = f.getModifiers();
                if (Modifier.isTransient(mod) || Modifier.isStatic(mod)) {
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
        if (idProperties == null || idProperties.length == 0){
            throw new UnexpectedException("Composite id has no properties: " + idClass.getName());
        }
        for (PropertyDescriptor idProperty : idProperties) {
            // do we have a field for this?
            String idPropertyName = idProperty.getName();
            // skip the "class" property...
            if (idPropertyName.equals("class"))
                continue;
            Model.Property modelProperty = this.properties.get(idPropertyName);
            if (modelProperty == null)
                throw new UnexpectedException("Composite id property missing: " + clazz.getName() + "."
                        + idPropertyName + " (defined in IdClass " + idClass.getName() + ")");
            // sanity check
            Object value = modelProperty.field.get(model);

            if (modelProperty.isMultiple)
                throw new UnexpectedException("Composite id property cannot be multiple: " + clazz.getName() + "."
                        + idPropertyName);
            // now is this property a relation? if yes then we must use its ID
            // in the key (as per specs)
            if (modelProperty.isRelation) {
                // get its id
                if (!Model.class.isAssignableFrom(modelProperty.type))
                    throw new UnexpectedException("Composite id property entity has to be a subclass of Model: "
                            + clazz.getName() + "." + idPropertyName);
                // we already checked that cast above
                @SuppressWarnings("unchecked")
                Model.Factory factory = Model.Manager.factoryFor((Class<? extends Model>) modelProperty.type);
                if (factory == null)
                    throw new UnexpectedException("Failed to find factory for Composite id property entity: "
                            + clazz.getName() + "." + idPropertyName);
                // we already checked that cast above
                if (value != null)
                    value = factory.keyValue((Model) value);
            }
            // now affect the composite id with this id
            PropertyUtils.setSimpleProperty(id, idPropertyName, value);
        }
        return id;
    }

    @Override
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
            Field[] fields = keyFields();
            Object[] values = new Object[fields.length];
            int i = 0;
            for (Field f : fields) {
                Object o = f.get(m);
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

    public static Set<Field> getModelFields(Class<?> clazz) {
        Set<Field> fields = new LinkedHashSet<>();
        Class<?> tclazz = clazz;
        while (!tclazz.equals(Object.class)) {
            // Only add fields for mapped types
            if (tclazz.isAnnotationPresent(Entity.class) || tclazz.isAnnotationPresent(MappedSuperclass.class))
                Collections.addAll(fields, tclazz.getDeclaredFields());
            tclazz = tclazz.getSuperclass();
        }
        return fields;
    }

    Field keyField() {
        Class<?> c = this.clazz;
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
            throw new UnexpectedException("Error while determining the object @Id for an object of type " + clazz, e);
        }
        throw new UnexpectedException("Cannot get the object @Id for an object of type " + clazz);
    }

    Field[] keyFields() {
        Class<?> c = this.clazz;
        try {
            List<Field> fields = new ArrayList<>();
            while (!c.equals(Object.class)) {
                for (Field field : c.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                        field.setAccessible(true);
                        fields.add(field);
                    }
                }
                c = c.getSuperclass();
            }
            Field[] f = fields.toArray(new Field[fields.size()]);
            if (f.length == 0) {
                throw new UnexpectedException("Cannot get the object @Id for an object of type " + clazz);
            }
            return f;
        } catch (Exception e) {
            throw new UnexpectedException("Error while determining the object @Id for an object of type " + clazz, e);
        }
    }

    String getSearchQuery(List<String> searchFields) {
        StringBuilder q = new StringBuilder("");
        for (Model.Property property : this.listProperties()) {
            if (property.isSearchable
                    && (searchFields == null || searchFields.isEmpty() ? true : searchFields.contains(property.name))) {
                if (q.length() > 0) {
                    q.append(" or ");
                }
                q.append("lower(").append(property.name).append(") like ?1");
            }
        }
        return q.toString();
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
                    final String modelDbName = JPA.getDBName(modelProperty.relationType);
                    modelProperty.choices = new Model.Choices() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public List<Object> list() {
                            return JPA.em(modelDbName).createQuery("from " + field.getType().getName()).getResultList();
                        }
                    };
                }
            }
            if (field.isAnnotationPresent(ManyToOne.class)) {
                modelProperty.isRelation = true;
                modelProperty.relationType = field.getType();
                final String modelDbName = JPA.getDBName(modelProperty.relationType);
                modelProperty.choices = new Model.Choices() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public List<Object> list() {
                        return JPA.em(modelDbName).createQuery("from " + field.getType().getName()).getResultList();
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
                    final String modelDbName = JPA.getDBName(modelProperty.relationType);
                    modelProperty.choices = new Model.Choices() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public List<Object> list() {
                            return JPA.em(modelDbName).createQuery("from " + fieldType.getName()).getResultList();
                        }
                    };
                }
            }
            if (field.isAnnotationPresent(ManyToMany.class)) {
                if (field.getAnnotation(ManyToMany.class).mappedBy().equals("")) {
                    modelProperty.isRelation = true;
                    modelProperty.isMultiple = true;
                    modelProperty.relationType = fieldType;
                    final String modelDbName = JPA.getDBName(field.getType());
                    modelProperty.choices = new Model.Choices() {

                        @SuppressWarnings("unchecked")
                        @Override
                        public List<Object> list() {
                            return JPA.em(modelDbName).createQuery("from " + fieldType.getName()).getResultList();
                        }
                    };
                }
            }
        }
        if (field.getType().isEnum()) {
            modelProperty.choices = new Model.Choices() {

                @SuppressWarnings("unchecked")
                @Override
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
            if (field.getType().isAnnotationPresent(Embeddable.class)
                    || field.getType().isAnnotationPresent(IdClass.class)) {
                modelProperty.isRelation = true;
                modelProperty.relationType = field.getType();
            }
        }
        return modelProperty;
    }
}
