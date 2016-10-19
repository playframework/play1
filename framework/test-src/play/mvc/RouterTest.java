package play.mvc;

import com.ning.http.client.RequestBuilder;
import org.junit.Test;

import org.mockito.Matchers;
import org.mockito.Mockito;
import play.Play;
import play.PlayPlugin;
import play.mvc.Http.Request;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.plugins.PluginCollection;

import java.util.*;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

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
    public void test_reverse() {
        Play.configuration = new Properties();

        // add one route
        Router.addRoute("GET", "/one/url", "action");
        // add another route
        Router.addRoute("GET", "/another/url", "action");

        Router.ActionDefinition action = Router.reverse("action", new HashMap<String, Object>());
        assertEquals("/another/url",action.url);
    }

    @Test
    public void test_getActionRoutes(){
        Play.configuration = new Properties();

        // add one route
        Router.addRoute("GET", "/one/url", "action");
        // add another route
        Router.addRoute("GET", "/another/url", "action");

        Router.Route route = mock(Router.Route.class);
        route.path="/third/url";
        route.args=new ArrayList<Router.Route.Arg>();
        route.method="GET";
        route.staticArgs=new HashMap<String, String>();

        Router.ActionRoute actionRoute = new Router.ActionRoute();
        actionRoute.setRoute(route);
        List<Router.ActionRoute> actionRoutes = Collections.singletonList(actionRoute);

        PluginCollection pluginCollection = mock(PluginCollection.class);
        //Plugin returns selected action route which will redefine matching routes
        when(pluginCollection.selectActionRoutes(Matchers.<List<Router.ActionRoute>>any())).thenReturn(actionRoutes);
        Play.pluginCollection=pluginCollection;

        Router.ActionDefinition action = Router.reverse("action", new HashMap<String, Object>());
        assertEquals("/third/url",action.url);
    }

    @Test
    public void test_selectActionRoutes(){
        PluginCollection pc = mock(PluginCollection.class);
        Play.pluginCollection = pc;
        PlayPlugin onePlayPlugin = mock(PlayPlugin.class);
        //Single plugin selectActionRoutes not overrided
        when(onePlayPlugin.selectActionRoutes(Matchers.<List<Router.ActionRoute>>any())).thenCallRealMethod();
        when(pc.getEnabledPlugins()).thenReturn(Collections.singletonList(onePlayPlugin));
        when(pc.selectActionRoutes(Matchers.<List<Router.ActionRoute>>any())).thenCallRealMethod();

        Router.ActionRoute actionRouteOne = new Router.ActionRoute();
        List<Router.ActionRoute> actionRoutesList = Collections.singletonList(actionRouteOne);
        List<Router.ActionRoute> selectedActionRoutes = Play.pluginCollection.selectActionRoutes(actionRoutesList);
        assertNull(selectedActionRoutes);

        //Single plugin selectActionRoutes returns same list as got from parameters
        when(onePlayPlugin.selectActionRoutes(Matchers.<List<Router.ActionRoute>>any())).thenReturn(actionRoutesList);
        assertEquals(actionRoutesList,Play.pluginCollection.selectActionRoutes(actionRoutesList));

        PlayPlugin anotherPlayPlugin = mock(PlayPlugin.class);
        Router.ActionRoute actionRouteAnother = new Router.ActionRoute();
        List<Router.ActionRoute> actionRoutesAnotherList = Collections.singletonList(actionRouteAnother);
        //Results of first plugin will be passed as parameter to the seccond, forming pipe
        when(anotherPlayPlugin.selectActionRoutes(actionRoutesList)).thenReturn(actionRoutesAnotherList);

        List<PlayPlugin> enabledPlugins = new ArrayList<PlayPlugin>();
        enabledPlugins.add(onePlayPlugin);
        enabledPlugins.add(anotherPlayPlugin);
        //Two plugins returned by getEnabledPlugins
        when(pc.getEnabledPlugins()).thenReturn(enabledPlugins);
        //Check two plugins were called in a pipe
        assertEquals(actionRoutesAnotherList,Play.pluginCollection.selectActionRoutes(actionRoutesList));
    }
}
