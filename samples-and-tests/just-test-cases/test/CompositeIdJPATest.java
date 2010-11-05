import models.CompositeIdEntity;
import models.CompositeIdForeignA;
import models.CompositeIdForeignB;

import org.junit.Before;
import org.junit.Test;

import play.db.jpa.JPA;
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
        CompositeIdForeignA a1 = CompositeIdForeignA.find("testId", "a1").first();
        CompositeIdForeignA a2 = CompositeIdForeignA.find("testId", "a2").first();
        CompositeIdForeignB b1 = CompositeIdForeignB.find("testId", "b1").first();
        CompositeIdForeignB b2 = CompositeIdForeignB.find("testId", "b2").first();
        CompositeIdForeignB b3 = CompositeIdForeignB.find("testId", "b3").first();
        CompositeIdForeignB b4 = CompositeIdForeignB.find("testId", "b4").first();
        assertNotNull(a1);
    	assertEquals(2, a1.a2Bs.size());
        for(CompositeIdForeignA a : CompositeIdForeignA.<CompositeIdForeignA>findAll()){
        	assertEquals(2, a.a2Bs.size());
        }
        for(CompositeIdForeignB b : CompositeIdForeignB.<CompositeIdForeignB>findAll()){
        	assertEquals(1, b.a2Bs.size());
        }
    }
}

