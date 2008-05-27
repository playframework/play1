package play.exceptions;

public class TemplateNotFoundException extends PlayException {

    private String path;

    public TemplateNotFoundException(String path) {
        super("Template not found : " + path);
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String getErrorTitle() {
        return String.format("Template not found");
    }

    @Override
    public String getErrorDescription() {
        return String.format("The <strong>%s</strong> template does not exist.", path);
    }
}