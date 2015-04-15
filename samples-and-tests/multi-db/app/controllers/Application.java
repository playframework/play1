package controllers;

import play.*;
import play.db.jpa.JPA;
import play.libs.F.Promise;
import play.mvc.*;

import java.util.*;

import jobs.SomeJob;
import models.*;

public class Application extends Controller {

    public static void index() {
    	List<Student> students = Student.findAll();
    	List<Teacher> teachers = Teacher.findAll();

    	// Ok, renders our results from the 2 different db
        
        render(students, teachers);
    }

    public static void connectionLeak() {
        JPA.closeTx(JPA.DEFAULT);
        renderText("View Connection leaked!");
    }


    public static void noTransactionJob() {
        SomeJob job = new SomeJob();
        Promise p = job.now();
        await(p);
        renderText("Job Connection leaked!");
    }
    
}