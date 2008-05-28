package play.db.jpa;

import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityManager;

public class JPAModel implements Serializable {

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
    
    public static <T extends JPAModel> T findOneBy(String query, Object... params) {
        throw new UnsupportedOperationException("Not implemented. Check the JPAEnhancer !");
    }
    
    public static <T extends JPAModel> List<T> findBy(String query, Object... params) {
        throw new UnsupportedOperationException("Not implemented. Check the JPAEnhancer !");
    }
    
    public static Integer deleteBy(String query, Object... params) {
        throw new UnsupportedOperationException("Not implemented. Check the JPAEnhancer !");
    }
    
    public EntityManager getEntityManager() {
        return JPAContext.getEntityManager();
    }
    
    private Object getId() {
        return null;
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
    
}
