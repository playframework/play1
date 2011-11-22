package controllers;

import play.i18n.Lang;
import play.mvc.Controller;


public class Language extends Controller {
    
    public static void lang() {
        renderText(Lang.getLocale().toString());
    }
}
