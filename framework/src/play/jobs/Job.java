package play.jobs;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import play.Play;
import play.Invoker;
import play.Logger;
import play.exceptions.JavaExecutionException;
import play.exceptions.PlayException;
import play.libs.Time;

public class Job<V> extends Invoker.Invocation implements Callable<V> {

    protected ExecutorService executor;
    protected long lastRun = 0;
    protected boolean wasError = false;

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
    public Future<V> now() {
        return JobsPlugin.executor.submit((Callable) this);
    }

    /**
     * Start this job in several seconds
     * @return the job completion
     */
    public Future<V> in(String delay) {
        return in(Time.parseDuration(delay));
    }

    /**
     * Start this job in several seconds
     * @return the job completion
     */
    public Future in(int seconds) {
        return JobsPlugin.executor.schedule((Callable) this, seconds, TimeUnit.SECONDS);
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
        try {
            if (init()) {
                before();
                V result = null;
                try {
                    lastRun = System.currentTimeMillis();
                    result = doJobWithResult();
                    wasError = false;
                } catch (Exception e) {
                    wasError = true;
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
