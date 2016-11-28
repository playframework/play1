package play.deps;

import java.io.*;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.plugins.repository.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import play.Play;
import play.PlayBuilder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;


public class YamlParserTest {
    
    @BeforeClass
    public static void setUp(){
        // Play
        new PlayBuilder().build();
        System.setProperty("play.version", Play.version);
        System.setProperty("application.path", Play.applicationPath.getAbsolutePath());

        // We will create a "tmp/modules" directory to simulate the play dependencies
        File moduleDir = new File(Play.applicationPath, "modules");
        moduleDir.mkdirs();
        
        String[] moduleNames = {"crud", "deadbolt-1.5.4", "pdf-1.5"};
        try {
            for (String module : moduleNames) {
                File moduleFile = new File(moduleDir, module);
                moduleFile.createNewFile();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @AfterClass
    public static void cleanUp(){
        File moduleDir = new File(Play.applicationPath, "modules");
        try {
            FileUtils.deleteDirectory(moduleDir);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    
    @Test(expected = FileNotFoundException.class)
    public void fileNotFoundTest() throws Exception {    
        Set<String> modules = null;
        try {
            modules =  YamlParser.getOrderedModuleList(new File(Play.applicationPath, "fakeFile.yml"));
        } catch (Exception e) {
            assertTrue(e.getMessage().startsWith("There was a problem to find the file"));
            throw e;
        }
    }
    

    @Test
    public void retrieveModulesTest() {    
        Set<String> modules = null;
        try {
            modules =  YamlParser.getOrderedModuleList(new File(getClass().getResource("/play/deps/dependencies_test1.yml").toURI()));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertNotNull(modules);
        Iterator<String> it = modules.iterator(); 
        assertTrue(it.hasNext());
        String moduleName = it.next();   
        assertEquals("crud", moduleName);
        assertTrue(it.hasNext());
        
        moduleName = it.next();
        assertEquals("deadbolt-1.5.4", moduleName);
        
        assertTrue(it.hasNext());
        moduleName = it.next();
        assertEquals("pdf-1.5", moduleName);     
    }
    
    @Test
    public void retrieveModulesTest2() {    
        Set<String> modules = null;
        try {
            modules =  YamlParser.getOrderedModuleList(new File(getClass().getResource("/play/deps/dependencies_test2.yml").toURI()));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertNotNull(modules);
        Iterator<String> it = modules.iterator(); 
        assertTrue(it.hasNext());
        String moduleName = it.next();   
        
        assertEquals("pdf-1.5",moduleName);
        
        assertTrue(it.hasNext());
        moduleName = it.next();  
        assertEquals("deadbolt-1.5.4", moduleName);
        
        assertTrue(it.hasNext());
        moduleName = it.next();  
        assertEquals("crud", moduleName);     
    }

    @Test
    public void transitiveDependenciesFalseTest() throws IOException, URISyntaxException, ParseException {
        Resource resource = mock(Resource.class);
        InputStream inputStream = new FileInputStream(
                new File(getClass().getResource("/play/deps/dependencies_test_transitiveDependencies.yml").toURI()));

        doReturn(inputStream).when(resource).openStream();
        doReturn(0L).when(resource).getLastModified();

        YamlParser yamlParser = new YamlParser();
        ModuleDescriptor moduleDescriptor = yamlParser.parseDescriptor(
                null /*unused*/,
                null /*unused*/,
                resource,
                false /*unused*/
        );

        assertNotNull(moduleDescriptor);

        final DependencyDescriptor[] dependencies = moduleDescriptor.getDependencies();
        assertEquals(2, dependencies.length);

        for (DependencyDescriptor dependencyDescriptor : dependencies) {
            assertFalse("No dependency should be transitive because we disabled transitive globally.", dependencyDescriptor.isTransitive());
        }
    }
}
