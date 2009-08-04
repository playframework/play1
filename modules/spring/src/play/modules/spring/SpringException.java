package play.modules.spring;

import play.exceptions.PlayException;

public class SpringException extends PlayException {

    public String getErrorDescription() {
        return "The Spring application context is not started.";
    }

    public String getErrorTitle() {
        return "Spring context is not started !";
    }
    
}