package play.exceptions;

import java.util.Map;
import java.util.List;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.templates.Template;
import java.util.Arrays;

/**
 * No route found (during reverse routing)
 */
public class NoRouteFoundException extends PlayException implements SourceAttachment {

    String file;
    String action;
    Map<String, Object> args;
    String sourceFile;
    List<String> source;
    Integer line;

    public NoRouteFoundException(String file) {
        super("No route found");
        this.file = file;
    }

    public NoRouteFoundException(String file, ApplicationClass applicationClass, Integer line) {
        this(file);
        this.sourceFile = applicationClass.javaFile.relativePath();
        this.source = Arrays.asList(applicationClass.javaSource.split("\n"));
        this.line = line;
    }

    public NoRouteFoundException(String file, Template template, Integer line) {
        this(file);
        this.sourceFile = template.name;
        this.source = Arrays.asList(template.source.split("\n"));
        this.line = line;
    }
    
    public NoRouteFoundException(String action, Map<String, Object> args) {
        super("No route found");
        this.action = action;
        this.args = args;
        if(this.action.startsWith("controllers.")) {
            this.action = this.action.substring(12);
        }
    } 
    
    public NoRouteFoundException(String action, Map<String, Object> args, ApplicationClass applicationClass, Integer line) {
        this(action, args);
        this.sourceFile = applicationClass.javaFile.relativePath();
        this.source = Arrays.asList(applicationClass.javaSource.split("\n"));
        this.line = line;
    }
    
    public NoRouteFoundException(String action, Map<String, Object> args, Template template, Integer line) {
        this(action, args);
        this.sourceFile = template.name;
        this.source = Arrays.asList(template.source.split("\n"));
        this.line = line;
    }

    public String getAction() {
        return action;
    }

    public Map<String, Object> getArgs() {
        return args;
    }    

    @Override
    public String getErrorTitle() {
        return "No route found";
    }

    @Override
    public String getErrorDescription() {
        if(file != null) {
            return String.format("No route able to display file <strong>%s</strong> was found.", file);
        }
        if(args == null) {
            return String.format("No route able to invoke action <strong>%s</strong> was found.", action);
        }
        return String.format("No route able to invoke action <strong>%s</strong> with arguments <strong>%s</strong> was found.", action, args);
    }
    
    @Override
    public boolean isSourceAvailable() {
        return source != null;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getFile() {
        return file;
    }


    public List<String> getSource() {
        return source;
    }

    public Integer getLineNumber() {
        return line;
    }

}
