package play.db;

import static org.junit.Assert.*; 

import java.sql.Connection;
import java.util.Properties;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import play.Play;

public class DbTest  {
    
    @Test
    public void testDatasource () throws Exception{
        DB.init();
        assertNotNull(DB.datasource);
        Connection c = DB.datasource.getConnection();
        assertNotNull(c);
        c.close();
    }
    
    @Test
    public void testNoReload () throws Exception{
        DB.init();
        DataSource ds = DB.datasource;
        DB.init();
        assertSame(DB.datasource, ds);
    }
    
    @Before
    public void setUp() throws Exception {
        Properties p = new Properties ();
        p.setProperty("db.driver", "org.hsqldb.jdbcDriver");
        p.setProperty("db.url", "jdbc:hsqldb:mem:aname");
        p.setProperty("db.user", "sa");
        p.setProperty("db.pass", "");
        Play.configuration=p;
    }
}
