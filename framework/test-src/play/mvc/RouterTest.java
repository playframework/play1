package play.mvc;

import org.junit.Test;
import play.Play;
import play.i18n.Lang;
import play.mvc.Http.Request;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void test_loadRoutesFiles() {
        Play.multilangRouteFiles=false;

        VirtualFile appRoot = mock(VirtualFile.class);
        List<VirtualFile> routes = new ArrayList<>();
        VirtualFile routesFile = mock(VirtualFile.class);
        when(routesFile.getName()).thenReturn("routes");
        routes.add(routesFile);
        VirtualFile appConf = mock(VirtualFile.class);
        when(appConf.getName()).thenReturn("application.conf");
        routes.add(appConf);

        VirtualFile confFolder = mock(VirtualFile.class);
        when(confFolder.list()).thenReturn(routes);

        when(appRoot.child("conf")).thenReturn(confFolder);

        assertEquals(1,Play.loadRoutesFiles(appRoot).size());
        assertEquals(false,Play.multilangRouteFiles);

        routes = new ArrayList<>();
        VirtualFile routesEnFile = mock(VirtualFile.class);
        when(routesEnFile.getName()).thenReturn("routes.en");
        routes.add(routesEnFile);
        VirtualFile routesRuFile = mock(VirtualFile.class);
        when(routesRuFile.getName()).thenReturn("routes.RU_ru");
        routes.add(routesRuFile);
        routes.add(appConf);
        when(confFolder.list()).thenReturn(routes);

        assertEquals(2,Play.loadRoutesFiles(appRoot).size());
        assertEquals(true,Play.multilangRouteFiles);

    }

    @Test
    public void test_detectNoChanges() {
        long now = System.currentTimeMillis();
        Router router = new Router();
        router.lastLoading = now;

        List<VirtualFile> routes = new ArrayList<>();
        VirtualFile routesNotModifiedFile = mock(VirtualFile.class);
        when(routesNotModifiedFile.getName()).thenReturn("routes.en");
        when(routesNotModifiedFile.lastModified()).thenReturn(now-1000);
        routes.add(routesNotModifiedFile);

        VirtualFile routesNotModifiedFile1 = mock(VirtualFile.class);
        when(routesNotModifiedFile1.getName()).thenReturn("routes.ru_RU");
        when(routesNotModifiedFile1.lastModified()).thenReturn(now);
        routes.add(routesNotModifiedFile1);

        Play.routes = routes;

        HashMap<String, VirtualFile> modulesRoutes = new HashMap<>();
        VirtualFile moduleRoute1 = mock(VirtualFile.class);
        when(moduleRoute1.lastModified()).thenReturn(now-1000);
        VirtualFile moduleRoute2 = mock(VirtualFile.class);
        when(moduleRoute2.lastModified()).thenReturn(now);
        modulesRoutes.put("1",moduleRoute1);
        modulesRoutes.put("2",moduleRoute2);

        Play.modulesRoutes=modulesRoutes;

        router.detectChanges("");
    }

    @Test
    public void test_reverseMultiLangRoutes(){
        Play.configuration = new Properties();
        List<String> applicationLangs = new ArrayList<>();
        applicationLangs.add("ru");
        applicationLangs.add("fr_FR");
        applicationLangs.add("en_GB");
        Play.langs=applicationLangs;
        Play.multilangRouteFiles=true;

        Router router = new Router();
        router.appendRoute("GET","/test/action","testAction","","","conf/routes.en_GB",0);
        router.appendRoute("GET","/test/deistvie","testAction","","","conf/routes.ru",1);
        router.appendRoute("GET","/test/activite","testAction","","","conf/routes.fr_FR",2);
        router.appendRoute("GET","/test/act","testAnotherAction","","","conf/routes.fr_FR",3);
        router.appendRoute("GET","/test/akt","testAnotherAction","","","conf/routes.ru",4);
        router.appendRoute("GET","/test/active","testAnotherAction","","","conf/routes.en_GB",5);

        Lang.change("ru");
        Router.ActionDefinition testAction = router.reverse("testAction", new HashMap<String, Object>());
        assertEquals("/test/deistvie",testAction.url);

        Lang.change("en_GB");
        testAction = router.reverse("testAction", new HashMap<String, Object>());
        assertEquals("/test/action",testAction.url);

        Lang.change("fr_FR");
        testAction = router.reverse("testAction", new HashMap<String, Object>());
        assertEquals("/test/activite",testAction.url);
    }

    @Test
    public void test_routeMultilangActivatesLang(){
        Play.configuration = new Properties();
        List<String> applicationLangs = new ArrayList<>();
        applicationLangs.add("ru");
        applicationLangs.add("fr_FR");
        applicationLangs.add("en_GB");
        Play.langs=applicationLangs;
        Play.multilangRouteFiles=true;

        Router router = new Router();
        router.appendRoute("GET","/test/action","testAction","","","conf/routes.en_GB",0);
        router.appendRoute("GET","/test/deistvie","testAction","","","conf/routes.ru",1);
        router.appendRoute("GET","/test/activite","testAction","","","conf/routes.fr_FR",2);
        router.appendRoute("GET","/test/act","testAnotherAction","","","conf/routes.fr_FR",3);
        router.appendRoute("GET","/test/akt","testAnotherAction","","","conf/routes.ru",4);
        router.appendRoute("GET","/test/active","testAnotherAction","","","conf/routes.en_GB",5);

        Lang.change("en_GB");
        assertEquals("en_GB",Lang.get());
        Http.Request request = mock(Http.Request.class);
        request.method="GET";
        request.path="/test/activite";
        request.format="text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
        request.domain="github.com";
        router.route(request);
        assertEquals("fr_FR",Lang.get());
    }
}
