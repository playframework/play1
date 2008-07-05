package play.jobs;

import java.util.ArrayList;
import java.util.List;
import org.quartz.Job;
import org.quartz.JobExecutionException;
import play.PlayPlugin;
import play.Play;
import play.exceptions.JavaExecutionException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;

public class JobsPlugin extends PlayPlugin {

    @Override
    public void onApplicationStart() {
        List<Class> jobs = new ArrayList();
        for(Class clazz : Play.classloader.getAllClasses()) {
            if(Job.class.isAssignableFrom(clazz)) {
                jobs.add(clazz);                
            }
        }
        // At start
        for(final Class clazz : jobs) {
            // @AtApplicationStart
            if(clazz.isAnnotationPresent(AtApplicationStart.class)) {
                try {
                    ((Job)clazz.newInstance()).execute(null);
                } catch(InstantiationException e) {
                    throw new UnexpectedException(e);
                } catch(IllegalAccessException e) {
                    throw new UnexpectedException(e);
                } catch(JobExecutionException e) {
                    throw new UnexpectedException(e);
                } catch(Exception ex) {
                    StackTraceElement element = PlayException.getInterestingStrackTraceElement(ex);
                    if(element != null) {
                        throw new JavaExecutionException(Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber(), ex);
                    }
                    throw new UnexpectedException(ex);
                }
            }
        }
    }

}
