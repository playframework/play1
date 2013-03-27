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
import play.libs.Expression;
import play.libs.Time;
import play.libs.Time.CronExpression;
import play.utils.Java;
import play.utils.PThreadFactory;

public class JobsPlugin extends PlayPlugin {

    public static ScheduledThreadPoolExecutor executor = null;
    public static List<Job> scheduledJobs = null;

    @Override
    public String getStatus() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        if (executor == null) {
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
        if (!scheduledJobs.isEmpty()) {
            out.println();
            out.println("Scheduled jobs ("+scheduledJobs.size()+"):");
            out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~");
            for (Job job : scheduledJobs) {
                out.print(job.getClass().getName());
                if (job.getClass().isAnnotationPresent(OnApplicationStart.class) && !(job.getClass().isAnnotationPresent(On.class) || job.getClass().isAnnotationPresent(Every.class))) {
                    OnApplicationStart appStartAnnotation = job.getClass().getAnnotation(OnApplicationStart.class);
                    out.print(" run at application start" + (appStartAnnotation.async()?" (async)" : "") + ".");
                }

                if( job.getClass().isAnnotationPresent(On.class)) {

                    String cron = job.getClass().getAnnotation(On.class).value();
                    if (cron != null && cron.startsWith("cron.")) {
                        cron = Play.configuration.getProperty(cron);
                    }
                    out.print(" run with cron expression " + cron + ".");
                }
                if (job.getClass().isAnnotationPresent(Every.class)) {
                    out.print(" run every " + job.getClass().getAnnotation(Every.class).value() + ".");
                }
                if (job.lastRun > 0) {
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
        if (!executor.getQueue().isEmpty()) {
            out.println();
            out.println("Waiting jobs:");
            out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            for (Object o : executor.getQueue()) {
                ScheduledFuture task = (ScheduledFuture)o;
                out.println(Java.extractUnderlyingCallable((FutureTask)task) + " will run in " + task.getDelay(TimeUnit.SECONDS) + " seconds");        
            }
        }
        return sw.toString();
    }


    @Override
    public void afterApplicationStart() {
        List<Class<?>> jobs = new ArrayList<Class<?>>();
        for (Class clazz : Play.classloader.getAllClasses()) {
            if (Job.class.isAssignableFrom(clazz)) {
                jobs.add(clazz);
            }
        }
        scheduledJobs = new ArrayList<Job>();
        for (final Class<?> clazz : jobs) {
            // @OnApplicationStart
            if (clazz.isAnnotationPresent(OnApplicationStart.class)) {
                //check if we're going to run the job sync or async
                OnApplicationStart appStartAnnotation = clazz.getAnnotation(OnApplicationStart.class);
                if( !appStartAnnotation.async()) {
                    //run job sync
                    try {
                        Job<?> job = ((Job<?>) clazz.newInstance());
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
                } else {
                    //run job async
                    try {
                        Job<?> job = ((Job<?>) clazz.newInstance());
                        scheduledJobs.add(job);
                        //start running job now in the background
                        @SuppressWarnings("unchecked")
                        Callable<Job> callable = (Callable<Job>)job;
                        executor.submit(callable);
                    } catch (InstantiationException ex) {
                        throw new UnexpectedException("Cannot instanciate Job " + clazz.getName());
                    } catch (IllegalAccessException ex) {
                        throw new UnexpectedException("Cannot instanciate Job " + clazz.getName());
                    }
                }
            }

            // @On
            if (clazz.isAnnotationPresent(On.class)) {
                try {
                    Job<?> job = ((Job<?>) clazz.newInstance());
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
                    String value = job.getClass().getAnnotation(Every.class).value();
                    if (value.startsWith("cron.")) {
                        value = Play.configuration.getProperty(value);
                    }
                    value = Expression.evaluate(value, value).toString();
                    if(!"never".equalsIgnoreCase(value)){
                        executor.scheduleWithFixedDelay(job, Time.parseDuration(value), Time.parseDuration(value), TimeUnit.SECONDS);
                    }
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
        executor = new ScheduledThreadPoolExecutor(core, new PThreadFactory("jobs"), new ThreadPoolExecutor.AbortPolicy());
    }

    public static <V> void scheduleForCRON(Job<V> job) {
        if (!job.getClass().isAnnotationPresent(On.class)) {
            return;
        }
        String cron = job.getClass().getAnnotation(On.class).value();
        if (cron.startsWith("cron.")) {
            cron = Play.configuration.getProperty(cron);
        }
        cron = Expression.evaluate(cron, cron).toString();
        if (cron == null || "".equals(cron) || "never".equalsIgnoreCase(cron)) {
            Logger.info("Skipping job %s, cron expression is not defined", job.getClass().getName());
            return;
        }
        try {
            Date now = new Date();
            cron = Expression.evaluate(cron, cron).toString();
            CronExpression cronExp = new CronExpression(cron);
            Date nextDate = cronExp.getNextValidTimeAfter(now);
            if (nextDate == null) {
                Logger.warn("The cron expression for job %s doesn't have any match in the future, will never be executed", job.getClass().getName());
                return;
            }
            if (nextDate.equals(job.nextPlannedExecution)) {
                // Bug #13: avoid running the job twice for the same time
                // (happens when we end up running the job a few minutes before the planned time)
                Date nextInvalid = cronExp.getNextInvalidTimeAfter(nextDate);
                nextDate = cronExp.getNextValidTimeAfter(nextInvalid);
            }
            job.nextPlannedExecution = nextDate;
            executor.schedule((Callable<V>)job, nextDate.getTime() - now.getTime(), TimeUnit.MILLISECONDS);
            job.executor = executor;
        } catch (Exception ex) {
            throw new UnexpectedException(ex);
        }
    }

    @Override
    public void onApplicationStop() {
        
        List<Class> jobs = Play.classloader.getAssignableClasses(Job.class);
        
        for (final Class clazz : jobs) {
            // @OnApplicationStop
            if (clazz.isAnnotationPresent(OnApplicationStop.class)) {
                try {
                    Job<?> job = ((Job<?>) clazz.newInstance());
                    scheduledJobs.add(job);
                    job.run();
                    if (job.wasError) {
                        if (job.lastException != null) {
                            throw job.lastException;
                        }
                        throw new RuntimeException("@OnApplicationStop Job has failed");
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
        }
        
        executor.shutdownNow();
        executor.getQueue().clear();
    }
}
