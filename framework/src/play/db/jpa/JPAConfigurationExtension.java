package play.db.jpa;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * Implementors of this class can extend the {@link Ejb3Configuration} before
 * it is used to build the {@link javax.persistence.EntityManagerFactory}
 */
public abstract class JPAConfigurationExtension {

    public JPAConfigurationExtension() {

    }

    /**
     * Override this method in order to restrict the extension to a specific db configuration.
     * By default, this method returns <code>null</code> and hence applies to all {@link JPAConfigurationExtension}-s.
     *
     * @return the db config name as in the properties file this extension should provide to,
     *         <code>null</code> if it should apply to all database configurations.
     */
    public String getConfigurationName() {
        return null;
    }

    /**
     * Performs additional configuration on the JPA configuration.
     *
     * @param cfg the {@link Ejb3Configuration} to configure
     */
    public abstract void configure(Ejb3Configuration cfg);
}
