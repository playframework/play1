package play.mvc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation used to specify which formats a Controller or Action should respond to automatically
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RespondTo { 
    
    /**
     * Responds to these formats automatically
     */
    String[] value();
    
}
