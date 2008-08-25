package play.db.jpa;

import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Query;

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
     * store (ie insert or update) the entity
     */
    public void save() {
        getEntityManager().persist(this);         
    }
    
    public <T> T delete() {
        try {
            getEntityManager().remove(this);
            return (T) this;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
  
    /**
     * @return number of entities of this class
     */
    public static Long count() {
        throw new UnsupportedOperationException("Not implemented. Check the JPAEnhancer !");
    }
    
    public static <T extends JPAModel> List<T> findAll() {
    	throw new UnsupportedOperationException("Not implemented. Check the JPAEnhancer !");
    }
    
    public static <T extends JPAModel> T findById(Long id) {
    	throw new UnsupportedOperationException("Not implemented. Check the JPAEnhancer !");
    }
    
    /**
     * find one item matching the parametrized query
     * @param <T>
     * @param query the parametrized query expressed in OQL
     * @param params parameters of the query
     * @return <T> the first item matching the query or null
     */
    public static <T extends JPAModel> T findOneBy(String query, Object... params) {
        throw new UnsupportedOperationException("Not implemented. Check the JPAEnhancer !");
    }
    
    /**
     * find all items matching a parametrized query
     * @param <T>
     * @param query the parametrized query expressed in OQL
     * @param params parameters of the query
     * @return a list<T> of items matching the query
     */
    public static <T extends JPAModel> List<T> findBy(String query, Object... params) {
        throw new UnsupportedOperationException("Not implemented. Check the JPAEnhancer !");
    }
       
    public static EntityManager getEntityManager() {
        return JPAContext.getEntityManager();
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
    
    @SuppressWarnings("unused")
    protected static String createFindByQuery(String entityName, String entityClass, String query, Object... params) {
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
        return  "from " + entityName + " where " + query;       
    }
    
    @SuppressWarnings ("unused")
    protected static Query bindParameters (Query q, Object... params) {
        if(params == null) {
            return q;
        }
    	for (int i=0;i<params.length;i++) {
    		q.setParameter(i+1, params[i]);
    	}
    	return q;
    }
}
