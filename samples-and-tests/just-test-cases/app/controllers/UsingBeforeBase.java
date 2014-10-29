package controllers;

import play.*;
import play.mvc.*;

public class UsingBeforeBase extends Controller {

    @Before(only = { "a", "c" }, ignoreClassNames = true )
    public static void check()
    {
        renderText("CHECKED");
    }
    public static void a()
    {
        renderText("This is A");
    }

}
