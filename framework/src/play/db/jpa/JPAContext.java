package play.db.jpa;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceException;

import play.exceptions.JPAException;

/**
 * JPA Support
 */
public class JPAContext {

    private JPAConfig jpaConfig;
    private EntityManager entityManager;
    private boolean readonly = true;

    protected JPAContext(JPAConfig jpaConfig, boolean readonly, boolean beginTransaction) {

        this.jpaConfig = jpaConfig;

        EntityManager manager = jpaConfig.newEntityManager();
        manager.setFlushMode(FlushModeType.COMMIT);
        manager.setProperty("org.hibernate.readOnly", readonly);

        if (beginTransaction) {
            manager.getTransaction().begin();
        }

        entityManager = manager;
        this.readonly = readonly;
    }

    public JPAConfig getJPAConfig() {
        return jpaConfig;
    }

    /**
     * clear current JPA context and transaction
     * @param rollback shall current transaction be committed (false) or cancelled (true)
     */
    public void closeTx(boolean rollback) {

        try {
            if (entityManager.getTransaction().isActive()) {
                if (readonly || rollback || entityManager.getTransaction().getRollbackOnly()) {
                    entityManager.getTransaction().rollback();
                } else {
                    try {
                        entityManager.getTransaction().commit();
                    } catch (Throwable e) {
                        for (int i = 0; i < 10; i++) {
                            if (e instanceof PersistenceException && e.getCause() != null) {
                                e = e.getCause();
                                break;
                            }
                            e = e.getCause();
                            if (e == null) {
                                break;
                            }
                        }
                        throw new JPAException("Cannot commit", e);
                    }
                }
            }
        } finally {
            entityManager.close();
            //clear context
            jpaConfig.clearJPAContext();
        }

    }

    protected void close() {
        entityManager.close();;
    }

    /*
     * Retrieve the current entityManager
     */
    public EntityManager em() {
        return entityManager;
    }

    /*
     * Tell to JPA do not commit the current transaction
     */
    public void setRollbackOnly() {
        entityManager.getTransaction().setRollbackOnly();
    }


    /**
     * Execute a JPQL query
     */
    public int execute(String query) {
        return entityManager.createQuery(query).executeUpdate();
    }

    /**
     * @return true if current thread is running inside a transaction
     */
    public boolean isInsideTransaction() {
        return entityManager.getTransaction() != null;
    }
}
