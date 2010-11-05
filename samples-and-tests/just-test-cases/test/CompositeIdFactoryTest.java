import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.CompositeIdEntity;
import models.CompositeIdForeignA;
import models.CompositeIdForeignB;
import models.CompositeIdPk;

import org.junit.Before;
import org.junit.Test;

import play.db.Model;
import play.db.Model.Factory;
import play.db.Model.Property;
import play.db.jpa.JPA;
import play.test.Fixtures;
import play.test.UnitTest;

public class CompositeIdFactoryTest extends UnitTest {
    
    @Before
    public void setup() {
    	Fixtures.delete(CompositeIdEntity.class, CompositeIdForeignA.class, CompositeIdForeignB.class);
    }

    @Test
    public void testFactoryKeys() {
    	Factory factory = Model.Manager.factoryFor(CompositeIdEntity.class);
    	assertNotNull(factory);
    	List<Property> keys = factory.listKeys();
    	assertNotNull(keys);
    	assertEquals(2, keys.size());
    	Property a = findKey(keys, "compositeIdForeignA");
    	assertTrue(a.isKey);
    	assertTrue(a.isRelation);
    	Property b = findKey(keys, "compositeIdForeignB");
    	assertTrue(b.isKey);
    	assertTrue(b.isRelation);
    }

    @Test
    public void testFactoryKeyType() {
    	Factory factory = Model.Manager.factoryFor(CompositeIdEntity.class);
    	assertNotNull(factory);
    	assertEquals(CompositeIdPk.class, factory.keyType());
    }

    @Test
    public void testMakeId() {
    	Factory factory = Model.Manager.factoryFor(CompositeIdEntity.class);
    	assertNotNull(factory);
    	Map<String, Object> ids = new HashMap<String, Object>();
    	CompositeIdForeignA a = new CompositeIdForeignA();
    	a.id = 1l;
    	CompositeIdForeignB b = new CompositeIdForeignB();
    	b.id = 2l;
    	ids.put("compositeIdForeignA", a);
    	ids.put("compositeIdForeignB", b);
		// let's make a key
    	Object id = factory.makeKey(ids);
    	assertNotNull(id);
    	assertTrue(id instanceof CompositeIdPk);
    	CompositeIdPk pk = (CompositeIdPk) id;
    	assertEquals(Long.valueOf(1), pk.getCompositeIdForeignA());
    	assertEquals(Long.valueOf(2), pk.getCompositeIdForeignB());
    }

    @Test
    public void testGetId() {
    	Factory factory = Model.Manager.factoryFor(CompositeIdEntity.class);
    	assertNotNull(factory);
    	CompositeIdForeignA a = new CompositeIdForeignA();
    	a.save();
    	CompositeIdForeignB b = new CompositeIdForeignB();
    	b.save();
    	CompositeIdEntity e = new CompositeIdEntity();
    	e.compositeIdForeignA = a;
    	e.compositeIdForeignB = b;
    	e.save();
    	
		// let's get its key
    	Object id = factory.keyValue(e);
    	assertNotNull(id);
    	assertTrue(id instanceof CompositeIdPk);
    	CompositeIdPk pk = (CompositeIdPk) id;
    	assertEquals(a.id, pk.getCompositeIdForeignA());
    	assertEquals(b.id, pk.getCompositeIdForeignB());
    }

    @Test
    public void testBindById() {
    	Factory factory = Model.Manager.factoryFor(CompositeIdEntity.class);
    	assertNotNull(factory);
    	CompositeIdForeignA a = new CompositeIdForeignA();
    	a.save();
    	CompositeIdForeignB b = new CompositeIdForeignB();
    	b.save();
    	CompositeIdEntity e = new CompositeIdEntity();
    	e.compositeIdForeignA = a;
    	e.compositeIdForeignB = b;
    	e.save();
    	
		// let's get its key
    	Object id = factory.keyValue(e);
    	Model eDB = factory.findById(id);
    	assertEquals(e, eDB);
    }

	private Property findKey(List<Property> keys, String name) {
		for(Property p : keys){
			if(p.name.equals(name))
				return p;
		}
		fail("Could not find key property " + name);
		// never reached
		return null;
	}
}

