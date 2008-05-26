package play.db.jpa;

import javax.persistence.EntityManager;

public class JpaContext {
    
    private static ThreadLocal<JpaContext> local = new ThreadLocal<JpaContext>();
    public EntityManager entityManager;
    boolean readonly=true;
    
    public static JpaContext get () {
        if (local.get()==null)
            throw new IllegalStateException ("The Jpa context is not initialized, enable it in conf/application.conf");
        return local.get();
    }
    
    public static void clearContext () {
        local.remove();
    }
    
    public static void createContext (EntityManager entityManager, boolean readonly) {
        if (local.get()!=null)
            local.remove();
        JpaContext context = new JpaContext();
        context.entityManager=entityManager;
        context.readonly=readonly;
        local.set(context);
    } 
}
