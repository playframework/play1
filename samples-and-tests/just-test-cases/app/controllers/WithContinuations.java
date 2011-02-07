package controllers;

import play.*;
import play.mvc.*;
import play.db.jpa.*;
import play.libs.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import models.*;

public class WithContinuations extends Controller {

    public static void loopWithWait() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<5; i++) {
            if(i>0) sb.append(";");
            long s = System.currentTimeMillis();
            await("1s");
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
            String r = await(new jobs.DoSomething(1000).now());
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
            List<String> r = await(Task.waitAll(new jobs.DoSomething(1000).now(), new jobs.DoSomething(2000).now()));
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
            String r = await(Task.waitAny(new jobs.DoSomething(1000).now(), new jobs.DoSomething(2000).now()));
            boolean delay = System.currentTimeMillis() - s > 1000 && System.currentTimeMillis() - s < 2000;
            sb.append(i + ":" + delay + "[" + r + "]");
        }
        renderText(sb);
    }
    
    public static void withNaiveJPA() {
        User bob = new User("bob").save();
        await("1s");
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
        await("1s");
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
            await(100);
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
            await(100);
        }
        renderText("OK");
    }
    
    public static void streamedResult() {
        response.contentType = "text/html";
        response.writeChunk("<h1>This page should load progressively in about 10 seconds</h1>");
        long s = System.currentTimeMillis();
        for(int i=0; i<100; i++) {
            await(100);
            response.writeChunk("<h2>Hello " + i + "</h2>");
        }
        long t = System.currentTimeMillis() - s;
        response.writeChunk("Time: " + t + ", isOk->" + (t > 10000 && t < 20000));
    }
    
    public static void loopWithCallback() {
        final AtomicInteger i = new AtomicInteger(0);
        final AtomicLong s = new AtomicLong(System.currentTimeMillis());
        final StringBuilder sb = new StringBuilder();
        final F.Action0 f = new F.Action0() {
            public void invoke() {
                if(i.getAndIncrement() > 0) sb.append(";");
                if(i.get() > 5) {
                    renderText(sb);
                } else {
                    boolean delay = System.currentTimeMillis() - s.get() > 1000 && System.currentTimeMillis() - s.get() < 1500;
                    sb.append(i + ":" + delay);
                    s.set(System.currentTimeMillis());
                    await("1s", this);
                }
            }
        };
        await("1s", f);
    }
    
    public static void streamedCallback() {
        final StringBuilder sb = new StringBuilder();
        final AtomicLong s = new AtomicLong(System.currentTimeMillis());
        response.contentType = "text/html";
        F.Action0 callback = new F.Action0() {
            public void invoke() {
                sb.append(".");
                System.out.println(sb);
                if(sb.length() < 100) {
                    response.writeChunk("<h1>Hello " + sb.length() + "</h1>");
                    await(100, this);
                } else {
                    long t = System.currentTimeMillis() - s.get();
                    response.writeChunk("Time: " + t + ", isOk->" + (t > 10000 && t < 20000));
                }                
            }
        };
        response.writeChunk("<h1>Begin</h1>");
        await(100, callback);
    }
    
    public static void jpaAndCallback() {
        final User bob = new User("bob").save();
        await("1s", new F.Action0() {
            public void invoke() {
                // We are now in a new transaction! So it should fail
                bob.name = "coco";
                bob.save();
                renderText("OK");
            }
        });
    }
    
    public static void callbackWithResult() {
        await(Task.waitAny(new jobs.DoSomething(1000).now(), new jobs.DoSomething(2000).now()), new F.Action<String>() {
            public void invoke(String result) {
                renderText("yep -> %s", result);
            }
        });
    }
    
    public static void callbackWithResults() {
        await(Task.waitAll(new jobs.DoSomething(1000).now(), new jobs.DoSomething(2000).now()), new F.Action<List<String>>() {
            public void invoke(List<String> result) {
                renderText("yep -> %s", result);
            }
        });
    }
    
}

