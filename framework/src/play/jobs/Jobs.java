package play.jobs;

import java.util.concurrent.atomic.AtomicInteger;
import org.quartz.JobDetail;
import play.exceptions.UnexpectedException;

public class Jobs {

    static AtomicInteger jobID = new AtomicInteger();

    public static String triggerJob(Class<? extends Job> jobClass) {
        try {
            String name = "job" + jobID.getAndIncrement();
            JobDetail jobDetail = new JobDetail(name, "play", jobClass);
            JobsPlugin.scheduler.addJob(jobDetail, true);
            JobsPlugin.scheduler.triggerJob(jobDetail.getName(), "play");
            return name;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
}
