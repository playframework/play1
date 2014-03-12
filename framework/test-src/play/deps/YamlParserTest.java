package play.deps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import play.Play;
import play.PlayBuilder;

@Ignore
public class YamlParserTest {
    
    @BeforeClass
    public static void setUp(){
        // Play
        new PlayBuilder().build();
        System.setProperty("play.version", Play.version);
        
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
        List<String> modules = null;
        try {
            modules =  YamlParser.getOrderedModuleList(new File(Play.applicationPath, "fakeFile.yml"));
        } catch (Exception e) {
            assertTrue(e.getMessage().startsWith("There was a problem to find the file"));
            throw e;
        }
    }
    

    @Test
    public void retrieveModulesTest() {    
        List<String> modules = null;
        try {
            modules =  YamlParser.getOrderedModuleList(new File(getClass().getResource("/play/deps/dependencies_test1.yml").toURI()));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertNotNull(modules);
        assertEquals("crud", modules.get(0));
        assertEquals("deadbolt-1.5.4", modules.get(1));
        assertEquals("pdf-1.5", modules.get(2));     
    }
    
    @Test
    public void retrieveModulesTest2() {    
        List<String> modules = null;
        try {
            modules =  YamlParser.getOrderedModuleList(new File(getClass().getResource("/play/deps/dependencies_test2.yml").toURI()));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertNotNull(modules);
        assertEquals("pdf-1.5", modules.get(0));
        assertEquals("deadbolt-1.5.4", modules.get(1));
        assertEquals("crud", modules.get(2));     
    }
    
    
}
