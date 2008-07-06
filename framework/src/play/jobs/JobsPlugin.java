package play.jobs;

import java.util.ArrayList;
import java.util.List;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import play.PlayPlugin;
import play.Play;
import play.exceptions.JavaExecutionException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;

public class JobsPlugin extends PlayPlugin {

    static Scheduler scheduler;
    static List<Class> crons = new ArrayList<Class>();

    @Override
    public void onApplicationStart() {
        List<Class> jobs = new ArrayList();
        for (Class clazz : Play.classloader.getAllClasses()) {
            if (Job.class.isAssignableFrom(clazz)) {
                jobs.add(clazz);
            }
        }
        if (scheduler == null) {
            try {
                scheduler = new org.quartz.impl.StdSchedulerFactory().getScheduler();
                scheduler.start();
            } catch (Exception e) {
                throw new UnexpectedException("Cannot start scheduler");
            }
        }
        // At start
        for (final Class clazz : jobs) {
            // @OnApplicationStart
            if (clazz.isAnnotationPresent(OnApplicationStart.class)) {
                try {
                    ((Job) clazz.newInstance()).doJob();
                } catch (InstantiationException e) {
                    throw new UnexpectedException("Job could not be instantiated", e);
                } catch (IllegalAccessException e) {
                    throw new UnexpectedException("Job could not be instantiated", e);
                } catch (Exception ex) {
                    StackTraceElement element = PlayException.getInterestingStrackTraceElement(ex);
                    if (element != null) {
                        throw new JavaExecutionException(Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber(), ex);
                    }
                    throw new UnexpectedException(ex);
                }
            }
            // @On
            if (clazz.isAnnotationPresent(On.class)) {
                String cron = ((On) (clazz.getAnnotation(On.class))).value();
                try {
                    CronTrigger trigger = new CronTrigger(clazz.getName(), "play", cron);
                    JobDetail jobDetail = new JobDetail(clazz.getName(), null, clazz);
                    scheduler.scheduleJob(jobDetail, trigger);
                    crons.add(clazz);
                } catch (Exception ex) {
                    throw new UnexpectedException(ex);
                }
            }
        }
    }

    @Override
    public void onApplicationStop() {
        try {
            for (Class clazz : crons) {
                scheduler.unscheduleJob(clazz.getName(), "play");
            }
        } catch (SchedulerException ex) {
            throw new UnexpectedException("Cannot shitdow scheduler", ex);
        }
    }
}
