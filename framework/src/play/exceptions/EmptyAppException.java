package play.exceptions;

public class EmptyAppException extends PlayException {

    @Override
    public String getErrorTitle() {
        return "Empty application";
    }

    @Override
    public String getErrorDescription() {
        return "No routes found.";
    }

}
