import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import play.mvc.Http.Response;
import play.test.Fixtures;
import play.test.FunctionalTest;


public class BinaryTest extends FunctionalTest {
	

	@Before
	public void setUp() {
		Fixtures.deleteAttachmentsDir();
		// Fixtures.deleteAll(); // see Bug #491403
		Response deletedResponse = GET("/binary/deleteAll");
		assertStatus(200, deletedResponse);
	}
	
	@Test
	public void testUploadSomething(){
		String imageURL = "/binary/showavatar?id=1";
		Response getResponse = GET(imageURL);
		assertStatus(404, getResponse);
		
		String url="/binary/save";
		Map<String,String> parameters= new HashMap<String,String>();
		parameters.put("user.username", "username");
		Map<String, File> files= new HashMap<String, File>();
		File f = new File("test/fond1.png");
		assertTrue(f.exists());
		files.put("user.avatar", f);
		Response uploadResponse = POST(url, parameters, files);
		assertStatus(302, uploadResponse);
		String id = uploadResponse.getHeader("Location").split("=")[1];
		imageURL = "/binary/showavatar?id=" + id;
		getResponse = GET(imageURL);
		assertStatus(200, getResponse);
	}

}
