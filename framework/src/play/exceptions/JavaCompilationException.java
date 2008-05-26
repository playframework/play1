package play.exceptions;

import org.eclipse.jdt.core.compiler.IProblem;

public class JavaCompilationException extends JavaException {

    private IProblem problem;

    public JavaCompilationException(String fileName, IProblem problem) {
        super(fileName, problem.getSourceLineNumber(), problem.getMessage());
        this.problem = problem;
    }

    public IProblem getProblem() {
        return problem;
    }

    @Override
    public String getErrorTitle() {
        return String.format("Compilation error in %s", getSourceFile());
    }

    @Override
    public String getErrorDescription() {
        return String.format("The file <strong>%s</strong> could not be compiled.\nError raised is : <strong>%s</strong>", getSourceFile(), problem.getMessage());
    }
    
}
