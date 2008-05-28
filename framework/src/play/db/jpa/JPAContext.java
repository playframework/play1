package play.db.jpa;

import javax.persistence.EntityManager;

public class JPAContext {

    private static ThreadLocal<JPAContext> local = new ThreadLocal<JPAContext>();
    public EntityManager entityManager;
    boolean readonly = true;

    public static JPAContext get() {
        if (local.get() == null) {
            throw new IllegalStateException("The JPA context is not initialized, enable it in your conf/application.conf");
        }
        return local.get();
    }

    public static EntityManager getEntityManager() {
        return get().entityManager;
    }

    public static void clearContext() {
        local.remove();
    }

    public static void createContext(EntityManager entityManager, boolean readonly) {
        if (local.get() != null) {
            local.remove();
        }
        JPAContext context = new JPAContext();
        context.entityManager = entityManager;
        context.readonly = readonly;
        local.set(context);
    }
}
