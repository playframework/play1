package play;

import java.io.File;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.db.DB;
import play.exceptions.DatabaseException;
import play.mvc.Http.Response;

public class DBTest extends TestSupport {

    static TestApp testApp;

    @BeforeClass
    public static void init() throws Exception {
        testApp = createApp();
        testApp.addRoute("GET   /                               Application.index");
        testApp.writeController("Application",
                "package controllers;" +
                "public class Application extends play.mvc.Controller {" +
                "   public static void index() {" +
                "       play.db.DB.getConnection();" +
                "       renderText(\"Index\");" +
                "   }" +
                "}");
        start(testApp);
    }

    @AfterClass
    public static void end() {
        stop();
    }

    @Test
    public void noDB() throws Exception {
        try {
            Response response = GET("/");
            System.out.println(response.status);
        } catch (DatabaseException e) {
            assertTrue("Unexplicit message !", e.getErrorDescription().contains("No database found"));
            return;
        }
        fail("No DB error ???");
    }

    @Test
    public void memDB() throws Exception {
        testApp.writeConf("application.name=Yop\ndb=mem");
        Play.start();
        assertNotNull(DB.datasource);
        Response response = GET("/");
        assertIsOk(response);
    }

    @Test
    public void fsDB() throws Exception {
        testApp.writeConf("application.name=Yop\ndb=fs");
        Play.start();
        assertNotNull(DB.datasource);
        Response response = GET("/");
        assertIsOk(response);
        assertTrue("Where is the DB ???", new File(Play.applicationPath, "db").exists());
    }

    @Test
    public void directJDBC() throws Exception {
        testApp.writeConf("application.name=Yop\ndb.driver=org.hsqldb.jdbcDriver\ndb.user=sa\ndb.pass=\ndb.url=jdbc:hsqldb:file:" + (new File(Play.applicationPath, "yopdb/yopdb").getAbsolutePath()));
        Play.start();
        assertNotNull(DB.datasource);
        Response response = GET("/");
        assertIsOk(response);
        assertTrue("Where is the DB ???", new File(Play.applicationPath, "yopdb").exists());
    }

    @Test
    public void wrongDriver() throws Exception {
        testApp.writeConf("application.name=Yop\ndb.driver=xxxx.Driver\ndb.user=sa\ndb.pass=\ndb.url=jdbc:yop");
        try {
            Play.start();
        } catch (DatabaseException e) {
            assertTrue("Unexplicit message !", e.getErrorDescription().contains("Driver not found") && e.getErrorDescription().contains("xxxx.Driver"));
        }
        testApp.writeConf("application.name=Yop");
        assertNull(DB.datasource);
    }

    @Test
    public void wrongURL() throws Exception {
        testApp.writeConf("application.name=Yop\ndb.driver=org.hsqldb.jdbcDriver\ndb.user=sa\ndb.pass=\ndb.url=jdbc:yop");
        try {
            Play.start();
        } catch (DatabaseException e) {
            assertTrue("Unexplicit message !", e.getErrorDescription().contains("Cannot connected to the database"));
        }
        testApp.writeConf("application.name=Yop");
        assertNull(DB.datasource);
    }

    @Test
    public void SQLError() throws Exception {
        testApp.writeConf("application.name=Yop\ndb=mem");
        Play.start();
        testApp.writeController("Application",
                "package controllers;" +
                "public class Application extends play.mvc.Controller {" +
                "   public static void index() {" +
                "       System.out.println(111111111);" +
                "       play.db.DB.execute(\"select * from users\");" +
                "       renderText(\"Index\");" +
                "   }" +
                "}");
        Play.detectChanges();
        try {
            GET("/");
        } catch (DatabaseException e) {
            System.out.println(e.getErrorDescription());
            assertTrue("Unexplicit message !", e.getErrorDescription().contains("select * from users"));
            return;
        }        
        fail("No DatabaseException ??");
    }
    
    @Test
    public void goodSQL() throws Exception {
        testApp.writeController("Application",
                "package controllers;" +
                "public class Application extends play.mvc.Controller {" +
                "   public static void index() {" +
                "       play.db.DB.execute(\"drop table users IF EXISTS;\");" +
                "       renderText(\"Index\");" +
                "   }" +
                "}");
        Play.classes.clear();
        Play.start();
        Response res = GET("/");
        assertIsOk(res);
    }
}
