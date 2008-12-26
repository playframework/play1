package play.db.jpa;

import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Query;

/**
 * A super class for JPA entities
 */
@MappedSuperclass
public class JPAModel implements Serializable {

    @Id
    @GeneratedValue
    public Long id;

    /**
     * the JPAModel class is responsible for the JPA entity @Id field. 
     * @return Long the JPA @Id value of this entity
     */
    public Long getId() {
        return id;
    }

    /**
     * store (ie insert or update) the entity.
     */
    public <T> T save() {
        em().persist(this);
        return (T)this;
    }

    /**
     * Refresh the entity state.
     */
    public <T> T refresh() {
        em().refresh(this);
        return (T)this;
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

    /**
     * Count entities
     * @return number of entities of this class
     */
    public static Long count() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Count entities with a special query.
     * Example : Long moderatedPosts = Post.count("moderated", true);
     * @param query HQL query or shortcut
     * @param params Params to bind to the query
     * @return A long
     */
    public static Long count(String query, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Find all entities for this class
     * @return A list of entity
     */
    public static <T extends JPAModel> List<T> findAll() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Find the entity with the corresponding id.
     * @param id The entity id
     * @return The entity
     */
    public static <T extends JPAModel> T findById(Long id) {
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
    public static <T extends JPAModel> T findOneBy(String query, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * find all items matching a parametrized query
     * @param <T>
     * @param query the parametrized query expressed in OQL
     * @param params parameters of the query
     * @return a list<T> of items matching the query
     */
    public static <T extends JPAModel> List<T> findBy(String query, Object... params) {
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
     * JPAModel instances a and b are equals if either <strong>a == b</strong> or a and b have same </strong>{@link #id id} and class</strong>
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
        return this.getId().equals(((JPAModel) other).getId());
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
        return getClass().getSimpleName() + "[" + id + "]";
    }

    @SuppressWarnings("unused")
    protected static String createFindByQuery(String entityName, String entityClass, String query, Object... params) {
        if (query == null) {
            return "from " + entityName;
        }
        if (query.trim().toLowerCase().startsWith("select ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return query;
        }
        if (query.trim().indexOf(" ") == -1 && params != null && params.length == 1) {
            query += " = ?";
        }
        if (query.trim().indexOf(" ") == -1 && params == null) {
            query += " = null";
        }
        return "from " + entityName + " where " + query;
    }

    @SuppressWarnings("unused")
    protected static String createDeleteQuery(String entityName, String entityClass, String query, Object... params) {
        if (query == null) {
            return "delete from " + entityName;
        }
        if (query.trim().toLowerCase().startsWith("delete ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return "delete " + query;
        }
        if (query.trim().indexOf(" ") == -1 && params != null && params.length == 1) {
            query += " = ?";
        }
        if (query.trim().indexOf(" ") == -1 && params == null) {
            query += " = null";
        }
        return "delete from " + entityName + " where " + query;
    }

    @SuppressWarnings("unused")
    protected static String createCountQuery(String entityName, String entityClass, String query, Object... params) {
        if (query.trim().toLowerCase().startsWith("select ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return "select count(*) " + query;
        }
        if (query.trim().indexOf(" ") == -1 && params != null && params.length == 1) {
            query += " = ?";
        }
        if (query.trim().indexOf(" ") == -1 && params == null) {
            query += " = null";
        }
        return "select count(*) from " + entityName + " where " + query;
    }

    @SuppressWarnings("unused")
    protected static Query bindParameters(Query q, Object... params) {
        if (params == null) {
            return q;
        }
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i + 1, params[i]);
        }
        return q;
    }

    /**
     * A JPAQuery
     */
    public static class JPAQuery {

        public Query query;

        public JPAQuery(Query query) {
            this.query = query;
        }

        /**
         * Retrieve the first result of the query or null
         * @return An entity or null
         */
        public <T extends JPAModel> T one() {
            List<T> results = query.setMaxResults(1).getResultList();
            if (results.size() == 0) {
                return null;
            }
            return (T) results.get(0);
        }

        /**
         * Retrieve all results of the query
         * @return A list of entities
         */
        public <T extends JPAModel> List<T> all() {
            return query.getResultList();
        }

        /**
         * Retrieve a page of result
         * @param from Page number (start at 1)
         * @param length (page length)
         * @return A list of entities
         */
        public <T extends JPAModel> List<T> page(int from, int length) {
            if (from < 1) {
                throw new IllegalArgumentException("Page start at 1");
            }
            query.setFirstResult((from - 1) * length);
            query.setMaxResults(length);
            return query.getResultList();
        }
    }
}
