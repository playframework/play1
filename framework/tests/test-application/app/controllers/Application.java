package controllers;

import play.mvc.Controller;

public class Application extends Controller {

    public static void index() throws Exception {
        response.status = 202;
        renderText("SHIGETA フローラルウォーターは !");
    }

}
