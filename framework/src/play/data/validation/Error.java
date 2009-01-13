package play.data.validation;

import play.i18n.Messages;

public class Error {

    String message;
    String key;

    Error(String key, String message) {
        this.message = message;
        this.key = key;
    }
    
    public String getMessage() {
        return Messages.get(message, key);
    }
    
    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return getMessage();
    }

}
