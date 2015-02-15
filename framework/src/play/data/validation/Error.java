package play.data.validation;

import play.i18n.Messages;

/**
 * A validation error.
 */
public class Error {

    String message;
    String key;
    String[] variables;
    int severity = 0;

    public Error(String key, String message, String[] variables) {
        this(key, message, variables, 0);
    }

    public Error(String key, String message, String[] variables, int severity) {
        this.message = message;
        this.key = key;
        this.variables = variables;
        this.severity = severity;
    }
    
    /**
     * @return The translated message
     */
    public String message() {
        return message(key);
    }
    
    /**
     * @return The field name
     */
    public String getKey() {
        return key;
    }
    
    /**
     * @param key Alternate field name (default to java variable name)
     * @return The translated message
     */
    public String message(String key) {
        key = Messages.get(key);
        Object[] args = new Object[variables.length + 1];
        System.arraycopy(variables, 0, args, 1, variables.length);
        args[0] = key;
        return Messages.get(message, args);
    }

    @Override
    public String toString() {
        return message();
    }

    public int getSeverity() {
        return severity;
    }

    public String getMessageKey() {
        return message;
    }

}
