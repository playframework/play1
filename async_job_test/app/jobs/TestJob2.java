package jobs;

import play.jobs.OnApplicationStart;

@OnApplicationStart(async=true)
public class TestJob2 extends TestJob1{

	public TestJob2(){
		jobNo = 2;
		seconds = 4;
	}
	
}