package play;

/**
 *
 */

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test the Logger class. At the moment only a few methods.
 * 
 * @author niels
 *
 */
public class LoggerTest {

    private static final String APPLICATION_LOG_PATH_PROPERTYNAME = "application.log.path";

    private static Properties playConfig;

    private static File applicationPath;

    private static String id;

    private static org.apache.logging.log4j.Logger log4j;

    /**
     * Safes the original configuration and log.
     * 
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
     * Restore the original configuration and log.
     * 
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        Play.configuration = playConfig;
        Play.applicationPath = applicationPath;
        Play.id = id;
        Logger.log4j = log4j;
        if (Play.id != null && Play.configuration != null) {
            Logger.init();
        }
    }

    @Before
    public void setUp() throws Exception {
        new PlayBuilder().build();    
        String className = LoggerTest.class.getSimpleName() + ".class";
        URL url = LoggerTest.class.getResource(className);
        File file = Paths.get(url.toURI()).toFile().getParentFile();
        Play.applicationPath = file;
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link play.Logger#init()}.
     */
    @Test
    public void testInitWithPropertiesForDefaultRoot() {
        // given
        Play.configuration.put(APPLICATION_LOG_PATH_PROPERTYNAME,
                new File(Play.applicationPath, "testlog4j.properties").getAbsolutePath());

        Logger.log4j = null;
        init();
        // when
        org.apache.logging.log4j.Logger log4jLoggerDefault = LogManager.getLogger("testAPP");
        // then
        assertEquals(Level.DEBUG, log4jLoggerDefault.getLevel());
    }

    /**
     * Test method for {@link play.Logger#init()}.
     */
    @Test
    public void testInitWithPropertiesForCustom() {
        // given
        Play.configuration.put(APPLICATION_LOG_PATH_PROPERTYNAME,
                new File(Play.applicationPath, "testlog4j.properties").getAbsolutePath());
        Logger.log4j = null;
        init();
        // when
        org.apache.logging.log4j.Logger log4jLoggerCustom = LogManager.getLogger("logtest.properties");
        // then
        assertEquals(Level.WARN, log4jLoggerCustom.getLevel());
    }

    /**
     * Test method for {@link play.Logger#init()}.
     */
    @Test
    public void testInitWithPropertiesForPlay() {
        // given
        Play.configuration.put(APPLICATION_LOG_PATH_PROPERTYNAME,
                new File(Play.applicationPath, "testlog4j.properties").getAbsolutePath());
        Logger.log4j = null;
        init();
        // when
        org.apache.logging.log4j.Logger log4jLogger = LogManager.getLogger("play");
        org.apache.logging.log4j.Logger log4jLoggerPlay = Logger.log4j;
        // then
        assertEquals(Level.INFO, log4jLogger.getLevel());
        assertEquals(Level.INFO, log4jLoggerPlay.getLevel());
    }

    /**
     * Test method for {@link play.Logger#init()}.
     */
    @Test
    public void testInitWithXMLForCustom() {
        // given
        Play.configuration.put(APPLICATION_LOG_PATH_PROPERTYNAME,
                new File(Play.applicationPath, "testlog4j.xml").getAbsolutePath());
        Logger.log4j = null;
        init();
        // when
        org.apache.logging.log4j.Logger log4jLogger = LogManager.getLogger("logtest.xml");
        // then
        assertEquals(Level.TRACE, log4jLogger.getLevel());
    }

    /**
     * Test method for {@link play.Logger#init()}.
     */
    @Test
    public void testInitWithXMLForPlay() {
        // given
        Play.configuration.put(APPLICATION_LOG_PATH_PROPERTYNAME,
                new File(Play.applicationPath, "testlog4j.xml").getAbsolutePath());
        Logger.log4j = null;
        init();
        // when
        org.apache.logging.log4j.Logger log4jLogger = Logger.log4j;
        // then
        assertEquals(Level.DEBUG, log4jLogger.getLevel());
    }

    private void init() {
        Logger.init(new Logger.LoggerInit() {
            @Override
            public URL getLog4jConf() {
                try {
                    return new File(Play.configuration.getProperty(APPLICATION_LOG_PATH_PROPERTYNAME)).toURI().toURL();
                } catch (MalformedURLException ignored) {
                }
                return super.getLog4jConf();
            }

            @Override
            public boolean access() throws URISyntaxException {
                return new File(Play.configuration.getProperty(APPLICATION_LOG_PATH_PROPERTYNAME)).isFile();
            }
        });
    }
}
