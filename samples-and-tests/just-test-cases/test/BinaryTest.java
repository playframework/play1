import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import play.Play;
import play.mvc.Http.Response;
import play.mvc.results.Redirect;
import play.test.Fixtures;
import play.test.FunctionalTest;
import controllers.Binary;


public class BinaryTest extends FunctionalTest {
	

	@Before
	public void setUp() {
		Fixtures.deleteDirectory("attachments");
		// Fixtures.deleteAll(); // see Bug #491403
		String deleteURL=null;
		try{
			Binary.deleteAll();
		}catch(Redirect redirect){
			deleteURL=redirect.url;
		}		
		Response deletedResponse = GET(deleteURL);
		assertStatus(200, deletedResponse);
	}
	
	@Test
	public void testUploadSomething() {
		String imageURL=null;
		try{
			Binary.showAvatar(1l);
		}catch(Redirect redirect){
			imageURL=redirect.url;
		}
		Response getResponse = GET(imageURL);
		assertStatus(404, getResponse);
		
		String url=null;
		try{
			Binary.save(null);
		}catch(Redirect redirect){
			url = redirect.url;
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
		imageURL = null;
		try{
			Binary.showAvatar(new Long(id));
		}catch(Redirect redirect){
			imageURL = redirect.url;
		}
		getResponse = GET(imageURL);
		assertStatus(200, getResponse);
	}

}
