package play.db.jpa;

import java.lang.reflect.Modifier;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.ejb.Ejb3Configuration;
import play.Invoker;
import play.Play;
import play.classloading.ApplicationClasses;
import play.exceptions.JPAException;

/**
 * JPA Support for a specific JPA/DB configuration
 *
 * dbConfigName corresponds to properties-names in application.conf.
 *
 * The default DBConfig is the one configured using 'db.' in application.conf
 *
 * dbConfigName = 'other' is configured like this:
 *
 * db_other = mem
 * db_other.user = batman
 *
 *
 * A particular JPAConfig-instance uses the DBConfig with the same configName
 */

public class JPAConfig {
    private final String configName;
    private EntityManagerFactory entityManagerFactory = null;
    private ThreadLocal<JPAContext> local = new ThreadLocal<JPAContext>();
    public final JPQL jpql;

    protected JPAConfig(Ejb3Configuration cfg, String configName) {
        this.configName = configName;
        invokeJPAConfigurationExtensions(cfg, configName);
        entityManagerFactory = cfg.buildEntityManagerFactory();
        jpql = new JPQL(this);
    }

    public String getConfigName() {
        return configName;
    }

    protected void close() {
        if (isEnabled()) {
            try {
                entityManagerFactory.close();
            } catch (Exception e) {
                // ignore it - we don't care if it failed..
            }
            entityManagerFactory = null;
        }
    }

    /**
     * @return true if an entityManagerFactory has started
     */
    public boolean isEnabled() {
        return entityManagerFactory != null;
    }

    /*
     * Build a new entityManager.
     * (In most case you want to use the local entityManager with em)
     */
    public EntityManager newEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    /**
     * gets the active or create new
     * @return the active JPAContext bound to current thread
     */
    public JPAContext getJPAContext() {
        return getJPAContext(null);
    }


    /**
     * gets the active or create new. manualReadOnly is only used if we're create new context
     * @param manualReadOnly is not null, this value is used instead of value from @Transactional.readOnly
     * @return the active JPAContext bound to current thread
     */
    protected JPAContext getJPAContext(Boolean manualReadOnly) {
        JPAContext context = local.get();
        if ( context == null) {
            // throw new JPAException("The JPAContext is not initialized. JPA Entity Manager automatically start when one or more classes annotated with the @javax.persistence.Entity annotation are found in the application.");

            // This is the first time someone tries to use JPA in this thread.
            // we must initialize it

            if(Invoker.InvocationContext.current() != null && Invoker.InvocationContext.current().getAnnotation(NoTransaction.class) != null ) {
                //Called method or class is annotated with @NoTransaction telling us that
                //we should not start a transaction
                throw new JPAException("Cannot create JPAContext due to @NoTransaction");
            }

            boolean readOnly = false;
            if (manualReadOnly!=null) {
                readOnly = manualReadOnly;
            } else {
               Invoker.InvocationContext invocationContext = Invoker.InvocationContext.current();
               if (invocationContext != null) {
                  Transactional tx = invocationContext.getAnnotation(Transactional.class);
                  if (tx != null) {
                     readOnly = tx.readOnly();
                  }
               }
            }
            context = new JPAContext(this, readOnly, JPAPlugin.autoTxs);

            local.set(context);
        }
        return context;
    }

    protected void clearJPAContext() {
        JPAContext context = local.get();
        if (context != null) {
            try {
                context.close();
            } catch(Exception e) {
                // Let's it fail
            }
            local.remove();
        }
    }

    /**
     * @return true if JPA is enabled in current thread
     */
    public boolean threadHasJPAContext() {
        return local.get() != null;
    }

    public boolean isInsideTransaction() {
        if (!threadHasJPAContext()) {
            return false;
        }
        return getJPAContext().isInsideTransaction();
    }

    /**
     * Looks up all {@link JPAConfigurationExtension} implementations and applies them to the JPA configuration.
     * 
     * @param cfg the {@link} Ejb3Configuration for this {@link JPAConfig}
     * @param configName the name of the db configuration
     */
    protected void invokeJPAConfigurationExtensions(Ejb3Configuration cfg, String configName) {
        for(ApplicationClasses.ApplicationClass c : Play.classes.getAssignableClasses(JPAConfigurationExtension.class)) {
            if(!Modifier.isAbstract(c.getClass().getModifiers())) {
                JPAConfigurationExtension extension = null;
                try {
                    extension = (JPAConfigurationExtension) c.javaClass.newInstance();
                } catch (Throwable t) {
                    throw new JPAException(String.format("Could not instantiate JPAConfigurationExtension '%s'", c.javaClass.getName()), t);
                }
                if(extension.getConfigurationName() == null || extension.getConfigurationName().equals(configName)) {
                    extension.configure(cfg);
                }
            }
        }
    }
}
