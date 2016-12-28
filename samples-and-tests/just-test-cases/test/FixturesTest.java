import java.text.ParseException;
import java.text.SimpleDateFormat;

import models.Bloc;
import models.vendor.Vendor;
import models.vendor.tag.AreaTag;
import models.vendor.tag.FunctionTag;
import models.vendor.tag.Tag;
import models.*;

import org.junit.Before;
import org.junit.Test;

import play.test.Fixtures;
import play.test.UnitTest;

public class FixturesTest extends UnitTest {

    @Before
    public void setup() {
        Fixtures.deleteAll();
    }

    @Test
    public void loadData() {
        Fixtures.load("vendor-data.yml");
        assertEquals(2, Vendor.findAll().size());
        assertEquals(4, Tag.findAll().size());
        assertEquals(2, AreaTag.findAll().size());
        assertEquals(2, FunctionTag.findAll().size());

        assertEquals(2, Vendor.find(
                "SELECT DISTINCT v.id " +
                "FROM Vendor v " +
                "JOIN v.tags as t " +
                "WHERE t.label IN ('China', 'Wedding') " +
                "GROUP BY v.id HAVING count(t.id) = 2 ").fetch().size());

        assertEquals(1, Bloc.count());

        Bloc b = Bloc.<Bloc>findAll().get(0);
        assertEquals("Yop", b.name);
        assertEquals(2, b.criterias.size());
        assertEquals("value1", b.criterias.get("key1"));
        assertEquals("value2", b.criterias.get("key2"));

        try {
            assertEquals(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse("2001/11/23 21:03:17"), b.created);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }
    
    @Test
    public void checkEmptyReferences() {
        Fixtures.load("refs.yml");        
        assertEquals(1, Referenced.count());
        assertEquals(2, Base.count());
        
        Base b1 = Base.find("byName", "Base1").first();
        Referenced r = Referenced.all().first();
        assertNotNull(b1);
        assertNotNull(b1.ref);
        assertEquals(r, b1.ref);
        Base b2 = Base.find("byName", "Base2").first();
        assertNotNull(b2);
        assertNull(b2.ref);
    }

	@Test
    public void withGenericModel() {
		Fixtures.load("pc.yml");
		Parent parent = Parent.all().first();
		assertNotNull(parent);
		assertNotNull(parent.children);
		assertFalse(parent.children.isEmpty());
	}

    @Test
    public void manyToMany() {
        Fixtures.load("manyToMany.yml");
        Farmer fred = Farmer.find("name = ?", "Fred").first();
        assertEquals(2, fred.fruits.size());

        Fruit pear = Fruit.find("name = ?", "Pear").first();
        assertEquals(2, pear.farmers.size());
    }

    @Test
    public void oneToMany() {
        Fixtures.load("oneToMany.yml");
        Municipality ba = Municipality.find("name = ?", "Buenos Aires").first();
        assertEquals(2, ba.neighborhoods.size());

        Neighborhood almagro = Neighborhood.find("name = ?", "Almagro").first();
        assertNotNull(almagro.municipality);
        assertEquals("Buenos Aires", almagro.municipality.name);
    }

    @Test
    public void manyToOne() {
        Fixtures.load("manyToOne.yml");
        Municipality ba = Municipality.find("name = ?", "Buenos Aires").first();
        assertEquals(2, ba.neighborhoods.size());

        Neighborhood almagro = Neighborhood.find("name = ?", "Almagro").first();
        assertNotNull(almagro.municipality);
        assertEquals("Buenos Aires", almagro.municipality.name);
    }
}
