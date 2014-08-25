import java.io.IOException;

import cn.bran.japid.util.JapidFlags;
import play.jobs.Job;
 
//@OnApplicationStart
public class Bootstrap extends Job {
 
    public void doJob() {
    	try {
//			MyMemClient.init();
    		JapidFlags.setLogLevelInfo();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
 
}