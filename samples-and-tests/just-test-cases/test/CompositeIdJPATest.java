import models.CompositeIdEntity;
import models.CompositeIdForeignA;
import models.CompositeIdForeignB;
import models.CompositeIdPk;

import org.junit.Before;
import org.junit.Test;

import play.db.jpa.JPA;
import play.db.jpa.JPABase;
import play.test.Fixtures;
import play.test.UnitTest;

public class CompositeIdJPATest extends UnitTest {
    
    @Before
    public void setup() {
        Fixtures.deleteAll();
        Fixtures.load("compositeIds.yml");
    }

    @Test
    public void testImport() {
        assertEquals(2, CompositeIdForeignA.count());
        assertEquals(4, CompositeIdForeignB.count());
        assertEquals(4, CompositeIdEntity.count());
        for(CompositeIdForeignA a : CompositeIdForeignA.<CompositeIdForeignA>findAll()){
        	assertEquals(2, a.a2Bs.size());
        }
        for(CompositeIdForeignB b : CompositeIdForeignB.<CompositeIdForeignB>findAll()){
        	assertEquals(1, b.a2Bs.size());
        }
    }
    
    @Test
    public void testAllAndDelete(){
    	assertEquals(4, CompositeIdEntity.all().fetch().size());
    	CompositeIdEntity.deleteAll();
    	assertEquals(0, CompositeIdEntity.all().fetch().size());
    }

    @Test
    public void testFind(){
    	CompositeIdForeignA a = (CompositeIdForeignA) CompositeIdForeignA.findAll().get(0);
    	CompositeIdEntity e = a.a2Bs.iterator().next();
    	CompositeIdForeignB b = e.compositeIdForeignB;
    	
    	CompositeIdPk id = new CompositeIdPk();
    	id.setCompositeIdForeignA(a.id);
    	id.setCompositeIdForeignB(b.id);
    	
		CompositeIdEntity eDB = CompositeIdEntity.findById(id);
		assertNotNull(eDB);
		assertTrue(eDB == e);
    }
}

