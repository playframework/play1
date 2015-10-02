package play.jobs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Run the job using a Cron expression
 * We use the Quartz CRON trigger (http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface On {
    String value();
}
