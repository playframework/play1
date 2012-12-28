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
    
    public static void testField() {
    	SubProject subProject = new SubProject();
    	subProject.setSubProjectObservation("sub project observation value");
    	subProject.subProjectName = "sub project name value";
    	subProject.setObservation("project observation value");
    	subProject.name = "project name value";
    	
    	subProject.parent = new Project();
    	subProject.parent.name = "parent project name value";
    	subProject.parent.setObservation("parent project observation value");
    	
    	SubProject subProjectNulls = new SubProject();
    	
    	render(subProject, subProjectNulls);
    }
    
}

