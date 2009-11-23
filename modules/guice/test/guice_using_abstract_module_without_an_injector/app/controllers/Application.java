package controllers;

import play.mvc.*;
import javax.inject.Inject;
import utils.TestInter;


public class Application extends Controller {

    @Inject static TestInter test;

    public static void index() {
		String shouldBeOk = test.printer();
        render(shouldBeOk);
    }

}
