
import java.util.HashMap;
import java.util.Map;

import models.CompositeIdEntity;
import models.CompositeIdForeignA;
import models.CompositeIdForeignB;

import org.junit.Test;

import play.db.Model;
import play.test.Fixtures;
import play.test.UnitTest;

public class CompositeIdFixturesTest extends UnitTest {
    
	// to get around access restrictions
	public static class FixturesTest extends Fixtures{
	    protected static void resolveDependencies(Class<? extends Model> type, Map<String, String[]> serialized, Map<String, Object> idCache) throws Exception {
	    	Fixtures.resolveDependencies(type, serialized, idCache);
	    }
	}
	
    @Test
    public void testImport() throws Exception {
    	CompositeIdForeignA a = new CompositeIdForeignA();
    	a.save();
    	CompositeIdForeignB b = new CompositeIdForeignB();
    	b.save();
    	Map<String, Object> idCache = new HashMap<String, Object>();
    	idCache.put("models.CompositeIdForeignA-a", a.getId());
    	idCache.put("models.CompositeIdForeignB-b", b.getId());
		Map<String, String[]> serialized = new HashMap<String, String[]>();
		serialized.put("object.compositeIdForeignA", new String[]{"a"});
		serialized.put("object.compositeIdForeignB", new String[]{"b"});
		FixturesTest.resolveDependencies(CompositeIdEntity.class, serialized, idCache);
		
		assertEquals(2, serialized.size());
		assertEquals(2, idCache.size());
		
		assertFalse(serialized.containsKey("object.CompositeIdForeignA"));
		String[] serializedIds = serialized.get("object.compositeIdForeignA.id");
		assertNotNull(serializedIds);
		assertEquals(1, serializedIds.length);
		assertEquals(a.id.toString(), serializedIds[0]);

		assertFalse(serialized.containsKey("object.CompositeIdForeignB"));
		serializedIds = serialized.get("object.compositeIdForeignB.id");
		assertNotNull(serializedIds);
		assertEquals(1, serializedIds.length);
		assertEquals(b.id.toString(), serializedIds[0]);
    }
}

