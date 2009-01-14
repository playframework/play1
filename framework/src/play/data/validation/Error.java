package play.data.validation;

import play.i18n.Messages;

public class Error {

    String message;
    String key;
    String[] variables;

    Error(String key, String message, String[] variables) {
        this.message = message;
        this.key = key;
        this.variables = variables;
    }
    
    public String message() {
        return message(key);
    }
    
    public String getKey() {
        return key;
    }
    
    public String message(String key) {
        key = Messages.get(key);
        Object[] args = new Object[variables.length + 1];
        System.arraycopy(variables, 0, args, 1, variables.length);
        args[0] = key;
        return Messages.get(message, (Object[])args);
    }

    @Override
    public String toString() {
        return message();
    }

}
