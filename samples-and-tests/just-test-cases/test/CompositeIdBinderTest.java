import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.CompositeIdEntity;
import models.CompositeIdForeignA;
import models.CompositeIdForeignB;
import models.CompositeIdPk;

import org.junit.Before;
import org.junit.Test;

import play.Logger;
import play.data.binding.Binder;
import play.db.Model;
import play.db.Model.Factory;
import play.db.Model.Property;
import play.db.jpa.JPA;
import play.test.Fixtures;
import play.test.UnitTest;

public class CompositeIdBinderTest extends UnitTest {
    
    @Before
    public void setup() {
    	Fixtures.delete(CompositeIdEntity.class, CompositeIdForeignA.class, CompositeIdForeignB.class);
    }

    @Test
    public void testBinderFound() {
    	CompositeIdForeignA a = new CompositeIdForeignA();
        a.testId = "Hello from A";
    	a.save();
    	CompositeIdForeignB b = new CompositeIdForeignB();
        b.testId = "Hello from B";
    	b.save();
    	CompositeIdEntity e = new CompositeIdEntity();
    	e.compositeIdForeignA = a;
    	e.compositeIdForeignB = b;
    	e.save();

    	Map<String, String[]> params = new HashMap<String, String[]>();
    	params.put("object.compositeIdForeignA.id", new String[]{a.id.toString()});
    	params.put("object.compositeIdForeignB.id", new String[]{b.id.toString()});
		CompositeIdEntity bound = (CompositeIdEntity)Binder.bind("object", CompositeIdEntity.class, CompositeIdEntity.class, null, params);

        Logger.info("1" + e._key());
        Logger.info("2" + bound._key());



		// they have to be the same object
      	assertEquals(bound, e);
    }

    @Test
    public void testBinderNotFound() {
    	Map<String, String[]> params = new HashMap<String, String[]>();
    	params.put("object.compositeIdForeignA.id", new String[]{"10000"});
    	params.put("object.compositeIdForeignB.id", new String[]{"10000"});
		Object bound = Binder.bind("object", CompositeIdEntity.class, CompositeIdEntity.class, null, params);

        assertTrue(bound instanceof CompositeIdEntity);

		CompositeIdEntity entity = (CompositeIdEntity) bound;
		assertNull(entity.compositeIdForeignA);
		assertNull(entity.compositeIdForeignB);

		assertFalse(entity.isPersistent());
    }

    @Test
    public void testBinderSimple() {
    	CompositeIdForeignA a = new CompositeIdForeignA();
    	a.save();
    	CompositeIdForeignB b = new CompositeIdForeignB();
    	b.save();
    	CompositeIdEntity e = new CompositeIdEntity();
    	e.compositeIdForeignA = a;
    	e.compositeIdForeignB = b;
    	e.save();

    	Map<String, String[]> params = new HashMap<String, String[]>();
    	params.put("object.id", new String[]{a.id.toString()});
		Object bound = Binder.bind("object", CompositeIdForeignA.class, CompositeIdForeignA.class, null, params);

		// they have to be the same object
		assertTrue(a == bound);
		assertEquals(a, bound);
    }
    /*
     * a.id = a1
     * a.a2Bs.compositeIdForeignA.id = a1
     * a.a2Bs.compositeIdForeignB.id = b1
     * a.a2Bs.compositeIdForeignA.id = a1
     * a.a2Bs.compositeIdForeignB.id = b2
     */
}

