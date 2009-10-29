package play.exceptions;

import java.util.Arrays;
import java.util.List;
import play.vfs.VirtualFile;

/**
 * A java compilation error
 */
public class CompilationException extends PlayException implements SourceAttachment {

    private String problem;
    private VirtualFile source;
    private Integer line;

    public CompilationException(String problem) {
        super(problem);
        this.problem = problem;
    }

    public CompilationException(VirtualFile source, String problem, int line) {
        super(problem);
        this.problem = problem;
        this.line = line;
        this.source = source;
    }

    @Override
    public String getErrorTitle() {
        return String.format("Compilation error");
    }

    @Override
    public String getErrorDescription() {
        return String.format("The file <strong>%s</strong> could not be compiled.\nError raised is : <strong>%s</strong>", isSourceAvailable() ? source.relativePath() : "", problem);
    }
    
    public String getMessage() {
        return problem;
    }

    public List<String> getSource() {
        return Arrays.asList(source.contentAsString().split("\n"));
    }

    public Integer getLineNumber() {
        return line;
    }

    public String getSourceFile() {
        return source.relativePath();
    }

    @Override
    public boolean isSourceAvailable() {
        return source != null && line != null;
    }
    
}
