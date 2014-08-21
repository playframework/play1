package play.exceptions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.templates.Template;

/**
 * A template is missing (tag, ...)
 */
public class TemplateNotFoundException extends PlayException implements SourceAttachment {

    private String path;
    private String sourceFile;
    private List<String> source;
    private Integer line;

    public TemplateNotFoundException(String path) {
        super("Template not found : " + path);
        this.path = path;
    }
    
    public TemplateNotFoundException(String path, ApplicationClass applicationClass, Integer line) {
        this(path);
        //This occurs with using property -Dprecompiled=true and no file is found
        if (applicationClass.javaFile != null) {
            this.sourceFile = applicationClass.javaFile.relativePath();
            this.source = Arrays.asList(applicationClass.javaSource.split("\n"));
        }
        else {
            this.sourceFile = "{unknown source file.  appclass=" + applicationClass + "}";
            this.source = Collections.emptyList();
        }
        this.line = line;
    }
    
    public TemplateNotFoundException(String path, Template template, Integer line) {
        this(path);
        this.sourceFile = template.name;
        this.source = Arrays.asList(template.source.split("\n"));
        this.line = line;
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public String getErrorTitle() {
        return String.format("Template not found");
    }

    @Override
    public String getErrorDescription() {
        return String.format("The template <strong>%s</strong> does not exist.", this.path);
    }

    @Override
    public boolean isSourceAvailable() {
        return this.source != null;
    }

    @Override
    public String getSourceFile() {
        return this.sourceFile;
    }

    @Override
    public List<String> getSource() {
        return this.source;
    }

    @Override
    public Integer getLineNumber() {
        return this.line;
    }
}