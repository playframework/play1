package play.db.jpa;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Registers the Play! flush event listeners, which implement the
 * {@code JPABase.willBeSaved} semantics, on the vanilla Hibernate ORM.
 */
public class PlayHibernateIntegrator implements Integrator {

	@Override
	public void integrate(Metadata metadata, BootstrapContext bootstrapContext, SessionFactoryImplementor sessionFactory) {
		EventListenerRegistry registry = sessionFactory.getServiceRegistry()
				.getService( EventListenerRegistry.class );
		registry.setListeners( EventType.FLUSH, new PlayFlushEventListener() );
		registry.setListeners( EventType.AUTO_FLUSH, new PlayAutoFlushEventListener() );
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
	}
}
