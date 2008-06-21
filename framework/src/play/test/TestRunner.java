package play.test;

import java.io.File;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import play.Play;
import play.Logger;
import play.vfs.VirtualFile;

public class TestRunner extends RunListener {
    
    public static void main(String[] args) {
        
        File root = new File(System.getProperty("application.path"));
        Play.init(root, "test");  
                
        JUnitCore junit = new JUnitCore();
        junit.addListener(new TestRunner());
        boolean allOk = true;
        
        VirtualFile testPath = Play.getFile("test/application");
        for(Class testClass : Play.classloader.getAllClasses(testPath)) {
            Logger.info("");
            Logger.info("Running %s ...", testClass.getSimpleName());
            Result result = junit.run(testClass);
            if(result.wasSuccessful()) {
                Logger.info("OK", result.getRunCount());
            } else {
                Logger.error("FAILED. %s test%s failed", result.getFailureCount(), result.getFailureCount()>1 ? "s have" : " has");
            }
            Logger.info("");  
            allOk = allOk && result.wasSuccessful();
        }
        
        if(allOk) {
            Logger.info("All tests are OK"); 
        } else {
            Logger.error("Tests have failed"); 
        }
        
        Play.stop();
        
    }   
    
    // ~~~~~~ Run listener
    
    @Override
    public void testStarted(Description description) throws Exception {
        Logger.info("    - %s", description.getDisplayName());
        lastTestHasFailed = false;
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        Logger.info("    ! %s", failure.getMessage() == null ? "Oops" : failure.getMessage());
        if(!(failure.getException() instanceof AssertionError)) {
            Logger.error(failure.getException(), "    ! Exception raised is");
        }
        lastTestHasFailed = true;
    }

    @Override
    public void testFinished(Description arg0) throws Exception {
        if(lastTestHasFailed) {
            Logger.info("");
        }
    }
    
    boolean lastTestHasFailed = false;

}
