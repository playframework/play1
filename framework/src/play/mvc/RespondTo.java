package play.mvc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
