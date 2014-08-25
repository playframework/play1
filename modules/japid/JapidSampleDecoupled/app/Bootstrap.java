import java.io.IOException;

import cn.bran.japid.util.JapidFlags;
import cn.bran.play.JapidPlayRenderer;

import play.Play;
import play.Play.Mode;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
 
@OnApplicationStart
public class Bootstrap extends Job {
 
    public void doJob() {
    	System.out.println("bootstrap called");
    	JapidFlags.setLogLevelDebug();
    }
}