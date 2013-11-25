package play.db.jpa;


import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.type.Type;
import play.Invoker.InvocationContext;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.data.binding.Binder;
import play.data.binding.NoBinding;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;
import play.db.DB;
import play.db.Model;
import play.exceptions.JPAException;
import play.exceptions.UnexpectedException;
import play.utils.Utils;

import javax.persistence.*;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import org.apache.commons.lang.*;
import play.db.Configuration;


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

protected ThreadLocal<Object> entities = new ThreadLocal<Object>();

@Override
public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)  {
 entities.set(entity);
 return super.onSave(entity, id, state, propertyNames, types);
}

@Override
public void afterTransactionCompletion(org.hibernate.Transaction tx) {
 entities.remove();
}

}