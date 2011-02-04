package controllers;

import play.*;
import play.mvc.*;
import play.db.jpa.*;
import play.libs.*;

import java.util.*;

import models.*;

public class WithContinuations extends Controller {

    public static void loopWithWait() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<5; i++) {
            if(i>0) sb.append(";");
            long s = System.currentTimeMillis();
            wait("1s");
            boolean delay = System.currentTimeMillis() - s > 1000 && System.currentTimeMillis() - s < 1500;
            sb.append(i + ":" + delay);
        }
        renderText(sb);
    }
    
    public static void waitFuture() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<5; i++) {
            if(i>0) sb.append(";");
            long s = System.currentTimeMillis();
            String r = wait(new jobs.DoSomething(1000).now());
            boolean delay = System.currentTimeMillis() - s > 1000 && System.currentTimeMillis() - s < 1500;
            sb.append(i + ":" + delay + "[" + r + "]");
        }
        renderText(sb);
    }
    
    public static void waitAll() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<2; i++) {
            if(i>0) sb.append(";");
            long s = System.currentTimeMillis();
            List<String> r = wait(Task.waitAll(new jobs.DoSomething(1000).now(), new jobs.DoSomething(2000).now()));
            boolean delay = System.currentTimeMillis() - s > 2000 && System.currentTimeMillis() - s < 2500;
            sb.append(i + ":" + delay + "[" + r + "]");
        }
        renderText(sb);
    }
    
    public static void waitAny() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<2; i++) {
            if(i>0) sb.append(";");
            long s = System.currentTimeMillis();
            String r = wait(Task.waitAny(new jobs.DoSomething(1000).now(), new jobs.DoSomething(2000).now()));
            boolean delay = System.currentTimeMillis() - s > 1000 && System.currentTimeMillis() - s < 2000;
            sb.append(i + ":" + delay + "[" + r + "]");
        }
        renderText(sb);
    }
    
    public static void withNaiveJPA() {
        User bob = new User("bob").save();
        wait("1s");
        // We are now in a new transaction! So it should fail
        bob.name = "coco";
        bob.save();
        renderText("OK");
    }
    
    public static void getUserByName(String name) {
        renderText("Users:" + User.count() + " -> " + User.find("byName", name).first());
    }
    
    public static void withJPA() {
        User bob = new User("kiki").save();
        wait("1s");
        // We are now in a new transaction! So we need to merge previous JPA instances
        bob = bob.merge();
        bob.name = "coco";
        bob.save();
        renderText("OK");
    }
    
    public static void rollbackWithoutContinuations() {
        for(int i=0; i<10; i++) {
            new User("user" + i).save();
        }
        // No users should be inserted
        JPA.setRollbackOnly();
        renderText("OK");
    }
    
    public static void rollbackWithContinuations() {
        for(int i=0; i<10; i++) {
            new User("user" + i).save();
            wait(100);
        }
        // Too late! Each continuation uses its own transaction... we can't rollback them anymore
        JPA.setRollbackOnly();
        renderText("OK");
    }
    
    public static void rollbackWithContinuationsThatWorks() {
        for(int i=0; i<10; i++) {
            new User("oops" + i).save();
            // Rollback before triggering the continuation, so we'll properly rollback the current transaction
            JPA.setRollbackOnly();
            wait(100);
        }
        renderText("OK");
    }
    
}

