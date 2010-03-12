package play.jobs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.utils.Java;
import play.libs.Time;

public class JobsPlugin extends PlayPlugin {

    public static ScheduledThreadPoolExecutor executor = null;
    public static List<Job> scheduledJobs = null;

    @Override
    public String getStatus() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        if(executor == null) {
            out.println("Jobs execution pool:");
            out.println("~~~~~~~~~~~~~~~~~~~");
            out.println("(not yet started)");
            return sw.toString();
        }
        out.println("Jobs execution pool:");
        out.println("~~~~~~~~~~~~~~~~~~~");
        out.println("Pool size: " + executor.getPoolSize());
        out.println("Active count: " + executor.getActiveCount());
        out.println("Scheduled task count: " + executor.getTaskCount());
        out.println("Queue size: " + executor.getQueue().size());
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        if(!scheduledJobs.isEmpty()) {
            out.println();
            out.println("Scheduled jobs ("+scheduledJobs.size()+"):");
            out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~");
            for(Job job : scheduledJobs) {
                out.print(job.getClass().getName());
                if(job.getClass().isAnnotationPresent(OnApplicationStart.class)) {
                    out.print(" run at application start.");
                }
                if(job.getClass().isAnnotationPresent(On.class)) {
                    out.print(" run with cron expression " + ((On)job.getClass().getAnnotation(On.class)).value() + ".");
                }
                if(job.getClass().isAnnotationPresent(Every.class)) {
                    out.print(" run every " + ((Every)job.getClass().getAnnotation(Every.class)).value() + ".");
                }
                if(job.lastRun > 0) {
                    out.print(" (last run at " + df.format(new Date(job.lastRun)));
                    if(job.wasError) {
                        out.print(" with error)");
                    } else {
                        out.print(")");
                    }
                } else {
                    out.print(" (has never run)");
                }
                out.println();
            }
        }
        if(!executor.getQueue().isEmpty()) {
            out.println();
            out.println("Waiting jobs:");
            out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            for(Object o : executor.getQueue()) {
                ScheduledFuture task = (ScheduledFuture)o;
                out.println(Java.extractUnderlyingCallable((FutureTask)task) + " will run in " + task.getDelay(TimeUnit.SECONDS) + " seconds");        
            }
        }
        return sw.toString();
    }


    @Override
    public void afterApplicationStart() {
        List<Class> jobs = new ArrayList<Class>();
        for (Class clazz : Play.classloader.getAllClasses()) {
            if (Job.class.isAssignableFrom(clazz)) {
                jobs.add(clazz);
            }
        }
        scheduledJobs = new ArrayList<Job>();
        for (final Class clazz : jobs) {
            // @OnApplicationStart
            if (clazz.isAnnotationPresent(OnApplicationStart.class)) {
                try {
                    Job job = ((Job) clazz.newInstance());
                    scheduledJobs.add(job);
                    job.run();     
                    if(job.wasError) {
                        if(job.lastException != null) {
                            throw job.lastException;
                        }
                        throw new RuntimeException("@OnApplicationStart Job has failed");
                    }
                } catch (InstantiationException e) {
                    throw new UnexpectedException("Job could not be instantiated", e);
                } catch (IllegalAccessException e) {
                    throw new UnexpectedException("Job could not be instantiated", e);
                } catch (Throwable ex) {
                    if (ex instanceof PlayException) {
                        throw (PlayException) ex;
                    }
                    throw new UnexpectedException(ex);
                }
            }
            // @On
            if (clazz.isAnnotationPresent(On.class)) {
                try {
                    Job job = ((Job) clazz.newInstance());
                    scheduledJobs.add(job);
                    scheduleForCRON(job);
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
                    scheduledJobs.add(job);
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
