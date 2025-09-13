import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.test.*;
import models.*;

public class CascadeTest extends UnitTest {
    
    @BeforeEach
    public void setup() {
        Fixtures.deleteAll();
        Fixtures.load("nf-data.yml");
    }

    @Test
    public void case1() {
        Face face = Face.find("byName", "Bob").first();
        face.nose.delete();
        assertEquals(0, Nose.count());
        assertEquals(1, Face.count());
    }
    
    @Test
    public void case2() {
        Face face = Face.find("byName", "Bob").first();
        face.delete();
        assertEquals(0, Nose.count());
        assertEquals(0, Face.count());
    }

}
