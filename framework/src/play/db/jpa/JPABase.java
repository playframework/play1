package play.db.jpa;

import org.hibernate.Session;
import org.hibernate.type.Type;
import org.hibernate.type.EntityType;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.collection.*;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.*;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.impl.SessionImpl;
import org.hibernate.proxy.HibernateProxy;
import play.PlayPlugin;
import play.exceptions.UnexpectedException;

import javax.persistence.*;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.*;

/**
 * A super class for JPA entities
 */
@MappedSuperclass
public class JPABase implements Serializable, play.db.Model {

    public void _save() {
        if (!em().contains(this)) {
            em().persist(this);
            PlayPlugin.postEvent("JPASupport.objectPersisted", this);
        }
        avoidCascadeSaveLoops.set(new HashSet<JPABase>());
        try {
            saveAndCascade(true);
        } finally {
            avoidCascadeSaveLoops.get().clear();
        }
        try {
            em().flush();
        } catch (PersistenceException e) {
            if (e.getCause() instanceof GenericJDBCException) {
                throw new PersistenceException(((GenericJDBCException) e.getCause()).getSQL(), e);
            } else {
                throw e;
            }
        }
        avoidCascadeSaveLoops.set(new HashSet<JPABase>());
        try {
            saveAndCascade(false);
        } finally {
            avoidCascadeSaveLoops.get().clear();
        }
    }

    public void _delete() {
        try {
            avoidCascadeSaveLoops.set(new HashSet<JPABase>());
            try {
                saveAndCascade(true);
            } finally {
                avoidCascadeSaveLoops.get().clear();
            }
            em().remove(this);
            try {
                em().flush();
            } catch (PersistenceException e) {
                if (e.getCause() instanceof GenericJDBCException) {
                    throw new PersistenceException(((GenericJDBCException) e.getCause()).getSQL(), e);
                } else {
                    throw e;
                }
            }
            avoidCascadeSaveLoops.set(new HashSet<JPABase>());
            try {
                saveAndCascade(false);
            } finally {
                avoidCascadeSaveLoops.get().clear();
            }
            PlayPlugin.postEvent("JPASupport.objectDeleted", this);
        } catch (PersistenceException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public Object _key() {
        return Model.Manager.factoryFor(this.getClass()).keyValue(this);
    }

    // ~~~ SAVING
    public transient boolean willBeSaved = false;
    static transient ThreadLocal<Set<JPABase>> avoidCascadeSaveLoops = new ThreadLocal<Set<JPABase>>();

    private void saveAndCascade(boolean willBeSaved) {
        this.willBeSaved = willBeSaved;
        if (avoidCascadeSaveLoops.get().contains(this)) {
            return;
        } else {
            avoidCascadeSaveLoops.get().add(this);
            if (willBeSaved) {
                PlayPlugin.postEvent("JPASupport.objectUpdated", this);
            }
        }
        // Cascade save
        try {
            Set<Field> fields = new HashSet<Field>();
            Class clazz = this.getClass();
            while (!clazz.equals(JPABase.class)) {
                java.util.Collections.addAll(fields, clazz.getDeclaredFields());
                clazz = clazz.getSuperclass();
            }
            for (Field field : fields) {
                field.setAccessible(true);
                if (Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                boolean doCascade = false;
                if (field.isAnnotationPresent(OneToOne.class)) {
                    doCascade = cascadeAll(field.getAnnotation(OneToOne.class).cascade());
                }
                if (field.isAnnotationPresent(OneToMany.class)) {
                    doCascade = cascadeAll(field.getAnnotation(OneToMany.class).cascade());
                }
                if (field.isAnnotationPresent(ManyToOne.class)) {
                    doCascade = cascadeAll(field.getAnnotation(ManyToOne.class).cascade());
                }
                if (field.isAnnotationPresent(ManyToMany.class)) {
                    doCascade = cascadeAll(field.getAnnotation(ManyToMany.class).cascade());
                }
                if (doCascade) {
                    Object value = field.get(this);
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof PersistentMap) {
                        if (((PersistentMap) value).wasInitialized()) {

                            cascadeOrphans(this, (PersistentCollection) value, willBeSaved);

                            for (Object o : ((Map) value).values()) {
                                saveAndCascadeIfJPABase(o, willBeSaved);
                            }
                        }
                        continue;
                    }
                    if (value instanceof PersistentCollection) {
                        if (((PersistentCollection) value).wasInitialized()) {

                            cascadeOrphans(this, (PersistentCollection) value, willBeSaved);

                            for (Object o : (Collection) value) {
                                saveAndCascadeIfJPABase(o, willBeSaved);
                            }
                        }
                        continue;
                    }
                    if (value instanceof HibernateProxy && value instanceof JPABase) {
                        if (!((HibernateProxy) value).getHibernateLazyInitializer().isUninitialized()) {
                            ((JPABase) ((HibernateProxy) value).getHibernateLazyInitializer().getImplementation()).saveAndCascade(willBeSaved);
                        }
                        continue;
                    }
                    if (value instanceof JPABase) {
                        ((JPABase) value).saveAndCascade(willBeSaved);
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            throw new UnexpectedException("During cascading save()", e);
        }
    }

    private static void cascadeOrphans(JPABase base, PersistentCollection persistentCollection, boolean willBeSaved) {
        SessionImpl session = ((SessionImpl) JPA.em().getDelegate());
        CollectionEntry ce = session.getPersistenceContext().getCollectionEntry(persistentCollection);

        if (ce != null) {
            CollectionPersister cp = ce.getLoadedPersister();
            if (cp != null) {
                Type ct = cp.getElementType();
                if (ct instanceof EntityType) {
                    String entityName = ((EntityType) ct).getAssociatedEntityName(session.getFactory());
                    Collection orphans = ce.getOrphans(entityName, persistentCollection);
                    for (Object o : orphans) {
                        saveAndCascadeIfJPABase(o, willBeSaved);
                    }
                }
            }
        }
    }

    private static void saveAndCascadeIfJPABase(Object o, boolean willBeSaved) {
        if (o instanceof JPABase) {
            ((JPABase) o).saveAndCascade(willBeSaved);
        }
    }

    private static boolean cascadeAll(CascadeType[] types) {
        for (CascadeType cascadeType : types) {
            if (cascadeType == CascadeType.ALL || cascadeType == CascadeType.PERSIST) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieve the current entityManager
     *
     * @return the current entityManager
     */
    public static EntityManager em() {
        return JPA.em();
    }

    public boolean isPersistent() {
        return JPA.em().contains(this);
    }

    /**
     * JPASupport instances a and b are equals if either <strong>a == b</strong> or a and b have same </strong>{@link #key key} and class</strong>
     *
     * @param other
     * @return true if equality condition above is verified
     */
    @Override
    public boolean equals(Object other) {
        final Object key = this._key();

        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (key == null) {
            return false;
        }
        if (play.db.Model.class.isAssignableFrom(other.getClass()) && key.getClass().isArray()) {
            Object otherKey = ((play.db.Model) other)._key();
            if (otherKey.getClass().isArray()) {
                return Arrays.deepEquals((Object[]) key, (Object[]) otherKey);
            }
            return false;
        }


        if (!this.getClass().isAssignableFrom(other.getClass())) {
            return false;
        }

        return key.equals(((play.db.Model) other)._key());
    }

    @Override
    public int hashCode() {
        final Object key = this._key();
        if (key == null) {
            return 0;
        }
        if (key.getClass().isArray()) {
            return Arrays.deepHashCode((Object[]) key);
        }
        return key.hashCode();
    }

    @Override
    public String toString() {
        final Object key = this._key();
        String keyStr = "";
        if (key != null && key.getClass().isArray()) {
            for (Object object : (Object[]) key) {
                keyStr += object.toString() + ", ";
            }
            keyStr = keyStr.substring(0, keyStr.length() - 2);
        } else if (key != null) {
            keyStr = key.toString();
        }
        return getClass().getSimpleName() + "[" + keyStr + "]";
    }

    public static class JPAQueryException extends RuntimeException {

        public JPAQueryException(String message) {
            super(message);
        }

        public JPAQueryException(String message, Throwable e) {
            super(message + ": " + e.getMessage(), e);
        }

        public static Throwable findBestCause(Throwable e) {
            Throwable best = e;
            Throwable cause = e;
            int it = 0;
            while ((cause = cause.getCause()) != null && it++ < 10) {
                if (cause instanceof ClassCastException) {
                    best = cause;
                    break;
                }
                if (cause instanceof SQLException) {
                    best = cause;
                    break;
                }
            }
            return best;
        }
    }

    //

    @Deprecated
    public Object getEntityId() {
        return _key();
    }

}
