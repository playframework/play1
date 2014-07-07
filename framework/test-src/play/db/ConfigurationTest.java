package play.db;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.*;

import play.Play;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConfigurationTest {

    @Test
    public void dbNameResolverTest() {
        Play.configuration = new Properties();
        Play.configuration.put("db", "mysql:user:pwd@database_name");
        Set<String> dbNames = Configuration.getDbNames();
        assertEquals(1, dbNames.size());
        Iterator<String> it = dbNames.iterator();
        assertEquals("default", it.next());

        Play.configuration.put("db.test", "mysql:user:pwd@database_name2");
        dbNames = Configuration.getDbNames();
        assertEquals(2, dbNames.size());
        it = dbNames.iterator();
        assertEquals("default", it.next());
        assertEquals("test", it.next());
        
        Configuration configuration = new Configuration("default");
        assertEquals("mysql:user:pwd@database_name", configuration.getProperty("db"));
        
        configuration = new Configuration("test");
        assertEquals("mysql:user:pwd@database_name2", configuration.getProperty("db"));
    }

    @Test
    public void dbNameResolverMultiDbURLTest1() {
        Play.configuration = new Properties();
        Play.configuration.put("db.url", "jdbc:postgresql://localhost/database_name");
        Play.configuration.put("db.driver", "org.postgresql.Driver");
        Play.configuration.put("db.user", "user");
        Play.configuration.put("db.pass", "pass");

        Set<String> dbNames = Configuration.getDbNames();
        assertEquals(1, dbNames.size());
        Iterator<String> it = dbNames.iterator();
        assertEquals("default", it.next());

        Play.configuration.put("db.test.url", "jdbc:postgresql://localhost/database_name2");
        Play.configuration.put("db.test.driver", "org.postgresql.Driver");
        Play.configuration.put("db.test.user", "user2");
        Play.configuration.put("db.test.pass", "pass2");
        dbNames = Configuration.getDbNames();
        assertEquals(2, dbNames.size());
        it = dbNames.iterator();
        assertEquals("default", it.next());
        assertEquals("test", it.next());
        
        Configuration configuration = new Configuration("default");
        assertEquals("jdbc:postgresql://localhost/database_name", configuration.getProperty("db.url"));
        assertEquals("org.postgresql.Driver", configuration.getProperty("db.driver"));
        assertEquals("user", configuration.getProperty("db.user"));
        assertEquals("pass", configuration.getProperty("db.pass"));
        
        configuration = new Configuration("test");
        assertEquals("jdbc:postgresql://localhost/database_name2", configuration.getProperty("db.url"));
        assertEquals("org.postgresql.Driver", configuration.getProperty("db.driver"));
        assertEquals("user2", configuration.getProperty("db.user"));
        assertEquals("pass2", configuration.getProperty("db.pass"));
    }

    @Test
    public void dbNameResolverMySQLWithPoolTest() {
        Play.configuration = new Properties();
        Play.configuration.put("db.url", "jdbc:mysql://127.0.0.1/testPlay");
        Play.configuration.put("db.driver", "com.mysql.jdbc.Driver");
        Play.configuration.put("db.user", "root");
        Play.configuration.put("db.pass", "");
        Play.configuration.put("db.pool.timeout", "1000");
        Play.configuration.put("db.pool.maxSize", "20");
        Play.configuration.put("db.pool.minSize", "1");
        Play.configuration.put("db.pool.maxIdleTimeExcessConnections", "60");

        Set<String> dbNames = Configuration.getDbNames();
        assertEquals(1, dbNames.size());
        Iterator<String> it = dbNames.iterator();
        assertEquals("default", it.next());
        
        Configuration configuration = new Configuration("default");
        assertEquals("jdbc:mysql://127.0.0.1/testPlay", configuration.getProperty("db.url"));
        assertEquals("com.mysql.jdbc.Driver", configuration.getProperty("db.driver"));
        assertEquals("root", configuration.getProperty("db.user"));
        assertEquals("", configuration.getProperty("db.pass"));
        assertEquals("1000", configuration.getProperty("db.pool.timeout"));
        assertEquals("20", configuration.getProperty("db.pool.maxSize"));
        assertEquals("1", configuration.getProperty("db.pool.minSize"));
        assertEquals("60", configuration.getProperty("db.pool.maxIdleTimeExcessConnections"));
    }

    @Test
    public void dbNameResolverMySQLWithPoolAndDBConflictTest() {
        Play.configuration = new Properties();

        Play.configuration.put("db.url", "jdbc:mysql://127.0.0.1/testPlay");
        Play.configuration.put("db.driver", "com.mysql.jdbc.Driver");
        Play.configuration.put("db.user", "root");
        Play.configuration.put("db.pass", "");
        Play.configuration.put("db.pool.timeout", "1000");
        Play.configuration.put("db.pool.maxSize", "20");
        Play.configuration.put("db.pool.minSize", "1");
        Play.configuration.put("db.pool.maxIdleTimeExcessConnections", "60");

        Play.configuration.put("db", "mysql:user:pwd@database_name");
        Set<String> dbNames = Configuration.getDbNames();
        assertEquals(1, dbNames.size());
        Iterator<String> it = dbNames.iterator();
        assertEquals("default", it.next());
    }

    @Test
    public void dbNameResolverMySQLWithPoolAndDBTest() {
        Play.configuration = new Properties();

        Play.configuration.put("db.url", "jdbc:mysql://127.0.0.1/testPlay");
        Play.configuration.put("db.driver", "com.mysql.jdbc.Driver");
        Play.configuration.put("db.user", "root");
        Play.configuration.put("db.pass", "");
        Play.configuration.put("db.pool.timeout", "1000");
        Play.configuration.put("db.pool.maxSize", "20");
        Play.configuration.put("db.pool.minSize", "1");
        Play.configuration.put("db.pool.maxIdleTimeExcessConnections", "60");

        Play.configuration.put("db.test", "mysql:user:pwd@database_name");

        Set<String> dbNames = Configuration.getDbNames();
        assertEquals(2, dbNames.size());
        Iterator<String> it = dbNames.iterator();
        assertEquals("default", it.next());
        assertEquals("test", it.next());
        
        Configuration configuration = new Configuration("default");
        assertEquals("jdbc:mysql://127.0.0.1/testPlay", configuration.getProperty("db.url"));
        assertEquals("com.mysql.jdbc.Driver", configuration.getProperty("db.driver"));
        assertEquals("root", configuration.getProperty("db.user"));
        assertEquals("", configuration.getProperty("db.pass"));
        assertEquals("1000", configuration.getProperty("db.pool.timeout"));
        assertEquals("20", configuration.getProperty("db.pool.maxSize"));
        assertEquals("1", configuration.getProperty("db.pool.minSize"));
        assertEquals("60", configuration.getProperty("db.pool.maxIdleTimeExcessConnections"));
        
        configuration = new Configuration("test");
        assertEquals("mysql:user:pwd@database_name", configuration.getProperty("db"));
        assertNull(configuration.getProperty("db.url"));
        assertNull(configuration.getProperty("db.driver"));
        assertNull(configuration.getProperty("db.user"));
        assertNull(configuration.getProperty("db.pass"));
        assertNull(configuration.getProperty("db.pool.timeout"));
        assertNull(configuration.getProperty("db.pool.maxSize"));
        assertNull(configuration.getProperty("db.pool.minSize"));
        assertNull(configuration.getProperty("db.pool.maxIdleTimeExcessConnections"));
    }

    @Test
    public void dbNameResolverMySQLWithPoolAndDBTest2() {
        Play.configuration = new Properties();

        Play.configuration.put("db.test.url", "jdbc:mysql://127.0.0.1/testPlay");
        Play.configuration.put("db.test.driver", "com.mysql.jdbc.Driver");
        Play.configuration.put("db.test.user", "root");
        Play.configuration.put("db.test.pass", "");
        Play.configuration.put("db.test.pool.timeout", "1000");
        Play.configuration.put("db.test.pool.maxSize", "20");
        Play.configuration.put("db.test.pool.minSize", "1");
        Play.configuration.put("db.test.pool.maxIdleTimeExcessConnections", "60");

        Play.configuration.put("db", "mysql:user:pwd@database_name");

        Set<String> dbNames = Configuration.getDbNames();
        assertEquals(2, dbNames.size());
        Iterator<String> it = dbNames.iterator();
        assertEquals("default", it.next());
        assertEquals("test", it.next());
        
        Configuration configuration = new Configuration("test");
        assertNull(configuration.getProperty("db"));
        assertEquals("jdbc:mysql://127.0.0.1/testPlay", configuration.getProperty("db.url"));
        assertEquals("com.mysql.jdbc.Driver", configuration.getProperty("db.driver"));
        assertEquals("root", configuration.getProperty("db.user"));
        assertEquals("", configuration.getProperty("db.pass"));
        assertEquals("1000", configuration.getProperty("db.pool.timeout"));
        assertEquals("20", configuration.getProperty("db.pool.maxSize"));
        assertEquals("1", configuration.getProperty("db.pool.minSize"));
        assertEquals("60", configuration.getProperty("db.pool.maxIdleTimeExcessConnections"));
        
        configuration = new Configuration("default");
        assertEquals("mysql:user:pwd@database_name", configuration.getProperty("db"));
        assertNull(configuration.getProperty("db.url"));
        assertNull(configuration.getProperty("db.driver"));
        assertNull(configuration.getProperty("db.user"));
        assertNull(configuration.getProperty("db.pass"));
        assertNull(configuration.getProperty("db.pool.timeout"));
        assertNull(configuration.getProperty("db.pool.maxSize"));
        assertNull(configuration.getProperty("db.pool.minSize"));
        assertNull(configuration.getProperty("db.pool.maxIdleTimeExcessConnections"));
    }

    @Test
    public void convertToMultiDBTest() {
        Play.configuration = new Properties();

        Play.configuration.put("db.url", "jdbc:mysql://127.0.0.1/testPlay");
        Play.configuration.put("db.driver", "com.mysql.jdbc.Driver");
        Play.configuration.put("db.user", "root");
        Play.configuration.put("db.pass", "");
        Play.configuration.put("db.pool.timeout", "1000");
        Play.configuration.put("db.pool.maxSize", "20");
        Play.configuration.put("db.pool.minSize", "1");
        Play.configuration.put("db.pool.maxIdleTimeExcessConnections", "60");

        Configuration dbConfig = new Configuration("default");
        
        assertEquals("jdbc:mysql://127.0.0.1/testPlay", dbConfig.getProperty("db.url"));
        assertEquals("com.mysql.jdbc.Driver", dbConfig.getProperty("db.driver"));
        assertEquals("root",dbConfig.getProperty("db.user"));
        assertEquals("", dbConfig.getProperty("db.pass"));
        assertEquals("1000", dbConfig.getProperty("db.pool.timeout"));
        assertEquals("20", dbConfig.getProperty("db.pool.maxSize"));
        assertEquals("1", dbConfig.getProperty("db.pool.minSize"));
        assertEquals("60", dbConfig.getProperty("db.pool.maxIdleTimeExcessConnections"));

        Set<String> dbNames = Configuration.getDbNames();
        assertEquals(1, dbNames.size());
        Iterator<String> it = dbNames.iterator();
        assertEquals("default", it.next());
    }
    
    @Test
    public void getPropertiesForDefaultTest() {
        Play.configuration = new Properties();

        Play.configuration.put("db.url", "jdbc:mysql://127.0.0.1/testPlay");
        Play.configuration.put("db.driver", "com.mysql.jdbc.Driver");
        Play.configuration.put("db.user", "root");
        Play.configuration.put("db.pass", "");
        Play.configuration.put("db.pool.timeout", "1000");
        Play.configuration.put("db.pool.maxSize", "20");
        Play.configuration.put("db.pool.minSize", "1");
        Play.configuration.put("db.pool.maxIdleTimeExcessConnections", "60");
        
        Play.configuration.put("hibernate.ejb.event.post-insert", "postInsert");
        Play.configuration.put("hibernate.ejb.event.post-update", "postUpdate");

        Configuration dbConfig = new Configuration("default");
        Map<String, String> properties = dbConfig.getProperties();
        
        assertEquals("jdbc:mysql://127.0.0.1/testPlay", properties.get("db.url"));
        assertEquals("com.mysql.jdbc.Driver", properties.get("db.driver"));
        assertEquals("root",properties.get("db.user"));
        assertEquals("", properties.get("db.pass"));
        assertEquals("1000", properties.get("db.pool.timeout"));
        assertEquals("20", properties.get("db.pool.maxSize"));
        assertEquals("1", properties.get("db.pool.minSize"));
        assertEquals("60", properties.get("db.pool.maxIdleTimeExcessConnections"));
        
        assertEquals("postInsert", properties.get("hibernate.ejb.event.post-insert"));
        assertEquals("postUpdate", properties.get("hibernate.ejb.event.post-update"));
        
        assertEquals(Play.configuration.size(), properties.size());
    }
    
    @Test
    public void getPropertiesForDBTest() {
        Play.configuration = new Properties();

        Play.configuration.put("db.test.url", "jdbc:mysql://127.0.0.1/testPlay");
        Play.configuration.put("db.test.driver", "com.mysql.jdbc.Driver");
        Play.configuration.put("db.test.user", "root");
        Play.configuration.put("db.test.pass", "");
        Play.configuration.put("db.test.pool.timeout", "1000");
        Play.configuration.put("db.test.pool.maxSize", "20");
        Play.configuration.put("db.test.pool.minSize", "1");
        Play.configuration.put("db.test.pool.maxIdleTimeExcessConnections", "60");
        
        Play.configuration.put("hibernate.test.ejb.event.post-insert", "postInsert");
        Play.configuration.put("hibernate.test.ejb.event.post-update", "postUpdate");

        Configuration dbConfig = new Configuration("test");
        Map<String, String> properties = dbConfig.getProperties();
        
        assertEquals("jdbc:mysql://127.0.0.1/testPlay", properties.get("db.url"));
        assertEquals("com.mysql.jdbc.Driver", properties.get("db.driver"));
        assertEquals("root",properties.get("db.user"));
        assertEquals("", properties.get("db.pass"));
        assertEquals("1000", properties.get("db.pool.timeout"));
        assertEquals("20", properties.get("db.pool.maxSize"));
        assertEquals("1", properties.get("db.pool.minSize"));
        assertEquals("60", properties.get("db.pool.maxIdleTimeExcessConnections"));
        
        assertEquals("postInsert", properties.get("hibernate.ejb.event.post-insert"));
        assertEquals("postUpdate", properties.get("hibernate.ejb.event.post-update"));
        
        assertEquals(Play.configuration.size(), properties.size());
    }
}
