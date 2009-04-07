package controllers;

import java.util.*;

import play.*;
import play.mvc.*;
import play.test.*;
import play.exceptions.*;

public class TestRunner extends Controller {

    public static void index() {
        List<Class> applicationTests = Tests.allApplicationTests();
        render(applicationTests);
    }

}

