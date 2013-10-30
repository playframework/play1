package controllers;

import play.mvc.Controller;

// Controller to test various redirects
public class Redirector extends Controller {

    public static void index() {
        String target = params.get("target");
        target = target == null ? "/" : target;
        redirect(target);
    }

}
