package play.db;

import java.util.*;

import org.junit.*;

import play.Play;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConfigurationTest {

    @Before
    public void setUp() {
        Play.configuration = new Properties();
    }

    @Test
    public void dbNameResolver_singleDatabase() {
        Play.configuration.put("db", "mysql:user:pwd@database_name");
        Set<String> dbNames = Configuration.getDbNames();
        assertEquals(1, dbNames.size());
        assertEquals("default", dbNames.iterator().next());
    }
    
    @Test
    public void dbNameResolver_multipleDatabases() {
        Play.configuration.put("db", "mysql:user:pwd@database_name");
        Play.configuration.put("db.test", "mysql:user:pwd@database_name2");
        List<String> dbNames = new ArrayList<>(Configuration.getDbNames());
        assertEquals(2, dbNames.size());
        assertEquals("default", dbNames.get(0));
        assertEquals("test", dbNames.get(1));
        assertEquals("mysql:user:pwd@database_name", new Configuration("default").getProperty("db"));
        assertEquals("mysql:user:pwd@database_name2", new Configuration("test").getProperty("db"));
    }

    @Test
    public void dbNameResolverMultiDbURL_singleDatabase() {
        Play.configuration.put("db.url", "jdbc:postgresql://localhost/database_name");
        Play.configuration.put("db.driver", "org.postgresql.Driver");
        Play.configuration.put("db.user", "user");
        Play.configuration.put("db.pass", "pass");

        Set<String> dbNames = Configuration.getDbNames();
        assertEquals(1, dbNames.size());
        assertEquals("default", dbNames.iterator().next());
    }

    @Test
    public void dbNameResolverMultiDbURL_multipleDatabases() {
        Play.configuration.put("db.url", "jdbc:postgresql://localhost/database_name");
        Play.configuration.put("db.driver", "org.postgresql.Driver");
        Play.configuration.put("db.user", "user");
        Play.configuration.put("db.pass", "pass");
        Play.configuration.put("db.test.url", "jdbc:postgresql://localhost/database_name2");
        Play.configuration.put("db.test.driver", "org.postgresql.Driver");
        Play.configuration.put("db.test.user", "user2");
        Play.configuration.put("db.test.pass", "pass2");
        
        List<String> dbNames = new ArrayList<>(Configuration.getDbNames());
        assertEquals(2, dbNames.size());
        assertEquals("default", dbNames.get(0));
        assertEquals("test", dbNames.get(1));
        
        Configuration configuration1 = new Configuration("default");
        assertEquals("jdbc:postgresql://localhost/database_name", configuration1.getProperty("db.url"));
        assertEquals("org.postgresql.Driver", configuration1.getProperty("db.driver"));
        assertEquals("user", configuration1.getProperty("db.user"));
        assertEquals("pass", configuration1.getProperty("db.pass"));

        Configuration configuration2 = new Configuration("test");
        assertEquals("jdbc:postgresql://localhost/database_name2", configuration2.getProperty("db.url"));
        assertEquals("org.postgresql.Driver", configuration2.getProperty("db.driver"));
        assertEquals("user2", configuration2.getProperty("db.user"));
        assertEquals("pass2", configuration2.getProperty("db.pass"));
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
        assertEquals("default", dbNames.iterator().next());
        
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
        assertEquals("default", dbNames.iterator().next());
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
        //db
        Play.configuration.put("db.url", "jdbc:mysql://127.0.0.1/testPlay");
        Play.configuration.put("db.driver", "com.mysql.jdbc.Driver");
        Play.configuration.put("db.user", "root");
        Play.configuration.put("db.pass", "");
        Play.configuration.put("db.pool.timeout", "1000");
        Play.configuration.put("db.pool.maxSize", "20");
        Play.configuration.put("db.pool.minSize", "1");
        Play.configuration.put("db.pool.maxIdleTimeExcessConnections", "60");
        //javax.persistence
        Play.configuration.put("javax.persistence.lock.scope", "EXTENDED");
        Play.configuration.put("javax.persistence.lock.timeout", "1000");
        //jpa
        Play.configuration.put("jpa.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        Play.configuration.put("jpa.ddl", "update");
        Play.configuration.put("jpa.debugSQL", "true");
        //hibernate
        Play.configuration.put("hibernate.ejb.event.post-insert", "postInsert");
        Play.configuration.put("hibernate.ejb.event.post-update", "postUpdate");
        //org.hibernate
        Play.configuration.put("org.hibernate.flushMode", "AUTO");

        Configuration dbConfig = new Configuration("default");
        Map<String, String> properties = dbConfig.getProperties();

        //db
        assertEquals("jdbc:mysql://127.0.0.1/testPlay", properties.get("db.url"));
        assertEquals("com.mysql.jdbc.Driver", properties.get("db.driver"));
        assertEquals("root",properties.get("db.user"));
        assertEquals("", properties.get("db.pass"));
        assertEquals("1000", properties.get("db.pool.timeout"));
        assertEquals("20", properties.get("db.pool.maxSize"));
        assertEquals("1", properties.get("db.pool.minSize"));
        assertEquals("60", properties.get("db.pool.maxIdleTimeExcessConnections"));
        //javax.persistence
        assertEquals("EXTENDED", properties.get("javax.persistence.lock.scope"));
        assertEquals("1000", properties.get("javax.persistence.lock.timeout"));
        //jpa
        assertEquals("org.hibernate.dialect.PostgreSQLDialect", properties.get("jpa.dialect"));
        assertEquals("update", properties.get("jpa.ddl"));
        assertEquals("true", properties.get("jpa.debugSQL"));
        //hibernate
        assertEquals("postInsert", properties.get("hibernate.ejb.event.post-insert"));
        assertEquals("postUpdate", properties.get("hibernate.ejb.event.post-update"));
        //org.hibernate
        assertEquals("AUTO", properties.get("org.hibernate.flushMode"));

        assertEquals(Play.configuration.size(), properties.size());
    }
    
    @Test
    public void getPropertiesForDBTest() {
        //db
        Play.configuration.put("db.test.url", "jdbc:mysql://127.0.0.1/testPlay");
        Play.configuration.put("db.test.driver", "com.mysql.jdbc.Driver");
        Play.configuration.put("db.test.user", "root");
        Play.configuration.put("db.test.pass", "");
        Play.configuration.put("db.test.pool.timeout", "1000");
        Play.configuration.put("db.test.pool.maxSize", "20");
        Play.configuration.put("db.test.pool.minSize", "1");
        Play.configuration.put("db.test.pool.maxIdleTimeExcessConnections", "60");
        //javax.persistence
        Play.configuration.put("javax.persistence.test.lock.scope", "EXTENDED");
        Play.configuration.put("javax.persistence.test.lock.timeout", "1000");
        //jpa
        Play.configuration.put("jpa.test.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        Play.configuration.put("jpa.test.ddl", "update");
        Play.configuration.put("jpa.test.debugSQL", "true");
        //hibernate
        Play.configuration.put("hibernate.test.ejb.event.post-insert", "postInsert");
        Play.configuration.put("hibernate.test.ejb.event.post-update", "postUpdate");
        //org.hibernate
        Play.configuration.put("org.hibernate.test.flushMode", "AUTO");

        Configuration dbConfig = new Configuration("test");
        Map<String, String> properties = dbConfig.getProperties();

        //db
        assertEquals("jdbc:mysql://127.0.0.1/testPlay", properties.get("db.url"));
        assertEquals("com.mysql.jdbc.Driver", properties.get("db.driver"));
        assertEquals("root",properties.get("db.user"));
        assertEquals("", properties.get("db.pass"));
        assertEquals("1000", properties.get("db.pool.timeout"));
        assertEquals("20", properties.get("db.pool.maxSize"));
        assertEquals("1", properties.get("db.pool.minSize"));
        assertEquals("60", properties.get("db.pool.maxIdleTimeExcessConnections"));
        //javax.persistence
        assertEquals("EXTENDED", properties.get("javax.persistence.lock.scope"));
        assertEquals("1000", properties.get("javax.persistence.lock.timeout"));
        //jpa
        assertEquals("org.hibernate.dialect.PostgreSQLDialect", properties.get("jpa.dialect"));
        assertEquals("update", properties.get("jpa.ddl"));
        assertEquals("true", properties.get("jpa.debugSQL"));
        //hibernate
        assertEquals("postInsert", properties.get("hibernate.ejb.event.post-insert"));
        assertEquals("postUpdate", properties.get("hibernate.ejb.event.post-update"));
        //org.hibernate
        assertEquals("AUTO", properties.get("org.hibernate.flushMode"));

        assertEquals(Play.configuration.size(), properties.size());
    }

    @Test
    public void generatesConfigurationPropertyNameBasedOnDatabaseName() {
        Configuration configuration = new Configuration("another");
        assertEquals("db.another", configuration.generateKey("db"));
        assertEquals("db.another.driver", configuration.generateKey("db.driver"));
        assertEquals("db.another.url", configuration.generateKey("db.url"));
        assertEquals("another-property", configuration.generateKey("another-property"));
    }

    @Test
    public void usesDefaultConfigurationPropertyNameForDefaultDatabase() {
        Configuration configuration = new Configuration("default");
        assertEquals("db.default", configuration.generateKey("db"));
        assertEquals("db.default.driver", configuration.generateKey("db.driver"));
        assertEquals("db.default.url", configuration.generateKey("db.url"));
        assertEquals("another-property", configuration.generateKey("another-property"));
    }

    @Test
    public void putPropertyToDefaultConfiguration() {
        Configuration configuration = new Configuration("default");
        configuration.put("db.driver", "org.h2.Driver");
        assertEquals("org.h2.Driver", configuration.getProperty("db.driver"));
        assertEquals("org.h2.Driver", Play.configuration.getProperty("db.default.driver"));
    }
    
    @Test
    public void putPropertyToCustomConfiguration() {
        Configuration configuration = new Configuration("custom");
        
        configuration.put("db.driver", "com.oracle.OracleDriver");
        assertEquals("com.oracle.OracleDriver", configuration.getProperty("db.driver"));
    }

    @Test
    public void putPropertyToMultipleCustomConfigurations() {
        Configuration configuration = new Configuration("default");
        Configuration configuration1 = new Configuration("db1");
        Configuration configuration2 = new Configuration("db2");
        
        configuration.put("db.driver", "com.oracle.OracleDriver");
        configuration1.put("db.driver", "org.h2.Driver");
        configuration2.put("db.driver", "com.mysql.Driver");
        
        assertEquals("com.oracle.OracleDriver", configuration.getProperty("db.driver"));
        assertEquals("org.h2.Driver", configuration1.getProperty("db.driver"));
        assertEquals("com.mysql.Driver", configuration2.getProperty("db.driver"));
        
        assertEquals("com.oracle.OracleDriver", Play.configuration.getProperty("db.default.driver"));
        assertEquals("org.h2.Driver", Play.configuration.getProperty("db.db1.driver"));
        assertEquals("com.mysql.Driver", Play.configuration.getProperty("db.db2.driver"));
    }

    @Test
    public void getPropertyFromDefaultConfiguration() {
        Play.configuration.setProperty("db.default.url", "jdbc:h2:mem:play;MODE=MSSQLServer;LOCK_MODE=0");
        Configuration configuration = new Configuration("default");
        assertEquals("jdbc:h2:mem:play;MODE=MSSQLServer;LOCK_MODE=0", configuration.getProperty("db.url"));
    }
}
