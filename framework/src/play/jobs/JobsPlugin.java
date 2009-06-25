package play.jobs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.libs.Time;

public class JobsPlugin extends PlayPlugin {

    public static ScheduledThreadPoolExecutor executor = null;
    

    static {
    }

    @Override
    public void afterApplicationStart() {
        List<Class> jobs = new ArrayList();
        for (Class clazz : Play.classloader.getAllClasses()) {
            if (Job.class.isAssignableFrom(clazz)) {
                jobs.add(clazz);
            }
        }
        for (final Class clazz : jobs) {
            // @OnApplicationStart
            if (clazz.isAnnotationPresent(OnApplicationStart.class)) {
                try {
                    ((Job) clazz.newInstance()).run();
                } catch (InstantiationException e) {
                    throw new UnexpectedException("Job could not be instantiated", e);
                } catch (IllegalAccessException e) {
                    throw new UnexpectedException("Job could not be instantiated", e);
                } catch (Exception ex) {
                    if (ex instanceof PlayException) {
                        throw (PlayException) ex;
                    }
                    throw new UnexpectedException(ex);
                }
            }
            // @On
            if (clazz.isAnnotationPresent(On.class)) {
                try {
                    scheduleForCRON((Job) clazz.newInstance());
                } catch (InstantiationException ex) {
                    throw new UnexpectedException("Cannot instanciate Job " + clazz.getName());
                } catch (IllegalAccessException ex) {
                    throw new UnexpectedException("Cannot instanciate Job " + clazz.getName());
                }
            }
            // @Every
            if (clazz.isAnnotationPresent(Every.class)) {
                try {
                    Job job = (Job) clazz.newInstance();
                    String value = ((Every) (job.getClass().getAnnotation(Every.class))).value();
                    executor.scheduleWithFixedDelay(job, Time.parseDuration(value), Time.parseDuration(value), TimeUnit.SECONDS);
                } catch (InstantiationException ex) {
                    throw new UnexpectedException("Cannot instanciate Job " + clazz.getName());
                } catch (IllegalAccessException ex) {
                    throw new UnexpectedException("Cannot instanciate Job " + clazz.getName());
                }
            }
        }
    }

    @Override
    public void onApplicationStart() {
        int core = Integer.parseInt(Play.configuration.getProperty("play.jobs.pool", "10"));
        executor = new ScheduledThreadPoolExecutor(core, new ThreadPoolExecutor.AbortPolicy());
    }

    public static void scheduleForCRON(Job job) {
        if (job.getClass().isAnnotationPresent(On.class)) {
            String cron = ((On) (job.getClass().getAnnotation(On.class))).value();
            if (cron.startsWith("cron.")) {
                cron = Play.configuration.getProperty(cron);
            }
            if (cron != null && !cron.equals("")) {
                try {
                    Date now = new Date();
                    Date nextDate = Time.parseCRONExpression(cron);
                    long delay = nextDate.getTime() - now.getTime();
                    executor.schedule((Callable)job, delay, TimeUnit.MILLISECONDS);
                    job.executor = executor;
                } catch (Exception ex) {
                    throw new UnexpectedException(ex);
                }
            } else {
                Logger.info("Skipping job %s, cron expression is not defined", job.getClass().getName());
            }
        }
    }

    @Override
    public void onApplicationStop() {
        executor.shutdownNow();
        executor.getQueue().clear();
    }
}
