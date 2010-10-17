package play.mvc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark this method as @Before interceptor
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RespondTo { 
    
    String[] value();
    
}
