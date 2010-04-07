package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Fields extends Controller {

    public static void index() {
        render();
    }
    
    public static void post(Project project) {
        System.out.println(project);
        if(project.validateAndSave()) {
            renderText(project);
        }
        render("@index", project);
    }
    
}

