package play.exceptions;

public class UnexpectedException extends PlayException {

    public UnexpectedException(Throwable exception) {
        super("Unexpected Error", exception);
    }

    @Override
    public String getErrorTitle() {
        return String.format("Unexpected error: %s", getCause().getClass().getSimpleName());
    }

    @Override
    public String getErrorDescription() {
        return String.format("An unexpected error occured caused by exception <strong>%s</strong>:<br/> <strong>%s</strong>", getCause().getClass().getSimpleName(), getCause().getMessage());
    }
}

