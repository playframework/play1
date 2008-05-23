package controllers;

import play.mvc.Controller;

public class Application extends Controller {

    public static void index() throws Exception {
        renderText("SHIGETA フローラルウォーターは");
    }

}
