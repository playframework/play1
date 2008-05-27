package play.exceptions;

public class ActionNotFoundException extends PlayException {

    private String action;

    public ActionNotFoundException(String action) {
        super(String.format("Action %s not found", action));
        this.action = action;
    }
    
    public ActionNotFoundException(String action, Throwable cause) {
        super(String.format("Action %s not found", action), cause);
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    @Override
    public String getErrorTitle() {
        return String.format("Action %s not found", action);
    }

    @Override
    public String getErrorDescription() {
        return String.format("Action <strong>%s</strong> could not be found", action);
    }
}
