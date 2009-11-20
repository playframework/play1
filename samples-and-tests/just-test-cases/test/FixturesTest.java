import org.junit.*;

import java.util.*;
import play.test.*;
import models.*;
import models.vendor.Vendor;
import models.vendor.tag.AreaTag;
import models.vendor.tag.FunctionTag;
import models.vendor.tag.Tag;

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
    }

}
