import models.*;

import org.junit.Before;
import org.junit.Test;

import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.JPABase;
import play.test.Fixtures;
import play.test.UnitTest;

import java.util.List;

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
        for (CompositeIdForeignA a : CompositeIdForeignA.all().<CompositeIdForeignA>fetch()) {
            assertEquals(2, a.a2Bs.size());
        }
        for (CompositeIdForeignB b : CompositeIdForeignB.<CompositeIdForeignB>findAll()) {
            assertEquals(1, b.a2Bs.size());
        }
    }

    @Test
    public void testAllAndDelete() {
        assertEquals(4, CompositeIdEntity.all().fetch().size());
        CompositeIdEntity.deleteAll();
        assertEquals(0, CompositeIdEntity.all().fetch().size());
    }

    @Test
    public void testFind() {
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


    @Test
    public void testAllEmbeddedAndDelete() {

        List<UserCompositeId> users = UserCompositeId.findAll();

        assertEquals(2, users.size());

        UserId id = new UserId();
        id.firstName = "morten";
        id.lastName = "kjetland";
        UserCompositeId u = UserCompositeId.findById(id);
        assertEquals("morten", u.id.firstName);
        assertEquals("kjetland", u.id.lastName);
        assertEquals(25, (long)u.age);
    }

    @Test
    public void testEmbedded() {
        UserId id = new UserId();
        id.firstName = "emilie";
        id.lastName = "leroux";
        UserCompositeId user = new UserCompositeId();
        user.age = 1;
        user.id = id;
        user.save();

        id = new UserId();
        id.firstName = "nicolas";
        id.lastName = "leroux";
        UserCompositeId u = UserCompositeId.findById(id);

        assertEquals("nicolas", u.id.firstName);
        assertEquals("leroux", u.id.lastName);

    }
}

