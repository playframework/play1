package controllers;

import java.util.*;

import play.*;
import play.mvc.*;
import play.test.*;
import play.exceptions.*;

public class TestRunner extends Controller {

    public static void index() {
        List<Class> simpleTests = TestEngine.allSimpleTests();
        List<Class> virtualClientTests = TestEngine.allVirtualClientTests();
        render(simpleTests, virtualClientTests);
    }

    public static void run(String test) {
        if(test.equals("ts")) {
            render();
        }
        if(test.equals("montest.html")) {
            render("TestRunner/montest.html");
        }
        boolean success = TestEngine.run(test);
        if(success) {
            response.status = 200;
        } else {
            response.status = 500;
        }
        renderText("finished");
    }
    
    public static void saveResult(String test) {
        System.out.println("SAVING "+test);
        renderText("Result are saved for test %s", test);
    }

}

