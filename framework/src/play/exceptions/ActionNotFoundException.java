package play.exceptions;

public class ActionNotFoundException extends PlayException {

    private String action;

    public ActionNotFoundException(String action, Throwable cause) {
        super(String.format("Action %s not found", action.startsWith("controllers.") ? action.substring(12) : action), cause);
        this.action = action.startsWith("controllers.") ? action.substring(12) : action;
    }

    public String getAction() {
        return action;
    }

    @Override
    public String getErrorTitle() {
        return "Action not found";
    }

    @Override
    public String getErrorDescription() {
        return String.format(
                "Action <strong>%s</strong> could not be found. Error raised is <strong>%s</strong>",
                action,
                getCause() instanceof ClassNotFoundException ? "ClassNotFound: " + getCause().getMessage() : getCause().getMessage()
        );
    }
}
