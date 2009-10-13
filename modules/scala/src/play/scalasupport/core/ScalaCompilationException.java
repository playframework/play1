package play.scalasupport.core;

import java.util.Arrays;
import java.util.List;
import play.exceptions.*;
import play.vfs.VirtualFile;

/**
 * A scala compilation error
 */
public class ScalaCompilationException extends PlayException implements SourceAttachment {

    private VirtualFile sourceFile;
    private String problem;
    private int line;

    public ScalaCompilationException(VirtualFile sourceFile, String problem, int line) {
        super(problem);
        this.sourceFile = sourceFile;
        this.problem = problem;
        this.line = line;
    }

    @Override
    public String getErrorTitle() {
        return String.format("Compilation error");
    }

    @Override
    public String getErrorDescription() {
        return String.format("The file <strong>%s</strong> could not be compiled.\nError raised is : <strong>%s</strong>", getSourceFile(), problem);
    }
    
    @Override
    public Integer getLineNumber() {
        return line;
    }

    public List<String> getSource() {
        return Arrays.asList(sourceFile.contentAsString().split("\n"));
    }
    
    @Override
    public String getSourceFile() {
        return sourceFile.relativePath();
    }
    
    @Override
    public boolean isSourceAvailable() {
        return sourceFile != null;
    }
    
}
