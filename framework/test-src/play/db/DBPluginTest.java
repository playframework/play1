package play.db;


import org.junit.Test;

import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;

public class DBPluginTest {

    @Test
    public void verifyDetectedExtraDBConfigs() {
        Properties props = new Properties();
        props.put("db", "a");
        props.put("db.a", "a");
        props.put("db_a", "a");
        props.put("db_b.a", "a");
        Set<String> names = new DBPlugin().detectedExtraDBConfigs( props );
        assertTrue( names.size()==2);
        assertTrue( names.contains("a"));
        assertTrue( names.contains("b"));
    }
}
