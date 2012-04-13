import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import play.Logger;
import play.Play;
import play.data.MemoryUpload;
import play.data.Upload;
import play.mvc.Http;
import play.mvc.Http.Response;
import play.mvc.results.Redirect;
import play.test.Fixtures;
import play.test.FunctionalTest;
import controllers.Binary;

public class BinaryTest extends FunctionalTest {

    @Before
    public void setUp() {
        Fixtures.deleteAll(); // see Bug #491403
        Fixtures.deleteDirectory("attachments");
        URL deleteURL = reverse(); {
            Binary.deleteAll();
        }
        Response deletedResponse = GET(deleteURL);
        assertStatus(200, deletedResponse);
    }
    
    @Test
    public void testUploadSomething() {
        URL imageURL = reverse(); {
            Binary.showAvatar(1l);
        }
        Response getResponse = GET(imageURL);
        assertStatus(404, getResponse);
        
        URL url = reverse(); {
            Binary.save(null);
        }
        Map<String,String> parameters= new HashMap<String,String>();
        parameters.put("user.username", "username");
        Map<String, File> files= new HashMap<String, File>();
        File f = Play.getFile("test/fond1.png");
        assertTrue(f.exists());
        files.put("user.avatar", f);
        Response uploadResponse = POST(url, parameters, files);
        assertStatus(302, uploadResponse);
        String id = uploadResponse.getHeader("Location").split("=")[1];
        imageURL = reverse(); {
            Binary.showAvatar(new Long(id));
        }
        getResponse = GET(imageURL);
        assertStatus(200, getResponse);
    }

    @Test
    public void testUploadBigFile() {

        Map<String,String> parameters= new HashMap<String,String>();

        Map<String, File> files= new HashMap<String, File>();
        File file = Play.getFile("test/winie.jpg");
        assertTrue(file.exists());
        files.put("file", file);
        Response uploadResponse = POST("/Binary/uploadFile", parameters, files);

        assertStatus(200, uploadResponse);

        String size = uploadResponse.getHeader("Content-Length");

        assertEquals("Size does not match", "1366949", size);
    }

    @Test
    public void testUploadBigFile2() {

        Map<String,String> parameters= new HashMap<String,String>();

        Map<String, File> files= new HashMap<String, File>();
        File file = Play.getFile("test/winie.jpg");
        assertTrue(file.exists());

        files.put("upload", file);
        Response uploadResponse = POST("/Binary/upload", parameters, files);

        assertStatus(200, uploadResponse);

        String size = uploadResponse.getHeader("Content-Length");

        assertEquals("Size does not match", "1366949", size);
    }

    @Test
    public void testUploadSmallFile() {

        Map<String,String> parameters= new HashMap<String,String>();

        Map<String, File> files= new HashMap<String, File>();
        File file = Play.getFile("test/angel.gif");
        assertTrue(file.exists());
        files.put("file", file);
        Response uploadResponse = POST("/Binary/uploadFile", parameters, files);

        assertStatus(200, uploadResponse);

        String size = uploadResponse.getHeader("Content-Length");

        assertEquals("Size does not match", "2440", size);
    }

    @Test
    public void testUploadSmallFile2() {

        Map<String,String> parameters= new HashMap<String,String>();

        Map<String, File> files= new HashMap<String, File>();
        File file = Play.getFile("test/angel.gif");
        assertTrue(file.exists());

        files.put("upload", file);
        Response uploadResponse = POST("/Binary/upload", parameters, files);

        assertStatus(200, uploadResponse);

        String size = uploadResponse.getHeader("Content-Length");

        assertEquals("Size does not match", "2440", size);
    }

//  TODO: Missing possibility to upload multiple files at once
//  See: http://play.lighthouseapp.com/projects/57987-play-framework/tickets/472-functionaltest-and-ws-client-library-dont-allow-upload-of-multiple-file#ticket-472-2 
//    @Test
//    public void testMultipleUpload() {
//
//        Map<String,String> parameters= new HashMap<String,String>();
//
//        Map<String, File[]> files= new HashMap<String, File[]>();
//        File file1 = Play.getFile("test/angel.gif");
//        assertTrue(file1.exists());
//
//        File file2 = Play.getFile("test/winie   .gif");
//        assertTrue(file1.exists());
//
//        files.put("upload", new File[] {file1, file2 });
//        Response uploadResponse = POST("/Binary/uploadMultiple", parameters, files);
//
//        assertStatus(200, uploadResponse);
//
//        String size = uploadResponse.getHeader("Content-Length");
//
//        assertEquals("Size does not match", "2440", size);
//    }

    @Test
    public void testGetBinaryWithCustomContentType() {
        Response response = GET("/binary/getBinaryWithCustomContentType");
        assertIsOk(response);
        assertContentType("custom/contentType", response);
    }

    // Tests to check whether input streams to renderBinary are closed.

    @Test
    public void testGetEmptyBinary() {
        Response response = GET("/binary/getEmptyBinary");
        assertIsOk(response);
        assertTrue(Binary.emptyInputStreamClosed);
    }

    @Test
    public void testGetErrorBinary() {
        try {
            GET("/binary/getErrorBinary");
            fail();
        }
        catch (Exception e) {
        }
        assertTrue(Binary.errorInputStreamClosed);
    }
}
