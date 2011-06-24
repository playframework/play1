import org.junit.*;

import java.util.*;

import play.test.*;
import play.jobs.*;
import play.libs.*;

public class Functions extends UnitTest {

    @Test
    public void options() {

        F.Option<String> s1 = F.Option.Some("Coco");
        F.Option<Integer> s2 = F.Option.Some(4);
        F.Option<String> n = F.Option.None();

        boolean p = false;
        for(String coco : s1) {
            assertEquals("Coco", coco);
            p = true;
        }
        assertTrue("Loop missed?", p);

        p = false;
        for(Integer four : s2) {
            assertEquals((int)4, (int)four);
            p = true;
        }
        assertTrue("Loop missed?", p);

        for(String oops : n) {
            fail("Oops!");
        }

    }

    @Test
    public void either() {

        F.E3<String,Integer,Boolean> e = F.E3.<String,Integer,Boolean>_2(6);

        for(String oops : e._1) {
            fail("Oops!");
        }

        boolean p = false;
        for(Integer coco : e._2) {
            assertEquals((int)6, (int)coco);
            p = true;
        }
        assertTrue("Loop missed?", p);

        for(Boolean oops : e._3) {
            fail("Oops!");
        }        

    }

}