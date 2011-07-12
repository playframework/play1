package play;

import java.util.List;

import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;

import play.vfs.VirtualFile;
import static org.junit.Assert.*;

public class ConfigurationTest {

	private VirtualFile getBaseDir()
	{
		return VirtualFile.open(getClass().getResource("/play/").getPath());
	}
	@Test
	public void loadTest() throws Exception {
		Configuration conf = new Configuration(getBaseDir(), "configurationTest.conf");
		assertEquals(conf.getProperty("hello.world"), "hello, world");
	}
	
	@Test
    public void replaceKeywordTest() throws Exception {
		Configuration conf = new Configuration(getBaseDir(), "includeTest.conf");
		
		assertEquals(conf.getProperty("hello.world"), "HELLO! \"hello, world\"");
	}
	@Test
	public void profileTest() throws Exception {
		
	}
	@Test
	public void referenceLoopTest() throws Exception {
		Configuration conf = new Configuration(getBaseDir(), "loopTest1.conf");
	}
	
	@Test
	public void filterTest() throws Exception {
		Configuration conf = new Configuration(getBaseDir(), "configurationTest.conf");
		
		// 1. FILTER
		// expected:
		// - db.url
		// - db.driver
		// - db.user
		// - db.pass
		Configuration map1 = conf.group("db").fetch();
		assertEquals(map1.getProperty("db.url"), "jdbc:mysql:test");
		assertEquals(map1.getProperty("db.driver"), "test.Driver");
		assertEquals(map1.getProperty("db.user"), "sample_user");
		assertEquals(map1.getProperty("db.pass"), "mypassword");
		assertEquals(map1.size(), 4);
		assertEquals(conf.group("db").count(), 4);
		
		// 2. TREE FILTER & LABEL SPLIT
		// expected:
		// - url     <-- original config line is "db.url=...". fetch(false) method hides "db." prefix.
		Configuration map2 = conf.group("db").group("url").fetch(false);
		assertEquals(map2.getProperty("url"), "jdbc:mysql:test");
		assertEquals(map2.size(), 1);
		assertEquals(conf.group("db").group("url").count(), 1);
		
		// 3. SUB-LABEL
		// expected:
		// - db[readonly].url
		// - db[readonly].driver
		// - db[readonly].user
		// - db[readonly].pass
		Configuration map3 = conf.group("db", "readonly").fetch(false);
		assertEquals(map3.getProperty("url"), "jdbc:mysql:test");
		assertEquals(map3.getProperty("driver"), "test.Driver");
		assertEquals(map3.getProperty("user"), "sample_user");
		assertEquals(map3.getProperty("pass"), "mypassword");
		assertEquals(map3.size(), 4);
		assertEquals(conf.group("db", "readonly").count(), 4);
		
		// 4. LIST SUB-LABELS
		// expected:
		// ["readonly", "readwrite"]  <-- list "db[*].*" without main label "db.*"
		List<String> subLabels = conf.group("db").subLabels();
		assertTrue(subLabels.contains("readonly"));
		assertTrue(subLabels.contains("readwrite"));
		assertEquals(subLabels.size(), 2);
		
		
	}
}
