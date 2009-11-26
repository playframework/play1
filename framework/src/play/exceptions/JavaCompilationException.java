package play.exceptions;

import org.eclipse.jdt.core.compiler.IProblem;
import play.classloading.ApplicationClasses.ApplicationClass;

/**
 * A java compilation error
 */
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
        return String.format("Compilation error");
    }

    @Override
    public String getErrorDescription() {
        return String.format("The file <strong>%s</strong> could not be compiled.\nError raised is : <strong>%s</strong>", getSourceFile(), getMessage(problem));
    }
    
    public String getMessage(IProblem problem) {
        if(problem.getID() == IProblem.CannotImportPackage) {
            // Non sense !
            return problem.getArguments()[0]+" cannot be resolved";
        }
        return problem.getMessage();
    }
    
}
