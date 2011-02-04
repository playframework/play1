package controllers;

import play.*;
import play.mvc.*;
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
    
}

