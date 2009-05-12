package play.db.jpa;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import play.data.binding.BeanWrapper;
import play.exceptions.JPAException;
import play.exceptions.UnexpectedException;
import play.mvc.Scope.Params;

/**
 * A super class for JPA entities
 */
public class JPASupport implements Serializable {

    public Object getId() {
        try {
            Field idField = this.getClass().getField("id");
            idField.setAccessible(true);
            return idField.get(this);
        } catch(Exception e) {
            throw new JPAException("The class " + this.getClass() + " does not define any id field.");
        }
    }
    
    public static <T> T create(Class type, String name, Map<String, String[]> params) {
        try {
            Object model = type.newInstance();
            return (T)edit(model, name, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public <T> T edit(String name, Params params) {
        return (T)edit(this, name, params.all());
    }

    public static <T> T edit(Object o, String name, Map<String, String[]> params) {
        try {
            BeanWrapper bw = new BeanWrapper(o.getClass());
            bw.bind(name, o.getClass(), params, "", o);
            // relations
            for (Field field : o.getClass().getDeclaredFields()) {
                boolean isEntity = false;
                String relation = null;
                boolean multiple = false;
                //
                if (field.isAnnotationPresent(OneToOne.class) || field.isAnnotationPresent(ManyToOne.class)) {
                    isEntity = true;
                    relation = field.getType().getSimpleName();
                }
                if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class)) {
                    Class fieldType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    isEntity = true;
                    relation = fieldType.getSimpleName();
                    multiple = true;
                }

                if (isEntity) {
                    if (multiple) {
                        List l = new ArrayList();
                        String[] ids = params.get(name + "." + field.getName());
                        if (ids != null) {
                            for (String _id : ids) {
                                l.add(JPA.getEntityManager().createQuery("from " + relation + " where id = " + _id).getSingleResult());
                            }
                        }
                        field.set(o, l);
                    } else {
                        String[] ids = params.get(name + "." + field.getName());
                        if (ids != null && ids.length > 0 && !ids[0].equals("")) {
                            Object to = (Object) JPA.getEntityManager().createQuery("from " + relation + " where id = " + ids[0]).getSingleResult();
                            field.set(o, to);
                        } else {
                            field.set(o, null);
                        }
                    }
                }
                if(o instanceof JPAModel && field.getType().equals(FileAttachment.class)) {
                    FileAttachment fileAttachment = ((FileAttachment)field.get(o));
                    if(fileAttachment == null) {
                        fileAttachment = new FileAttachment((JPAModel)o, field.getName());
                        field.setAccessible(true);
                        field.set(o, fileAttachment);
                    }
                    File file = Params.current().get(name + "." +field.getName(), File.class);
                    if(file != null && file.exists() && file.length() > 0) {
                        fileAttachment.set(Params.current().get(name + "." +field.getName(), File.class));
                        fileAttachment.filename = file.getName();
                    } else {
                        String df = Params.current().get(name + "." +field.getName()+"_delete_", String.class);
                        if(df != null && df.equals("true")) {
                            fileAttachment.delete();
                            field.set(o, null);
                        }
                    }
                }
            }
            return (T) o;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    /**
     * store (ie insert or update) the entity.
     */
    public <T> T save() {
        em().persist(this);
        return (T) this;
    }

    /**
     * Refresh the entity state.
     */
    public <T> T refresh() {
        em().refresh(this);
        return (T) this;
    }

    /**
     * Merge this object to obtain a manager entity.
     */
    public <T> T merge() {
        return (T) em().merge(this);
    }

    /**
     * Delete the entity.
     * @return The deleted entity.
     */
    public <T> T delete() {
        try {
            em().remove(this);
            return (T) this;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T create(String name, Params params) {
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
     * Find all entities for this class
     * @return A list of entity
     */
    public static <T extends JPASupport> List<T> findAll() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Find the entity with the corresponding id.
     * @param id The entity id
     * @return The entity
     */
    public static <T extends JPASupport> T findById(Object id) {
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
     * @param query HQL query or shortcut
     * @param params Params to bind to the query
     * @return A JPAQuery
     */
    public static JPAQuery find() {
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
     * find one item matching the parametrized query
     * @param <T>
     * @param query the parametrized query expressed in OQL
     * @param params parameters of the query
     * @return <T> the first item matching the query or null
     */
    public static <T extends JPASupport> T findOneBy(String query, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * find all items matching a parametrized query
     * @param <T>
     * @param query the parametrized query expressed in OQL
     * @param params parameters of the query
     * @return a list<T> of items matching the query
     */
    public static <T extends JPASupport> List<T> findBy(String query, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Retrieve the current entityManager
     * @return the current entityManager
     */
    public static EntityManager em() {
        return JPA.getEntityManager();
    }

    /**
     * Retrieve the current entityManager
     * @return the current entityManager
     * @deprecated 
     */
    public static EntityManager getEntityManager() {
        return JPA.getEntityManager();
    }

    /**
     * JPASupport instances a and b are equals if either <strong>a == b</strong> or a and b have same </strong>{@link #id id} and class</strong>
     * @param other 
     * @return true if equality condition above is verified
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if ((this == other)) {
            return true;
        }
        if (!(this.getClass().equals(other.getClass()))) {
            return false;
        }
        if (this.getId() == null) {
            return false;
        }
        return this.getId().equals(((JPASupport) other).getId());
    }

    @Override
    public int hashCode() {
        if (this.getId() == null) {
            return 0;
        }
        return this.getId().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getId() + "]";
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

        /**
         * Retrieve the first result of the query or null
         * @return An entity or null
         */
        public <T extends JPASupport> T one() {
            try {
                List<T> results = query.setMaxResults(1).getResultList();
                if (results.size() == 0) {
                    return null;
                }
                return (T) results.get(0);
            } catch(Exception e) {
                throw new IllegalArgumentException("Error while executing query <strong>"+sq+"</strong>", e);
            }
        }

        /**
         * Retrieve all results of the query
         * @return A list of entities
         */
        public <T extends JPASupport> List<T> all() {
            try {
                return query.getResultList();
            } catch(Exception e) {
                throw new IllegalArgumentException("Error while executing query <strong>"+sq+"</strong>", e);
            }
        }

        /**
         * Retrieve a page of result
         * @param from Page number (start at 1)
         * @param length (page length)
         * @return A list of entities
         */
        public <T extends JPASupport> List<T> page(int from, int length) {
            if (from < 1) {
                throw new IllegalArgumentException("Page start at 1");
            }
            query.setFirstResult((from - 1) * length);
            query.setMaxResults(length);
            try {
                return query.getResultList();
            } catch(Exception e) {
                throw new IllegalArgumentException("Error while executing query <strong>"+sq+"</strong>", e);
            }
        }
    }

    // File attachments

    public void setupAttachment() {
        for (Field field : getClass().getFields()) {
            if (field.getType().equals(FileAttachment.class)) {
                try {
                    FileAttachment attachment = (FileAttachment)field.get(this);
                    if(attachment != null) {
                        attachment.model = this;
                        attachment.name = field.getName();
                    }
                } catch (Exception ex) {
                    throw new UnexpectedException(ex);
                }
            }
        }
    }

    public void saveAttachment() {
        for (Field field : getClass().getFields()) {
            if (field.getType().equals(FileAttachment.class)) {
                try {
                    FileAttachment attachment = (FileAttachment)field.get(this);
                    if(attachment != null) {
                        attachment.save();
                    }
                } catch (Exception ex) {
                    throw new UnexpectedException(ex);
                }
            }
        }
    }
}
