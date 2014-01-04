package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {
    	List<Student> students = Student.findAll();
    	List<Teacher> teachers = Teacher.findAll();

    	// Ok, renders our results from the 2 different db
        
        render(students, teachers);
    }

}