package play.db;

import java.sql.Connection;
import java.util.Properties;
import javax.sql.DataSource;
import junit.framework.TestCase;
import play.Play;

public class TestDb extends TestCase  {
    
    public void testDatasource () throws Exception{
        Db.init();
        assertNotNull(Db.datasource);
        Connection c = Db.datasource.getConnection();
        assertNotNull(c);
        c.close();
    }
    
    public void testNoReload () throws Exception{
        Db.init();
        DataSource ds = Db.datasource;
        Db.init();
        assertSame(Db.datasource, ds);
    }
    
    protected void setUp() throws Exception {
        Properties p = new Properties ();
        p.setProperty("db.driver", "org.hsqldb.jdbcDriver");
        p.setProperty("db.url", "jdbc:hsqldb:mem:aname");
        p.setProperty("db.user", "sa");
        p.setProperty("db.pass", "");
        Play.configuration=p;
    }
}
