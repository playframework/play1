package play;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import play.data.validation.Validation;
import play.mvc.Http.Response;

public class BindingTest extends TestSupport {
    
    static TestApp testApp;
    
    public static class Person {
        public String name;
        public String email;
    }
    
    @BeforeClass
    public static void init() throws Exception {
        testApp = createApp();
        testApp.addRoute("*   /action1                         Application.action1");
        testApp.addRoute("*   /action2                         Application.action2");
        testApp.addRoute("*   /action3                         Application.action3");
        testApp.addRoute("*   /action4                         Application.action4");
        testApp.addRoute("*   /action5                         Application.action5");
        testApp.addRoute("*   /action6                         Application.action6");
        testApp.addRoute("*   /action7                         Application.action7");
        testApp.addRoute("*   /action8                         Application.action8");
        testApp.addRoute("*   /action9                         Application.action9");
        testApp.writeController("Application", 
                "package controllers;" +                
                "public class Application extends play.mvc.Controller {" +                
                "   public static void action1(String p) {" +
                "       renderText(\"%s\", p);" +
                "   }" +
                "   public static void action2(String[] p) {" +
                "       renderText(\"%s\", p.length);" +
                "   }" +
                "   public static void action3(java.util.List<String> p) {" +
                "       renderText(\"%s\", p.size());" +
                "   }" +
                "   public static void action4(java.util.ArrayList<String> p) {" +
                "       renderText(\"%s\", p.size());" +
                "   }" +
                "   public static void action5(java.util.Set<String> p) {" +
                "       renderText(\"%s\", p.size());" +
                "   }" +
                "   public static void action6(Integer p) {" +
                "       renderText(\"%s\", p);" +
                "   }" +
                "   public static void action7(int p) {" +
                "       renderText(\"%s\", p);" +
                "   }" +
                "   public static void action8(int a, Double b, String c, Boolean d, Integer[] e) {" +
                "       renderText(\"%s %s %s %s %s\", a, b, c, d, e.length);" +
                "   }" +
                "   public static void action9( play.BindingTest.Person p) {" +
                "       renderText( p.name+\" \"+p.email  );" +
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
    public void bindJson() throws Exception {        
        String body = "{ \"p\":\"Jojo\" }";
        Response response = POST("/action1", "application/javascript", body );
        assertIsOk(response);
        assertContentEquals("Jojo", response);

        body = "{ a:14, b:8, c:\"Kiki\",d:true,e:[7,9] }";
        response = POST("/action8", "application/javascript", body );
        assertIsOk(response);
        assertContentEquals("14 8.0 Kiki true 2", response);

        // ko: pas le meme format de parametres multiples
        //body = "{ \"p\":[\"Jojo\",\"Java\"]}";
        //response = POST("/action2", "application/javascript", body );
        //assertIsOk(response);
        //assertContentEquals("2", response);
    }
    
    
    @Test
    public void bindString() throws Exception {        
        Response response = GET("/action1?p=Jojo");
        assertIsOk(response);
        assertContentEquals("Jojo", response);
        //
        response = GET("/action1?p=Jojo&p=Jaja");
        assertIsOk(response);
        assertContentEquals("Jojo", response);
    }

    @Test
    public void bindStringArray() throws Exception {        
        Response response = GET("/action2?p=Jojo");
        assertIsOk(response);
        assertContentEquals("1", response);
        //
        response = GET("/action2?p=Jojo&p=Jaja");
        assertIsOk(response);
        assertContentEquals("2", response);
        //
        response = GET("/action2?p=Jojo&p=Jaja&p=Jiji");
        assertIsOk(response);
        assertContentEquals("3", response);
    }
    
    @Test
    public void bindStringList() throws Exception {        
        Response response = GET("/action3?p=Jojo");
        assertIsOk(response);
        assertContentEquals("1", response);
        //
        response = GET("/action3?p=Jojo&p=Jaja");
        assertIsOk(response);
        assertContentEquals("2", response);
        //
        response = GET("/action3?p=Jojo&p=Jaja&p=Jiji");
        assertIsOk(response);
        assertContentEquals("3", response);
        //
        response = GET("/action3");
        assertIsOk(response);
        assertContentEquals("0", response);
    }
    
    @Test
    public void bindStringListImpl() throws Exception {        
        Response response = GET("/action4?p=Jojo");
        assertIsOk(response);
        assertContentEquals("1", response);
        //
        response = GET("/action4?p=Jojo&p=Jaja");
        assertIsOk(response);
        assertContentEquals("2", response);
        //
        response = GET("/action4?p=Jojo&p=Jaja&p=Jiji");
        assertIsOk(response);
        assertContentEquals("3", response);
    }
    
    @Test
    public void bindStringSet() throws Exception {        
        Response response = GET("/action5?p=Jojo");
        assertIsOk(response);
        assertContentEquals("1", response);
        //
        response = GET("/action5?p=Jojo&p=Jaja");
        assertIsOk(response);
        assertContentEquals("2", response);
        //
        response = GET("/action5?p=Jojo&p=Jaja&p=Jiji");
        assertIsOk(response);
        assertContentEquals("3", response);
    }
    
    @Test
    public void bindInteger() throws Exception {        
        Response response = GET("/action6?p=14");
        assertIsOk(response);
        assertContentEquals("14", response);
        //
        response = GET("/action6");
        assertIsOk(response);
        assertContentEquals("null", response);
    }
    
    @Test
    public void bindInt() throws Exception {        
        Response response = GET("/action7?p=14");
        assertIsOk(response);
        assertContentEquals("14", response);
    }
    
    @Test
    public void bindBadValue() throws Exception {        
        Response response = GET("/action7?p=kiki");
        assertTrue(Validation.hasError("p"));
    }
    
    @Test
    public void bindMultiple() throws Exception {        
        Response response = GET("/action8?a=14&b=8&c=Kiki&d=true&e=7&e=9");
        assertIsOk(response);
        assertContentEquals("14 8.0 Kiki true 2", response);
    }

    @Test
    public void bindComposite() throws Exception {        
        Response response = GET("/action9?p.name=Doe&p.email=john.doe@foo.com");
        assertIsOk(response);
        assertContentEquals("Doe john.doe@foo.com", response);
    }

}
