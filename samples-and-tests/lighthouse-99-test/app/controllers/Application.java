package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {
	
		Logger.debug("This log message should be logged when configuring log4j manually to DEBUG-level.");
        render();
    }

}