package play.db.jpa;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Query;
import javax.persistence.Transient;
import play.Play;

@MappedSuperclass
public class JPAModel implements Serializable {

    @Id
    @GeneratedValue
    public Long id;

    public Long getId() {
        return id;
    }

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
    
    public static Long count() {
        throw new UnsupportedOperationException("Not implemented. Check the JPAEnhancer !");
    }
    
    public static <T extends JPAModel> List<T> findAll() {
    	throw new UnsupportedOperationException("Not implemented. Check the JPAEnhancer !");
    }
    
    public static <T extends JPAModel> T findById(Long id) {
    	throw new UnsupportedOperationException("Not implemented. Check the JPAEnhancer !");
    }
    
    public static <T extends JPAModel> T findOneBy(String query, Object... params) {
        throw new UnsupportedOperationException("Not implemented. Check the JPAEnhancer !");
    }
    
    public static <T extends JPAModel> List<T> findBy(String query, Object... params) {
        throw new UnsupportedOperationException("Not implemented. Check the JPAEnhancer !");
    }
       
    public static EntityManager getEntityManager() {
        return JPAContext.getEntityManager();
    }
        
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
        if (query.trim().indexOf(" ") == -1 && params.length == 1) {
            query += " = ?";
        }
        try {
            // TODO : Les nouvelle version d'hibernate abandonne le strict JPA-QL et on peut remplacer ca
            // par une simple requete from
            query = " " + query + " ";
            Class clazz = Play.classloader.loadClass(entityClass);
            for (Field field : clazz.getFields()) {
                if (!Modifier.isTransient(field.getModifiers()) && !field.isAnnotationPresent(Transient.class)) {
                    String name = " " + field.getName() + " ";
                    query = query.replace(name, " o." + field.getName() + " ");
                }
            }
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isTransient(field.getModifiers()) && !field.isAnnotationPresent(Transient.class)) {
                    String name = " " + field.getName() + " ";
                    query = query.replace(name, " o." + field.getName() + " ");
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "select o from " + entityName + " o where " + query;
    }
    
    @SuppressWarnings ("unused")
    protected static Query bindParameters (Query q, Object... params) {
    	for (int i=0;i<params.length;i++) {
    		q.setParameter(i+1, params[i]);
    	}
    	return q;
    }
}
