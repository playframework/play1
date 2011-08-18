package play.jobs;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import play.Invoker;
import play.Invoker.InvocationContext;
import play.Logger;
import play.Play;
import play.exceptions.JavaExecutionException;
import play.exceptions.PlayException;
import play.libs.F.Promise;
import play.libs.Time;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

/**
 * A job is an asynchronously executed unit of work
 * @param <V> The job result type (if any)
 */
public class Job<V> extends Invoker.Invocation implements Callable<V> {

    public static final String invocationType = "Job";
    
    protected ExecutorService executor;
    protected long lastRun = 0;
    protected boolean wasError = false;
    protected Throwable lastException = null;

    Date nextPlannedExecution = null;

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

    @Override
    public void _finally() {
        super._finally();
        if (executor == JobsPlugin.executor) {
            JobsPlugin.scheduleForCRON(this);
        }
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }


}
