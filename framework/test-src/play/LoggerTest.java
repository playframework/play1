package play;
/**
 * 
 */


import java.io.File;
import java.util.Properties;



import org.apache.log4j.Level;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Test the Logger class. At the moment only a few methods.
 * @author niels
 *
 */
public class LoggerTest {

    private static final String APPLICATION_LOG_PATH_PROPERTYNAME = "application.log.path";
    
//    private static String applicationLogPath;
    
    private static Properties playConfig;
    
    private static File applicationPath;
    
    private static String id;
    
    private static org.apache.log4j.Logger log4j;
    
    /**
     * Safes the original configuration and log.
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        playConfig = Play.configuration;
        applicationPath = Play.applicationPath;
        id = Play.id;
        log4j = Logger.log4j;        
    }

    /**
     * Restore  the original configuration and log.
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        Play.configuration = playConfig;
        Play.applicationPath = applicationPath;
        Play.id = id ;
        Logger.log4j = log4j;
        if (Play.configuration != null) {
                Logger.init();
        }
    }
    
    @Before
    public void setUp() throws Exception {
        Play.configuration = new Properties();
        Play.applicationPath = new File(".");
        Play.id="test";   
    }

    @After
    public void tearDown() throws Exception {
    }


    /**
     * Test method for {@link play.Logger#init()}.
     */
    @Test
    public void testInitWithProperties() {        
        Play.configuration.put(APPLICATION_LOG_PATH_PROPERTYNAME, "/play/testlog4j.properties");
        Logger.log4j=null;
        Logger.init();
        org.apache.log4j.Logger log4jLogger = org.apache.log4j.Logger.getLogger("logtest.properties");
        assertEquals(Level.ERROR,  log4jLogger.getLevel());
    }

    /**
     * Test method for {@link play.Logger#init()}.
     */
    @Test
    public void testInitWithXML() {        
        Play.configuration.put(APPLICATION_LOG_PATH_PROPERTYNAME, "/play/testlog4j.xml");
        Logger.log4j=null;
        Logger.init();
        org.apache.log4j.Logger log4jLogger = org.apache.log4j.Logger.getLogger("logtest.xml");
        assertEquals(Level.ERROR,  log4jLogger.getLevel());
    }
}
