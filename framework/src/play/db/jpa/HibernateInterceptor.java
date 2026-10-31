package play.db.jpa;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

public class HibernateInterceptor implements Interceptor {

  public HibernateInterceptor() {
  }

  @Override
  public int[] findDirty(Object o, Object id, Object[] arg2, Object[] arg3, String[] arg4, Type[] arg5) {
    if (o instanceof JPABase && !((JPABase) o).willBeSaved) {
      return new int[0];
    }
    return null;
  }

  private static final ThreadLocal<Object> entities = new ThreadLocal<>();

  static Object currentSaveEntity() {
    return entities.get();
  }

  @Override
  public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
    entities.set(entity);
    return Interceptor.super.onPersist(entity, id, state, propertyNames, types);
  }

  @Override
  public void afterTransactionCompletion(org.hibernate.Transaction tx) {
    entities.remove();
  }
}