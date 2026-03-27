package controllers;

import play.*;
import play.db.jpa.JPA;
import play.mvc.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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
        CompletableFuture p = job.now();
        await(p);
        renderText("Job Connection leaked!");
    }
    
}