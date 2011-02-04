package jobs;

import play.jobs.OnApplicationStart;
import play.jobs.Job;
import java.lang.Exception;
import play.Logger;

@OnApplicationStart(async=false)
public class TestJob1 extends Job{
	
	protected int jobNo = 1;
	protected int seconds = 5;
	
	
	
	@Override
	public void doJob() throws Exception{
		
		Logger.info("TestJob "+jobNo+" sleeping " + seconds + " seconds");
		Thread.sleep(seconds * 1000);
		Logger.info("TestJob "+jobNo+" Done");
	}
	
	
	
	
}