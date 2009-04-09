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
        boolean success = TestEngine.run(test);
        if(success) {
            response.status = 200;
        } else {
            response.status = 500;
        }
        renderText("finished");
    }

}

