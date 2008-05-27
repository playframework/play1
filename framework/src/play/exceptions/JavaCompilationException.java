package play.exceptions;

import org.eclipse.jdt.core.compiler.IProblem;
import play.classloading.ApplicationClasses.ApplicationClass;

public class JavaCompilationException extends JavaException {

    private IProblem problem;

    public JavaCompilationException(ApplicationClass applicationClass, IProblem problem) {
        super(applicationClass, problem.getSourceLineNumber(), problem.getMessage());
        this.problem = problem;
    }

    public IProblem getProblem() {
        return problem;
    }

    @Override
    public String getErrorTitle() {
        return String.format("Java compilation error");
    }

    @Override
    public String getErrorDescription() {
        return String.format("The file <strong>%s</strong> could not be compiled.\nError raised is : <strong>%s</strong>", getSourceFile(), problem.getMessage());
    }
    
}
