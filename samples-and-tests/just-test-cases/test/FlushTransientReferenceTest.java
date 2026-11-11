import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import models.AnEntity;
import models.AnotherEntity;
import play.db.jpa.JPA;
import play.test.Fixtures;
import play.test.UnitTest;

public class FlushTransientReferenceTest extends UnitTest {

    @BeforeEach
    public void setup() {
        Fixtures.deleteAll();
    }

    /*
     * An entity on which save() has not been called must be left alone at flush time: it is neither
     * written, nor checked for references to transient instances. AnEntity.parent is a @ManyToOne
     * without cascade, which is the mapping the flush-time check looks at.
     */
    @Test
    public void unsavedOwnerIsNotCheckedForTransientReferences() {
        AnEntity entity = new AnEntity();
        entity.save();

        entity.parent = new AnotherEntity();

        // Any JPQL query triggers an auto-flush. This used to fail with a TransientObjectException.
        assertEquals(1, AnEntity.count());

        // Neither the transient instance nor the reference to it has been written.
        assertEquals(0, AnotherEntity.count());

        JPA.em().clear();
        AnEntity reloaded = AnEntity.findById(entity.id);
        assertNull(reloaded.parent);
    }

    /*
     * The guard must not stop a reference from being written when the owner is actually saved.
     */
    @Test
    public void savedOwnerStillWritesItsReference() {
        AnotherEntity parent = new AnotherEntity();
        parent.save();

        AnEntity entity = new AnEntity();
        entity.parent = parent;
        entity.save();

        JPA.em().clear();
        AnEntity reloaded = AnEntity.findById(entity.id);
        assertNotNull(reloaded.parent);
        assertEquals(parent.id, reloaded.parent.id);
    }

}