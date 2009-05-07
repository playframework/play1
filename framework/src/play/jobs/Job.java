package play.jobs;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import play.Invoker;
import play.Play;
import play.exceptions.JavaExecutionException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;

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
                try {
                    job.doJob();
                } catch(Exception e) {
                    StackTraceElement element = PlayException.getInterestingStrackTraceElement(e);
                    if (element != null) {
                        throw new JavaExecutionException(Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber(), e);
                    }
                    throw new UnexpectedException(e);
                }
            }
        });
    }

    public abstract void doJob();
}
