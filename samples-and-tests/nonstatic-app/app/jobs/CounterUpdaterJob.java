package jobs;

import play.jobs.Every;
import play.jobs.Job;
import play.jobs.On;
import services.Counter;

import javax.inject.Inject;

@Every("1s")
public class CounterUpdaterJob extends Job<Void> {
  @Inject
  private Counter counter;

  @Override
  public void doJob() {
    counter.inc("counter.job.every");
  }
}
