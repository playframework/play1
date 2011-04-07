package play.jobs;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.LazyLoader;
import play.Invoker;
import play.Invoker.InvocationContext;
import play.Logger;
import play.Play;
import play.exceptions.JavaExecutionException;
import play.exceptions.PlayException;
import play.libs.Time;
import play.libs.F.Promise;
import play.mvc.Http;
import play.utils.FakeRequestCreator;

/**
 * A job is an asynchronously executed unit of work
 * @param <V> The job result type (if any)
 */
public class Job<V> extends Invoker.Invocation implements Callable<V> {

    public static final String invocationType = "Job";
    public static final String applicationBaseUrl_configPropertyName = "application.baseUrl";

    protected ExecutorService executor;
    protected long lastRun = 0;
    protected boolean wasError = false;
    protected Throwable lastException = null;

    // [#707] Used to store original current before invoking plugins
    private Http.Request originalCurrentRequest = null;

    @Override
    public InvocationContext getInvocationContext() {
        return new InvocationContext(invocationType, this.getClass().getAnnotations());
    }
    
    /**
     * Here you do the job
     */
    public void doJob() throws Exception {
    }

    /**
     * Here you do the job and return a result
     */
    public V doJobWithResult() throws Exception {
        doJob();
        return null;
    }

    @Override
    public void execute() throws Exception {
    }

    /**
     * Start this job now (well ASAP)
     * @return the job completion
     */
    public Promise<V> now() {
        final Promise<V> smartFuture = new Promise<V>();
        JobsPlugin.executor.submit(new Callable<V>() {
            public V call() throws Exception {
                V result =  Job.this.call();
                smartFuture.invoke(result);
                return result;
            }
            
        });

        return smartFuture;
    }

    /**
     * Start this job in several seconds
     * @return the job completion
     */
    public Promise<V> in(String delay) {
        return in(Time.parseDuration(delay));
    }

    /**
     * Start this job in several seconds
     * @return the job completion
     */
    public Promise<V> in(int seconds) {
        final Promise<V> smartFuture = new Promise<V>();

        JobsPlugin.executor.schedule(new Callable<V>() {

            public V call() throws Exception {
                V result =  Job.this.call();
                smartFuture.invoke(result);
                return result;
            }

        }, seconds, TimeUnit.SECONDS);

        return smartFuture;
    }

    /**
     * Run this job every n seconds
     */
    public void every(String delay) {
        every(Time.parseDuration(delay));
    }

    /**
     * Run this job every n seconds
     */
    public void every(int seconds) {
        JobsPlugin.executor.scheduleWithFixedDelay(this, seconds, seconds, TimeUnit.SECONDS);
    }

    /**
     * [#707] make sure Request.current always is null when Plugins are called from jobs
     */
    private void storeAndClearRequestCurrent() {
        originalCurrentRequest = Http.Request.current.get();
        Http.Request.current.set(null);
    }

    /**
     * [#707] restore the original value of Request.current
     */
    private void restoreRequestCurrent() {
        Http.Request.current.set(originalCurrentRequest);
    }
    
    // Customize Invocation
    @Override
    public void onException(Throwable e) {
        wasError = true;
        lastException = e;
        try {
            super.onException(e);
        } catch(Throwable ex) {
            Logger.error(ex, "Error during job execution (%s)", this);
        }
    }

    @Override
    public boolean init() {
        storeAndClearRequestCurrent();
        return super.init();
    }

    @Override
    public void before() {
        super.before();
    }

    @Override
    public void after() {
        super.after();
    }

    @Override
    public void onSuccess() throws Exception {
        super.onSuccess();
    }

    @Override
    public void run() {
        call();
    }

    public V call() {
        Monitor monitor = null;
        try {
            if (init()) {
                before();
                V result = null;

                try {
                    lastException = null;
                    lastRun = System.currentTimeMillis();
                    monitor = MonitorFactory.start(getClass().getName()+".doJob()");

                    //Hack to enable template rendering with urls in jobs
                    if( Http.Request.current.get() == null) {
                        createFakeRequest();
                    }

                    result = doJobWithResult();
                    monitor.stop();
                    monitor = null;
                    wasError = false;
                } catch (PlayException e) {
                    throw e;
                } catch (Exception e) {
                    StackTraceElement element = PlayException.getInterestingStrackTraceElement(e);
                    if (element != null) {
                        throw new JavaExecutionException(Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber(), e);
                    }
                    throw e;
                }
                after();
                return result;
            }
        } catch (Throwable e) {
            onException(e);
        } finally {
            if(monitor != null) {
                monitor.stop();
            }
            _finally();
        }
        return null;
    }

    /**
     * If rendering with templates in a job, some template-operations require
     * a current Request-object, eg: @@{...}}.
     * This method creates a fake one based on a configurable baseUrl in application.conf
     */
    private static void createFakeRequest() {

        // We want this to fail ONLY if user is actually trying to resolve urls and the
        // configuration is missing..
        // To archieve this we create a lazy proxy for our FakeRequestCreator.
        // Our initialization is executed the first time any code tries to access our request..
        final Http.Request lazyRequest = (Http.Request)Enhancer.create(Http.Request.class, new LazyLoader() {
            public Object loadObject() throws Exception {
                // someone is trying to access our Request-object. We must
                // initialize it..
                String applicationBaseUrl = Play.configuration.getProperty(
                        applicationBaseUrl_configPropertyName);

                if( applicationBaseUrl == null ) {
                    throw new RuntimeException("Since you are probably trying to resolve urls from inside a Job, " +
                            "you have to configure '"+applicationBaseUrl_configPropertyName+"' in application.conf");
                }

                return FakeRequestCreator.createFakeRequestFromBaseUrl(applicationBaseUrl);

            }
        });

        Http.Request.current.set(lazyRequest);

    }



    @Override
    public void _finally() {
        super._finally();
        if (executor == JobsPlugin.executor) {
            JobsPlugin.scheduleForCRON(this);
        }
        restoreRequestCurrent();
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }


}
