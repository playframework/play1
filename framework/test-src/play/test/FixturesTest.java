package play.test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.*;

import models.ClassWithStaticFinalMap;
import play.Play;
import play.PlayBuilder;
import play.db.Model;
import play.db.Model.Property;
import play.plugins.PluginCollection;
import play.vfs.VirtualFile;


public class FixturesTest {
	
	public static List<MockModel> store;
    
    public static class MockModel implements Model {
    	public Integer id;
    	
        @Override
        public void _save() { this.id = store.size(); store.add(this); }
        @Override
        public void _delete() { store.remove(this); }
        @Override
        public Object _key() { return Model.Manager.factoryFor(this.getClass()).keyValue(this); }
    }
    
    public static Model.Factory mockModelFactory = new Model.Factory() {
        @Override
        public String keyName() { return null; }
        @Override
        public Class<?> keyType() { return null; }
        @Override
        public Object keyValue(play.db.Model m) { return ((MockModel)m).id; }
        @Override
        public play.db.Model findById(Object id) { return null; }
        @Override
        public List<play.db.Model> fetch(int offset, int length, String orderBy, String orderDirection,
        	    List<String> properties, String keywords, String where) { return null; }
        @Override
        public long count(List<String> properties, String keywords, String where) { return 0L; }
        @Override
        public void deleteAll() { /* Do nothing */ }
        @Override
        public List<Property> listProperties() { return null; }
    };
    
    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        new PlayBuilder().build();
        
        String className = FixturesTest.class.getSimpleName() + ".class";
        URL url = FixturesTest.class.getResource(className);
        File file = Paths.get(url.toURI()).toFile().getParentFile();
        
        Play.applicationPath = file;
        VirtualFile appRoot = VirtualFile.open(file);
        
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

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
    }

    @BeforeEach
    public void setUp() throws Exception {
    	// Initialise the model store.
    	store = new LinkedList<>();
    }

    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test 
    public void testModelClassStaticFinalMapField()  {
        // Fixtures should not attempt to set a static final field
        // on a Model object otherwise an exception would occur.
        Fixtures.loadModels(false, "testModelClassStaticFinalMapField.yml");
        
        // Ensure the model was loaded correctly.
        assertEquals(1, store.size());
        MockModel model = store.get(0);
        assertNotNull(model);
        assertInstanceOf(ClassWithStaticFinalMap.class, model);
        assertEquals("hello", ((ClassWithStaticFinalMap)model).name);
    }
}
