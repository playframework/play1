import play.jobs.*;
import play.*;
import utils.TestInter;
import javax.inject.Inject; 

@OnApplicationStart
public class Bootstrap extends Job {
    
	@Inject static TestInter test;

    public void doJob() {
		String ret = test.printer();
		Logger.info("guice in jobs:"+ret);
    }
    
}
