package play.db.jpa;


import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.type.Type;
import java.io.Serializable;


public class HibernateInterceptor extends EmptyInterceptor {

  public HibernateInterceptor() {

  }
  
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

    protected final ThreadLocal<Object> entities = new ThreadLocal<>();

    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        entities.set(entity);
        return super.onSave(entity, id, state, propertyNames, types);
    }

    @Override
    public void afterTransactionCompletion(org.hibernate.Transaction tx) {
        entities.remove();
    }
}