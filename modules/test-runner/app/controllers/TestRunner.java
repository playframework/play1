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

    public static void run(String test) throws Exception {
        if(test.equals("ts")) {
            render();
        }
        if(test.equals("montest.html")) {
            render("TestRunner/montest.html");
        }
		if(test.endsWith(".class")) {
			Thread.sleep(250);
			TestEngine.TestResults results = TestEngine.run(test.substring(0, test.length()-6));
			response.status = results.passed ? 200 : 500;
	        render("TestRunner/results.html", results);
		}        
    }
    
    public static void saveResult(String test) {
        System.out.println("SAVING "+test);
        renderText("Result are saved for test %s", test);
    }

}

