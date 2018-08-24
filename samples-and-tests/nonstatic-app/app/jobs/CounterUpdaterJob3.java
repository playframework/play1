package jobs;

import play.jobs.Job;
import play.jobs.On;
import play.jobs.OnApplicationStart;
import services.Counter;

import javax.inject.Inject;

@On("cron.counterUpdater")
public class CounterUpdaterJob3 extends Job<Void> {
  @Inject
  private Counter counter;

  @Override
  public void doJob() {
    counter.inc("counter.job.on");
  }
}
