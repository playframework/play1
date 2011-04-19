package controllers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import play.Logger;
import play.mvc.Controller;

public class CustomEncoding extends Controller {

    public static void getText() {
        response.encoding = "iso-8859-1";
        renderText("Norwegian letters: ÆØÅ");
    }
    
    public static void  getTemplate() {
        response.encoding = "iso-8859-1";
        String norwegianLetters = "ÆØÅ";
        // using txt-template, since special chars (ÆØÅ) gets turned into html-code when rendering html
        renderTemplate("CustomEncoding/getTemplate.txt", norwegianLetters);
        
    }

   
}
