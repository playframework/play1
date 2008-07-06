package play.jobs;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import play.Invoker;
import play.Play;

public abstract class Job implements org.quartz.Job {

    public void execute(final JobExecutionContext ctx) throws JobExecutionException {
        final long start = Play.startedAt;
        final Job job = this;
        Invoker.invokeInThread(new Invoker.Invocation() {
            public void execute() throws Exception {
                boolean hasRestarted = (Play.startedAt != start);
                if (hasRestarted) {
                    return;
                }
                job.doJob();
            }
        });
    }

    public abstract void doJob();
}
