package play.db.jpa;


import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.type.Type;
import java.io.Serializable;


public class HibernateInterceptor implements Interceptor {

  public HibernateInterceptor() {

  }

  @Override
  public int[] findDirty(Object o, Serializable id, Object[] arg2, Object[] arg3, String[] arg4, Type[] arg5) {
    if (!JPAPlugin.explicitSave) {
      return null;
    }
    if (o instanceof JPABase && !((JPABase) o).willBeSaved) {
      return new int[0];
    }
    return null;
  }

    @Override
    public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
        if (!JPAPlugin.explicitSave) {
            return;
        }
        if (collection instanceof PersistentCollection) {
            Object o = ((PersistentCollection) collection).getOwner();
            if (o instanceof JPABase) {
                // nothing to return - side effect only
            }
        } else {
            System.out.println("HOO: Case not handled !!!");
        }
    }

    @Override
    public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
        if (!JPAPlugin.explicitSave) {
            return;
        }
        if (collection instanceof PersistentCollection) {
            Object o = ((PersistentCollection) collection).getOwner();
            if (o instanceof JPABase) {
                // nothing to return - side effect only
            }
        } else {
            System.out.println("HOO: Case not handled !!!");
        }
    }

    @Override
    public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
        if (!JPAPlugin.explicitSave) {
            return;
        }
        if (collection instanceof PersistentCollection) {
            Object o = ((PersistentCollection) collection).getOwner();
            if (o instanceof JPABase) {
                // nothing to return - side effect only
            }
        } else {
            System.out.println("HOO: Case not handled !!!");
        }
    }

    protected final ThreadLocal<Object> entities = new ThreadLocal<>();

    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        entities.set(entity);
        return false;
    }

    @Override
    public void afterTransactionCompletion(org.hibernate.Transaction tx) {
        entities.remove();
    }
}
