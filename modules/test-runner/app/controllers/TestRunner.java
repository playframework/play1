package controllers;

import java.util.*;

import play.mvc.*;
import play.test.*;

public class TestRunner extends Controller {

    public static void index() {
        List<Class> unitTests = TestEngine.allUnitTests();
        List<Class> functionalTests = TestEngine.allFunctionalTests();
        render(unitTests, functionalTests);
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
	        render("TestRunner/results.html", test, results);
		}        
    }
    
    public static void saveResult(String test) {
        System.out.println("SAVING "+test);
        renderText("Result are saved for test %s", test);
    }

}

