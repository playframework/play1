package play.jobs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A job run at application start.
 *
 * Jobs can be executed in the background if you set async == true.
 *
 * This will make your app start accepting incoming requests faster.
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OnApplicationStart {

    /**
     * set this to true if you want the job to run
     * in the background when your application starts.
     * @return true if job will be executed async on program start
     */
    boolean async() default false;
}
