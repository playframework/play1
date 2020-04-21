package jobs;

import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import services.Counter;

import javax.inject.Inject;

@OnApplicationStart
public class CounterUpdaterJob2 extends Job<Void> {
  @Inject
  private Counter counter;

  @Override
  public void doJob() {
    counter.inc("counter.job.onApplicationStart");
  }
}
