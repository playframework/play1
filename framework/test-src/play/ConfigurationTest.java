package play;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import play.exceptions.ConfigurationException;
import play.vfs.VirtualFile;

public class ConfigurationTest extends Assert {

	private VirtualFile getBaseDir()
	{
		return VirtualFile.open(getClass().getResource("/play/").getPath());
	}
	@Test
	public void loadTest() throws Exception {
		Configuration conf = new Configuration(getBaseDir(), "configurationTest.conf");
		assertEquals("hello, world", conf.getProperty("hello.world"));
		
		// keyword
		assertEquals("Keyword is \"hello, world\".", conf.getProperty("hello.keyword"));
	}
	
	@Test
	public void profileTest() throws Exception {
		
		Play.id = "test";
		Configuration conf = new Configuration(getBaseDir(), "configurationTest.conf");
		assertEquals("hello, TEST!!", conf.getProperty("hello.world"));
		
	}
	@Test
	public void referenceLoopTest() throws Exception {
		try {
			Configuration conf = new Configuration(getBaseDir(), "includeLoopTest.conf");
			fail();
			
		} catch (ConfigurationException e) {
			
		}
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
		Configuration map1 = conf.group("db");
		assertEquals("jdbc:mysql:test", map1.getProperty("db.url"));
		assertEquals("test.Driver", map1.getProperty("db.driver"));
		assertEquals("sample_user", map1.getProperty("db.user"));
		assertEquals("mypassword", map1.getProperty("db.pass"));
		assertEquals(4, map1.size());
		assertEquals(4, conf.group("db").size());
		
		// 2. TREE FILTER & LABEL SPLIT
		// expected:
		// - url     <-- original config line is "db.url=...". fetch(false) method hides "db." prefix.
		Configuration map2 = conf.group("db").group("url").hidePrefix();
		assertEquals("jdbc:mysql:test", map2.getProperty());
		assertEquals(1, map2.size());
		assertEquals(1, conf.group("db").group("url").size());
		map2 = conf.group("dev.db");
		assertEquals(4, map2.size());
		assertEquals("dev.db", map2.prefix);
		
		// 3. SUB-LABEL
		// expected:
		// - db[readonly].url
		// - db[readonly].driver
		// - db[readonly].user
		// - db[readonly].pass
		Configuration map3 = conf.group("db", "readonly").hidePrefix();
		assertEquals("jdbc:mysql:test:ReadOnly", map3.getProperty("url"));
		assertEquals("test.ReadOnlyDriver", map3.getProperty("driver"));
		assertEquals("readonly_user", map3.getProperty("user"));
		assertEquals("myreadonlypassword", map3.getProperty("pass"));
		assertEquals(4, map3.size());
		assertEquals(4, conf.group("db", "readonly").size());
		
		// 4. Collect SubLabel
		List<String> sublabels = conf.group("db").getSubLables();
		assertEquals(2, sublabels.size());
		
	}
}
