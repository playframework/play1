package play.mvc;

import org.junit.Test;

import play.Play;
import play.mvc.Http.Request;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.*;

public class RouterTest {

    @Test
    public void test_getBaseUrl() {

        Play.configuration = new Properties();

        // test with currentRequest
        Http.Request request = Http.Request.createRequest(
                null,
                "GET",
                "/",
                "",
                null,
                null,
                null,
                null,
                false,
                80,
                "localhost",
                false,
                null,
                null
        );

        Http.Request.current.set( request );
        assertThat(Router.getBaseUrl()).isEqualTo("http://localhost");

        // test without current request
        Http.Request.current.remove();
        // application.baseUrl without trailing /
        Play.configuration.setProperty("application.baseUrl", "http://a");
        assertThat(Router.getBaseUrl()).isEqualTo("http://a");

        // application.baseUrl with trailing /
        Play.configuration.setProperty("application.baseUrl", "http://b/");
        assertThat(Router.getBaseUrl()).isEqualTo("http://b");
    }

    @Test
    public void test_hostStaticDir() {
        
        Play.configuration = new Properties();
        
        // we add a static route for a specific domain only
        Router.addRoute("GET", "example.com/pics/", "staticDir:/public/images");
        // another static route with NO specific domain
        Router.addRoute("GET", "/music/", "staticDir:/public/mp3");

        // we request a static image file (which lives only on a specific domain)
        Http.Request imageRequest = Http.Request.createRequest(
                null,
                "GET",
                "/pics/chuck-norris.jpg",
                "",
                null,
                null,
                null,
                null,
                false,
                80,
                "localhost", // domain gets changed below a few times
                false,
                null,
                null
        );
        // we also request a static music file (which lives on NO specific domain)
        Http.Request musicRequest = Http.Request.createRequest(
                null,
                "GET",
                "/music/michael-jackson_black-or-white.mp3",
                "",
                null,
                null,
                null,
                null,
                false,
                80,
                "localhost", // domain gets changed below a few times
                false,
                null,
                null
        );
        
        // Test on localhost
        assertFalse("Image file [" + imageRequest.domain + "] from the wrong/different domain must not be found", canRenderFile(imageRequest));
        assertTrue("Image file [" + imageRequest.domain + "] from the wrong/different domain must not be found", canRenderFile(musicRequest));
        
        // Test on localhost:9000
        imageRequest.port = 9000;
        musicRequest.port = 9000;
        assertFalse("Image file [" + imageRequest.domain + "] from the wrong/different domain must not be found", canRenderFile(imageRequest));
        assertTrue("Image file [" + imageRequest.domain + "] from the wrong/different domain must not be found", canRenderFile(musicRequest));
        
        // we request the image file from a "wrong"/different domain, it will not be found
        imageRequest.port = 80;
        musicRequest.port = 80;
        imageRequest.domain = "google.com";
        assertFalse("Image file [" + imageRequest.domain + "] from the wrong/different domain must not be found", canRenderFile(imageRequest));

        // same for musicfile, but it will be rendered because the domain doesn't matter
        musicRequest.domain = "google.com";
        
        assertTrue("Musicfile [" + musicRequest.domain + "] file  must be found", canRenderFile(musicRequest));
                
        // we request the image file from the "right" domain
        imageRequest.domain = "example.com";
        assertTrue("Image file [" + musicRequest.domain + "] from the right domain must be found", canRenderFile(imageRequest));
        
        // same for musicfile, it will be rendered again also on this domain
        musicRequest.domain = "example.com";
        assertTrue("Musicfile [" + musicRequest.domain + "] from the right domain must be found", canRenderFile(musicRequest));
    }
    
    public boolean canRenderFile(Request request){
        try {
            Router.route(request);
        } catch(RenderStatic rs) {
            return true;
        }  catch(NotFound nf) {
            return false;
        }
        return false;
    }
}
