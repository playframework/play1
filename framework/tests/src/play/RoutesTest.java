package play;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.exceptions.ActionNotFoundException;
import play.mvc.Http.Response;
import play.mvc.Router;

public class RoutesTest extends TestSupport {
    
    static TestApp testApp;
    
    @BeforeClass
    public static void init() throws Exception {
        testApp = createApp();
        testApp.addRoute("GET   /                               Application.index");   
        testApp.addRoute("GET   /hello                          Application.sayHello");  
        testApp.addRoute("GET   /hello/{name}                   Application.sayHelloTo"); 
        testApp.addRoute("GET   /noaction                       Application.doesNotExist"); 
        testApp.addRoute("GET   /clients/{<[0-9]+>id}           Application.showClient"); 
        testApp.addRoute("GET   /clients/{<[0-9]+>id}/account   Application.showClientAccount"); 
        testApp.addRoute("GET   /clients/{<[0-9]+>id}/admin     admin.Clients.index"); 
        testApp.addRoute("DELETE   /clients/{<[0-9]+>id}        admin.Clients.delete"); 
        testApp.writeController("Application", 
                "package controllers;" +                
                "public class Application extends play.mvc.Controller {" +                
                "   public static void index() {" +
                "       renderText(\"Index\");" +
                "   }" +
                "   public static void sayHello() {" +
                "       renderText(\"Hello !\");" +
                "   }" +
                "   public static void sayHelloTo(String name) {" +
                "       renderText(\"Hello %s !\", name);" +
                "   }" +
                "   public static void showClient(Integer id) {" +
                "       renderText(\"Client %s\", id);" +
                "   }" +
                "   public static void showClientAccount(Integer id) {" +
                "       renderText(\"Client %s account\", id);" +
                "   }" +
                "   public static void yop() {" +
                "       renderText(\"yop\");" +
                "   }" +
                "}"                
        );
        testApp.createControllerPackage("admin");
        testApp.writeController("admin/Clients", 
                "package controllers.admin;" +                
                "public class Clients extends play.mvc.Controller {" +                
                "   public static void index() {" +
                "       renderText(\"Admin\");" +
                "   }" +
                "   public static void delete(Integer id) {" +
                "       renderText(\"DELETED \"+id);" +
                "   }" +
                "}"                
        );
        start(testApp);
    }
    
    @AfterClass
    public static void end() {
        stop();
    }
    
    @Test
    public void testRootPath() throws Exception {        
        Response response = GET("/");
        assertIsOk(response);
        assertContentEquals("Index", response);
    }
    
    @Test
    public void testNotFound() throws Exception {        
        Response response = GET("/xxxx");
        assertIsNotFound(response);        
    }
    
    @Test
    public void testSimplePath() throws Exception {
        Response response = GET("/hello");
        assertIsOk(response);
        assertContentEquals("Hello !", response);
    }
    
    @Test
    public void testSimpleArg() throws Exception {
        Response response = GET("/hello/Guillaume");
        assertIsOk(response);
        assertContentEquals("Hello Guillaume !", response);
        //
        response = GET("/hello/Héhé");
        assertIsOk(response);
        assertContentEquals("Hello Héhé !", response);
    }
    
    @Test
    public void testCustomRegexArg() throws Exception {
        Response response = GET("/clients/15");
        assertIsOk(response);
        assertContentEquals("Client 15", response);
        //
        response = GET("/clients/xxx");
        assertIsNotFound(response);
    }
    
    @Test
    public void testMoreComplicated() throws Exception {
        Response response = GET("/clients/15/account");
        assertIsOk(response);
        assertContentEquals("Client 15 account", response);
        //
        response = GET("/clients/xxx/account");
        assertIsNotFound(response);
    }
    
    @Test
    public void testPackages() throws Exception {        
        Response response = GET("/clients/15/admin");
        assertIsOk(response);
        assertContentEquals("Admin", response);
    }
    
    @Test
    public void testRoutesReloading() throws Exception {        
        Response response = GET("/yop");
        assertIsNotFound(response);
        //
        testApp.addRoute("GET   /yop    Application.yop");  
        sleep(2);
        Router.detectChanges();
        response = GET("/yop");
        assertIsOk(response);
        assertContentEquals("yop", response);
    }   
    
    @Test(expected=ActionNotFoundException.class)
    public void testActionNotFound() throws Exception {        
        Response response = GET("/noaction");
        fail("An ActionNotFoundException should occurs !!");
    }   

    @Test
    public void testHttpMethodOverride() throws Exception {
        Response response = GET("/clients/15?x-http-method-override=DELETE");
        assertIsOk(response);
        assertContentEquals("DELETED 15", response);
    }

}
