package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {
        
        String mySetting = Play.configuration.getProperty("mySetting");
        Logger.info("mySetting="+mySetting);
        if(mySetting == null){
            mySetting = "missing";
        }
        renderText(mySetting);
    }

}