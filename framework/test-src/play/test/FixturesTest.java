package play.test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import play.Play;
import play.PlayBuilder;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.db.Model;
import play.db.Model.Property;
import play.plugins.PluginCollection;
import play.vfs.VirtualFile;


public class FixturesTest {
	
	public static class MockModel implements Model {
		@Override
		public void _save() { /* Do nothing */ }
		@Override
		public void _delete() { /* Do nothing */ }
		@Override
		public Object _key() { return Model.Manager.factoryFor(this.getClass()).keyValue(this); }
	}
	
	public static Model.Factory mockModelFactory = new Model.Factory() {
		@Override
		public String keyName() { return null; }
		@Override
		public Class<?> keyType() { return null; }
		@Override
		public Object keyValue(play.db.Model m) { return Long.valueOf(1); }
		@Override
		public play.db.Model findById(Object id) { return null; }
		@Override
		public List<play.db.Model> fetch(int offset, int length, String orderBy, String orderDirection,
				List<String> properties, String keywords, String where) { return null; }
		@Override
		public Long count(List<String> properties, String keywords, String where) { return null; }
		@Override
		public void deleteAll() { /* Do nothing */ }
		@Override
		public List<Property> listProperties() { return null; }
	};
	
	private static final String CLASS_WITH_STATIC_FINAL_MAP_NAME = "models.ClassWithStaticFinalMap";
	

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    	new PlayBuilder().build();
    	
    	String className = FixturesTest.class.getSimpleName() + ".class";
    	URL url = FixturesTest.class.getResource(className);
    	File file = Paths.get(url.toURI()).toFile().getParentFile();
    	
    	Play.applicationPath = file;
    	VirtualFile appRoot = VirtualFile.open(file);
    	
    	ApplicationClass testClass = new ApplicationClass(CLASS_WITH_STATIC_FINAL_MAP_NAME, appRoot.child("ClassWithStaticFinalMap.java.xml"));
    	
    	Play.classes = new ApplicationClasses() {
    		public ApplicationClass getApplicationClass(String name) {
    			return CLASS_WITH_STATIC_FINAL_MAP_NAME.equals(name) ? testClass : super.getApplicationClass(name);
    	    }
    	};
    	
    	Play.pluginCollection = new PluginCollection() {
    	    public Model.Factory modelFactory(Class<? extends play.db.Model> modelClass) {
    	    	return MockModel.class.isAssignableFrom(modelClass) ? mockModelFactory : null;
    	    }
    	};
    	
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
    public void testModelClassStaticFinalMapField()  {
    	// Fixtures should not attempt to set a static final field
    	// on a Model object otherwise an exception would occur.
    	Fixtures.loadModels(false, "testModelClassStaticFinalMapField.yml");
    }
}
