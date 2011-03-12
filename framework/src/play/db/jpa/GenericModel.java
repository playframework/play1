package play.db.jpa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import play.Play;
import play.data.binding.BeanWrapper;
import play.data.binding.Binder;
import play.data.validation.Validation;
import play.exceptions.UnexpectedException;
import play.mvc.Scope.Params;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import play.Logger;
import play.utils.Utils;

/**
 * A super class for JPA entities
 */
@MappedSuperclass
@SuppressWarnings("unchecked")
public class GenericModel extends JPABase {

    public static <T extends JPABase> T create(Class<?> type, String name, Map<String, String[]> params, Annotation[] annotations) {
        try {
            Constructor c = type.getDeclaredConstructor();
            c.setAccessible(true);
            Object model = c.newInstance();
            return (T) edit(model, name, params, annotations);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("deprecation")
    public static <T extends JPABase> T edit(Object o, String name, Map<String, String[]> params, Annotation[] annotations) {
        try {
            BeanWrapper bw = BeanWrapper.forClass(o.getClass());
            // Start with relations
            Set<Field> fields = new HashSet<Field>();
            Class clazz = o.getClass();
            while (!clazz.equals(Object.class)) {
                Collections.addAll(fields, clazz.getDeclaredFields());
                clazz = clazz.getSuperclass();
            }
            for (Field field : fields) {
                boolean isEntity = false;
                String relation = null;
                boolean multiple = false;
                //
                if (field.isAnnotationPresent(OneToOne.class) || field.isAnnotationPresent(ManyToOne.class)) {
                    isEntity = true;
                    relation = field.getType().getName();
                }
                if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class)) {
                    Class fieldType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    isEntity = true;
                    relation = fieldType.getName();
                    multiple = true;
                }

                if (isEntity) {
                    Class<Model> c = (Class<Model>) Play.classloader.loadClass(relation);
                    if (JPABase.class.isAssignableFrom(c)) {
                        String keyName = Model.Manager.factoryFor(c).keyName();
                        if (multiple && Collection.class.isAssignableFrom(field.getType())) {
                            Collection l = new ArrayList();
                            if (SortedSet.class.isAssignableFrom(field.getType())) {
                                l = new TreeSet();
                            } else if (Set.class.isAssignableFrom(field.getType())) {
                                l = new HashSet();
                            }
                            String[] ids = params.get(name + "." + field.getName() + "." + keyName);
                            if (ids != null) {
                                params.remove(name + "." + field.getName() + "." + keyName);
                                for (String _id : ids) {
                                    if (_id.equals("")) {
                                        continue;
                                    }
                                    Query q = JPA.em().createQuery("from " + relation + " where " + keyName + " = ?");
                                    q.setParameter(1, Binder.directBind(_id, Model.Manager.factoryFor((Class<Model>) Play.classloader.loadClass(relation)).keyType()));
                                    try {
                                        l.add(q.getSingleResult());
                                    } catch (NoResultException e) {
                                        Validation.addError(name + "." + field.getName(), "validation.notFound", _id);
                                    }
                                }
                                bw.set(field.getName(), o, l);
                            }
                        } else {
                            String[] ids = params.get(name + "." + field.getName() + "." + keyName);
                            if (ids != null && ids.length > 0 && !ids[0].equals("")) {
                                params.remove(name + "." + field.getName() + "." + keyName);
                                Query q = JPA.em().createQuery("from " + relation + " where " + keyName + " = ?");
                                q.setParameter(1, Binder.directBind(ids[0], Model.Manager.factoryFor((Class<Model>) Play.classloader.loadClass(relation)).keyType()));
                                try {
                                    String localName = name + "." + field.getName();
                                    Object to = q.getSingleResult();
                                    edit(to, localName, params, field.getAnnotations());
                                    params = Utils.filterMap(params, localName);
                                    bw.set(field.getName(), o, to);
                                } catch (NoResultException e) {
                                    Validation.addError(name + "." + field.getName(), "validation.notFound", ids[0]);
                                }
                            } else if (ids != null && ids.length > 0 && ids[0].equals("")) {
                                bw.set(field.getName(), o, null);
                                params.remove(name + "." + field.getName() + "." + keyName);
                            }
                        }
                    }
                }
                // ----- THIS CODE IS DEPRECATED AND WILL BE REMOVED IN NEXT VERSION -----
                if (field.getType().equals(FileAttachment.class) && Params.current() != null) {
                    FileAttachment fileAttachment = ((FileAttachment) field.get(o));
                    if (fileAttachment == null) {
                        fileAttachment = new FileAttachment(o, field.getName());
                        bw.set(field.getName(), o, fileAttachment);
                    }
                    File file = Params.current().get(name + "." + field.getName(), File.class);
                    if (file != null && file.exists() && file.length() > 0) {
                        fileAttachment.set(Params.current().get(name + "." + field.getName(), File.class));
                        fileAttachment.filename = file.getName();
                    } else {
                        String df = Params.current().get(name + "." + field.getName() + "_delete_", String.class);
                        if (df != null && df.equals("true")) {
                            fileAttachment.delete();
                            bw.set(field.getName(), o, null);
                        }
                    }
                    params.remove(name + "." + field.getName());
                }
                // -----
            }
            bw.bind(name, o.getClass(), params, "", o, annotations);
            return (T) o;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public <T extends GenericModel> T edit(String name, Map<String, String[]> params) {
        edit(this, name, params, new Annotation[0]);
        return (T) this;
    }

    public boolean validateAndSave() {
        if (Validation.current().valid(this).ok) {
            save();
            return true;
        }
        return false;
    }

    public boolean validateAndCreate() {
        if (Validation.current().valid(this).ok) {
            return create();
        }
        return false;
    }

    /**
     * store (ie insert) the entity.
     */
    public <T extends JPABase> T save() {
        if (!em().contains(this) && Play.mode.isDev()) {
            StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
            StackTraceElement caller = callStack[2];
            Logger.warn("save() has been called to persist a new JPA instance at %s line %s, use create() instead.", caller.getFileName(), caller.getLineNumber());
        }
        _save();
        return (T) this;
    }

    /**
     * store (ie insert) the entity.
     */
    public boolean create() {
        if (!em().contains(this)) {
            _save();
            return true;
        }
        return false;
    }

    /**
     * Refresh the entity state.
     */
    public <T extends JPABase> T refresh() {
        em().refresh(this);
        return (T) this;
    }

    /**
     * Merge this object to obtain a managed entity (usefull when the object comes from the Cache).
     */
    public <T extends JPABase> T merge() {
        return (T) em().merge(this);
    }

    /**
     * Delete the entity.
     * @return The deleted entity.
     */
    public <T extends JPABase> T delete() {
        _delete();
        return (T) this;
    }

    public static <T extends JPABase> T create(String name, Params params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Count entities
     * @return number of entities of this class
     */
    public static long count() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Count entities with a special query.
     * Example : Long moderatedPosts = Post.count("moderated", true);
     * @param query HQL query or shortcut
     * @param params Params to bind to the query
     * @return A long
     */
    public static long count(String query, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Find all entities of this type
     */
    public static <T extends JPABase> List<T> findAll() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Find the entity with the corresponding id.
     * @param id The entity id
     * @return The entity
     */
    public static <T extends JPABase> T findById(Object id) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Prepare a query to find entities.
     * @param query HQL query or shortcut
     * @param params Params to bind to the query
     * @return A JPAQuery
     */
    public static JPAQuery find(String query, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Prepare a query to find *all* entities.
     * @return A JPAQuery
     */
    public static JPAQuery all() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Batch delete of entities
     * @param query HQL query or shortcut
     * @param params Params to bind to the query
     * @return Number of entities deleted
     */
    public static int delete(String query, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Delete all entities
     * @return Number of entities deleted
     */
    public static int deleteAll() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * A JPAQuery
     */
    public static class JPAQuery {

        public Query query;
        public String sq;

        public JPAQuery(String sq, Query query) {
            this.query = query;
            this.sq = sq;
        }

        public JPAQuery(Query query) {
            this.query = query;
            this.sq = query.toString();
        }

        public <T> T first() {
            try {
                List<T> results = query.setMaxResults(1).getResultList();
                if (results.isEmpty()) {
                    return null;
                }
                return results.get(0);
            } catch (Exception e) {
                throw new JPAQueryException("Error while executing query <strong>" + sq + "</strong>", JPAQueryException.findBestCause(e));
            }
        }

        /**
         * Bind a JPQL named parameter to the current query.
         */
        public JPAQuery bind(String name, Object param) {
            if (param.getClass().isArray()) {
                param = Arrays.asList((Object[]) param);
            }
            if (param instanceof Integer) {
                param = ((Integer) param).longValue();
            }
            query.setParameter(name, param);
            return this;
        }

        /**
         * Retrieve all results of the query
         * @return A list of entities
         */
        public <T> List<T> fetch() {
            try {
                return query.getResultList();
            } catch (Exception e) {
                throw new JPAQueryException("Error while executing query <strong>" + sq + "</strong>", JPAQueryException.findBestCause(e));
            }
        }

        /**
         * Retrieve results of the query
         * @param max Max results to fetch
         * @return A list of entities
         */
        public <T> List<T> fetch(int max) {
            try {
                query.setMaxResults(max);
                return query.getResultList();
            } catch (Exception e) {
                throw new JPAQueryException("Error while executing query <strong>" + sq + "</strong>", JPAQueryException.findBestCause(e));
            }
        }

        /**
         * Set the position to start
         * @param position Position of the first element
         * @return A new query
         */
        public <T> JPAQuery from(int position) {
            query.setFirstResult(position);
            return this;
        }

        /**
         * Retrieve a page of result
         * @param page Page number (start at 1)
         * @param length (page length)
         * @return a list of entities
         */
        public <T> List<T> fetch(int page, int length) {
            if (page < 1) {
                page = 1;
            }
            query.setFirstResult((page - 1) * length);
            query.setMaxResults(length);
            try {
                return query.getResultList();
            } catch (Exception e) {
                throw new JPAQueryException("Error while executing query <strong>" + sq + "</strong>", JPAQueryException.findBestCause(e));
            }
        }
    }

    // ----- THIS CODE IS DEPRECATED AND WILL BE REMOVED IN NEXT VERSIONs
    @PostLoad
    @SuppressWarnings("deprecation")
    public void _setupAttachment() {
        Class c = this.getClass();
        while (!c.equals(Object.class)) {
            for (Field field : c.getDeclaredFields()) {
                if (FileAttachment.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        FileAttachment attachment = (FileAttachment) field.get(this);
                        if (attachment != null) {
                            attachment.model = this;
                            attachment.name = field.getName();
                        } else {
                            attachment = new FileAttachment();
                            attachment.model = this;
                            attachment.name = field.getName();
                            field.set(this, attachment);
                        }
                    } catch (Exception ex) {
                        throw new UnexpectedException(ex);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    @PostPersist
    @PostUpdate
    @SuppressWarnings("deprecation")
    public void _saveAttachment() {
        Class c = this.getClass();
        while (!c.equals(Object.class)) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getType().equals(FileAttachment.class)) {
                    try {
                        field.setAccessible(true);
                        FileAttachment attachment = (FileAttachment) field.get(this);
                        if (attachment != null) {
                            attachment.model = this;
                            attachment.name = field.getName();
                            attachment.save();
                        }
                    } catch (Exception ex) {
                        throw new UnexpectedException(ex);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }
    // -----
}
