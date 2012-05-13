package controllers;

import play.*;
import play.mvc.*;
import play.db.jpa.*;
import play.libs.*;
import play.libs.F.*;
import static play.libs.F.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

import models.*;

import play.jobs.*;

import play.exceptions.*;
import play.utils.*;
import play.data.validation.*;

public class WithContinuations extends Controller {
    
    @Before
    static void intercept() {
        // just to check
        Logger.info("Before continuation");
    }
    
    protected static void doAwait() {
        await(100);
    }
    
    protected static String doAwait2() {
        return await(new jobs.DoSomething(100).now());
    }

    public static void loopWithWait() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<5; i++) {
            if(i>0) sb.append(";");
            long s = System.currentTimeMillis();
            await(100);
            boolean delay = System.currentTimeMillis() - s > 100 && System.currentTimeMillis() - s < 1000;
            sb.append(i + ":" + delay);
        }
        renderText(sb);
    }
    
    public static void waitAndThenRedirect() {
        String hello = "Hello";
        String r = await(new jobs.DoSomething(100).now());
        System.out.println(play.classloading.enhancers.ControllersEnhancer.ControllerInstrumentation.isActionCallAllowed());
        sayHello(hello + " -> " + r);
    }
    
    public static void sayHello(String text) {
        render(text);
    }
    
    public static void waitFuture() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<5; i++) {
            if(i>0) sb.append(";");
            long s = System.currentTimeMillis();
            String r = await(new jobs.DoSomething(100).now());
            sb.append(i + ":[" + r + "]");
        }
        renderText(sb);
    }
    
    public static void waitWithTimeout() {
        Promise<String> task1 = new jobs.DoSomething(100).now();
        Promise<String> task2 = new jobs.DoSomething(2000).now();
        Either<List<String>,Timeout> r = await(Promise.waitEither(Promise.waitAll(task1, task2), Timeout(300)));
        
        for(Timeout t : r._2) {
            
            StringBuilder result = new StringBuilder();
            
            if(task1.isDone()) {
                result.append(" + Task1 -> " + task1.getOrNull());
            }
            
            if(task2.isDone()) {
                result.append(" + Task2 -> " + task2.getOrNull());
            }
            
            renderText("Timeout! Partial result is " + result);
        }
        
        renderText("Fail!");
    }
    
    public static void waitAll() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<2; i++) {
            if(i>0) sb.append(";");
            long s = System.currentTimeMillis();
            List<String> r = await(Promise.waitAll(new jobs.DoSomething(100).now(), new jobs.DoSomething(200).now()));
            sb.append(i + ":[" + r + "]");
        }
        renderText(sb);
    }
    
    public static void waitAny() {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<2; i++) {
            if(i>0) sb.append(";");
            long s = System.currentTimeMillis();
            String r = await(Promise.waitAny(new jobs.DoSomething(100).now(), new jobs.DoSomething(200).now()));
		    sb.append(i + ":[" + r + "]");
        }
        renderText(sb);
    }
    
    public static void withNaiveJPA() {
        User bob = new User("bob").save();
        await(100);
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
        await(100);
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
            await(10);
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
            await(10);
        }
        renderText("OK");
    }
    
    public static void streamedResult() {
        response.contentType = "text/html";
        response.writeChunk("<h1>This page should load progressively in about 3 second</h1>");
        long s = System.currentTimeMillis();
        for(int i=0; i<100; i++) {
            await(10);
            response.writeChunk("<h2>Hello " + i + "</h2>");
        }
        long t = System.currentTimeMillis() - s;
        response.writeChunk("Time: " + t + ", isOk->" + (t > 1000 && t < 10000));
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
                    boolean delay = System.currentTimeMillis() - s.get() > 100 && System.currentTimeMillis() - s.get() < 150;
                    sb.append(i + ":" + delay);
                    s.set(System.currentTimeMillis());
                    await(100, this);
                }
            }
        };
        await(100, f);
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
                    await(10, this);
                } else {
                    long t = System.currentTimeMillis() - s.get();
                    response.writeChunk("Time: " + t + ", isOk->" + (t > 1000 && t < 10000));
                }                
            }
        };
        response.writeChunk("<h1>Begin</h1>");
        await(10, callback);
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
        await(Promise.waitAny(new jobs.DoSomething(100).now(), new jobs.DoSomething(200).now()), new F.Action<String>() {
            public void invoke(String result) {
                renderText("yep -> %s", result);
            }
        });
    }
    
    public static void callbackWithResults() {
        await(Promise.waitAll(new jobs.DoSomething(100).now(), new jobs.DoSomething(200).now()), new F.Action<List<String>>() {
            public void invoke(List<String> result) {
                renderText("yep -> %s", result);
            }
        });
    }
    
    
    public static class CompletedFuture<T> implements Future {

        private final T data;
        
        public CompletedFuture(T data) {
            this.data = data;
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return true;
        }

        public T get() throws InterruptedException, ExecutionException {
            return data;
        }

        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return data;
        }
    }
    
    public static void renderTemplateWithVariablesAssignedBeforeAwait() {
        int n = 1;
        String a = "A";
        Job<String> job = new Job<String>(){
            public String doJobWithResult() {
                return "B";
            }
        };
        
        String b = await(job.now());
        
        String c = "C";
        
        await(40);
        
        String d = "D";
        
        await( new CompletedFuture<Boolean>(true));
        
        String e = "E";
        
        render(n,a,b,c,d,e);
    }
    
        public static void usingRenderArgsAndAwait() {
        renderArgs.put("a", "1");
        int size = Scope.RenderArgs.current().data.size();
        await(10);
        renderArgs.put("b", "2");
        size++;
        
        Job<String> job = new Job<String>(){
            public String doJobWithResult() {
                return "B";
            }
        };
        
        String b = await(job.now());
        
        renderArgs.put("c", "3");
        size++;
        
        await( new CompletedFuture<Boolean>(true));
        
        renderArgs.put("d", "4");
        size++;
        
        boolean res = "1234".equals(""+renderArgs.get("a")+renderArgs.get("b")+renderArgs.get("c")+renderArgs.get("d")) && size == Scope.RenderArgs.current().data.size();
        
        renderText( res );
    }
    
    public static void usingRenderArgsAndAwaitWithCallBack(String arg) {
         renderArgs.put("arg", arg);

         await("1s", new F.Action0() {

             public void invoke() {
                 renderText(renderArgs.get("arg"));
             }
         });
    }

    public static void usingRenderArgsAndAwaitWithFutureAndCallback(final String arg) {
        renderArgs.put("arg", arg);

        Promise<String> promise = new Job() {
           
 			public String doJobWithResult() throws Exception {
                return "result";
        }

        }.now();
        await(promise, new F.Action<String>() {

            public void invoke(String result) {
                renderText(result + "/" + renderArgs.get("arg"));
            }
        });
    }
    
    // This class does not use await() directly and therefor is not enhanched for Continuations
    public static class ControllerWithoutContinuations extends Controller{
        
        public static void useAwaitViaOtherClass() {
            int failCount = 0;
            try {
                WithContinuations.doAwait();
            } catch (ContinuationsException e) {
                failCount++;
            }
            
            try {
                WithContinuations.doAwait2();
            } catch (ContinuationsException e) {
                failCount++;
            }
            
            renderText("failCount: " + failCount);
        }
    }
    
    public static class ControllerWhichUsesAwaitViaInheritance extends WithContinuations {

        public static void useAwaitViaInheritance() {
            String res = WithContinuations.doAwait2();
            renderText(res != null);
        }
    }

    
    // I don't know how to test WebSocketController directly so since we only are testing that the await stuff
    // is working, we'll just call it via this regular controller - only testing that the enhancing is working ok.
    public static void useAwaitInWebSocketControllerWithContinuations() {
        WebSocketControllerWithContinuations.useAwait();
        renderText("ok");
    }
    
    // This is a WebSocketController, but since I'm not sre how to test a WebSocket,
    // I'll just call a protected static method in it doing continuations.
    // I'm doing this just to make sure that the enhancing is working for WebSocketControllers as well
    public static class WebSocketControllerWithContinuations extends WebSocketController {
        
        protected static void useAwait() {
            await(100);
        }
    }
    
    // I don't know how to test WebSocketController directly so since we only are testing that the await stuff
    // is working, we'll just call it via this regular controller - only testing that the enhancing is working ok.
    public static void useAwaitInWebSocketControllerWithoutContinuations() {
        renderText( WebSocketControllerWithoutContinuations.useAwaitViaOtherClass() );
    }


    public static class WebSocketControllerWithoutContinuations extends WebSocketController {
        
        protected static String useAwaitViaOtherClass() {
            int failCount = 0;
            try {
                WithContinuations.doAwait();
            } catch (ContinuationsException e) {
                failCount++;
            }
            
            return "failCount: " + failCount;
        }
    }
    
    public static void echoParamsAfterAwait(String a, Integer b) {
        String beforeString = "before await: " + getEchoString(a,b);
        await(1);
        String afterString = "after await: " + getEchoString(a,b);
        
        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\n" + beforeString + "\n" + afterString);
        renderText(beforeString + "\n" + afterString);
    }

    private static String getEchoString(String a, Integer b) {
        return "a: " + a + " b: " + b + " params[a]: " + Utils.join(params.getAll("a"),",") + " params[b]: " + Utils.join(params.getAll("b"), ",");
    }
    
    
    private static List<String> getErrorStringList(List<play.data.validation.Error> errorList) {
        List<String> list = new ArrayList<String>();
        for ( play.data.validation.Error e : errorList ) {
            list.add(e.getKey()+"="+e.message());
        }
        return list;
    }
    
    public static class SomeBean {
        @Required
        public String prop;
    }
    
    public static void validationAndAwait(@Required String a, Integer b) {
        validation.addError("b", "someError");
        String beforeErrors = Utils.join(getErrorStringList(validation.errors()), ",");
        await(1);
        SomeBean sb = new SomeBean();
        validation.valid(sb);
        String afterErrors = Utils.join(getErrorStringList(validation.errors()), ",");
        renderText("beforeErrors: " + beforeErrors + " afterErrors: " + afterErrors);
    }
    
    public static void paramsLocalVariableTracerAndAwait(int a) {
        int aa = a;
        await("1s");
        render(a,aa);
        
    }
    
    
}

