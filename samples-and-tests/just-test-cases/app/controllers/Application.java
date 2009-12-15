package controllers;

import java.util.*;

import play.*;
import play.mvc.*;
import play.libs.*;

import play.modules.spring.*;

import models.*;
import utils.*;

public class Application extends Controller {

    // bug
    public static void aa() {
        try {
            int test = 1;
        } catch (Exception ex) {
        }
    }
    
    // bug
    public static void aaa() {
        try {
            boolean test = TestUtil.invokeTest("a");
            Logger.info("test:" + test);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        renderText("IT WORKS");
    }

    public static void index() {
        render();
    }
    
    public static void simpleStatusCode() {
        response.status = 204;
    }
    
    public static void hello(String name) {
        render(name);
    }
    
    public static void yop() {
        render();
    }
    
    public static void dynamicClassBinding(boolean fail) {
        render(fail);
    }
    
    public static void tagContexts() {
        render();
    }
    
    public static void escapeData() {
        String oops = "&nbsp;<i>Yop <!-- Coucou --></i>&nbsp;";
        render(oops);
    }
    
    public static void aGetForm(String name) {
        render("Application/hello.html", name);
    }
    
    public static void aGetForm2(String name) {
        name = "2" + name;
        render("Application/hello.html", name);
    }
    
    public static void optional() {
        renderText("OK");
    }
    
    public static void reverse() {
        render();
    }
    
    public static void mail() {
        notifiers.Welcome.welcome();
        renderText("OK");
    }
    
    public static void mail2() {
        Welcome.welcome();
        renderText("OK2");
    }
  
    public static void mail3() {
        notifiers.Welcome.welcome2();
        renderText("OK3");
    }
    
    public static void mail4() {
	notifiers.Welcome.welcome3();
        renderText("OK4");
    }
    
    public static void ifthenelse() {
        boolean a = true;
        boolean b = false;
        String c = "";
        String d = "Yop";
        int e = 0;
        int f = 5;
        Boolean g = null;
        Boolean h = true;
        Object i = null;
        Object j = new Object();
        render(a,b,c,d,e,f,g,h,i,j);
    }
    
    public static void listTag() {
        List<String> a = new ArrayList<String>();
        a.add("aa");
        a.add("ab");
        a.add("ac");
        int[] b = new int[] {0, 1, 2 , 3};
        Iterator d = a.iterator();
        render(a,b,d);
    }
    
    public static void a() {
        render();
    }
    
    public static void useSpringBean() {
        Test test = (Test)Spring.getBean("test");
        renderText(test.yop());
    }
    
    @javax.inject.Inject
    static Test myTest;
    
    public static void useSpringBeanInject() {
        renderText(myTest.yop());
    }

    public static void googleSearch(String word) {
        WS.HttpResponse response = WS.GET("http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=%s", word);
        long results = response.getJson().getAsJsonObject().getAsJsonObject("responseData").getAsJsonObject("cursor").getAsJsonPrimitive("estimatedResultCount").getAsLong();
        renderText(results);
    }

}
