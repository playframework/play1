package play.test;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import play.Play;
import play.PlayBuilder;
import play.vfs.VirtualFile;


public class FixturesTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    	new PlayBuilder().build();
    	
    	String className = FixturesTest.class.getSimpleName() + ".class";
    	URL url = FixturesTest.class.getResource(className);
    	File file = Paths.get(url.toURI()).toFile().getParentFile();
    	
    	Play.applicationPath = file;
    	
    	VirtualFile appRoot = VirtualFile.open(file);
    	
    	Play.roots.clear();
    	Play.roots.add(appRoot);

    	Play.javaPath.clear();
    	Play.javaPath.add(appRoot);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test 
    public void testStaticMap()  {
    	Fixtures.loadModels(false, "FixturesTest.yml");
    }
}
