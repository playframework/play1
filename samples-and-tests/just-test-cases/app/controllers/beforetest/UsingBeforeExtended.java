package controllers.beforetest;

import controllers.*;
import play.*;

public class UsingBeforeExtended extends UsingBeforeBase {

    public static void b()
    {
        renderText("This is B");
    }

    public static void c()
    {
        renderText("This is C");
    }

}
