package jobs;

import play.Logger;
import play.db.jpa.NoTransaction;
import play.jobs.Job;

@NoTransaction
public class SomeJob extends Job {

    @Override
    public void doJob() throws Exception {
        Thread.sleep(10000);
        Logger.info("Job is done");
    }
}
