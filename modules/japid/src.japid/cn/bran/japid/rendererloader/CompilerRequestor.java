/**
 * 
 */
package cn.bran.japid.rendererloader;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;

import cn.bran.japid.compiler.JapidCompilationException;
import cn.bran.japid.template.JapidTemplate;
import cn.bran.japid.util.DirUtil;
import cn.bran.japid.util.JapidFlags;

/**
 * @author bran
 *
 */
final class CompilerRequestor implements ICompilerRequestor {
	/**
	 * 
	 */
	private final RendererCompiler rendererCompiler;

	/**
	 * @param rendererCompiler
	 */
	CompilerRequestor(RendererCompiler rendererCompiler) {
		this.rendererCompiler = rendererCompiler;
	}

	@Override
	public void acceptResult(CompilationResult result) {
	    // If error
	    if (result.hasErrors()) {
	    	// bran: sort the problems and report the first one
	        CategorizedProblem[] errors = result.getErrors();
	        Arrays.sort(errors, new Comparator<CategorizedProblem>() {
				@Override
				public int compare(CategorizedProblem o1, CategorizedProblem o2) {
					return o1.getSourceLineNumber() - o2.getSourceLineNumber();
				}
	        });
	        
	        for (IProblem problem: errors) {
	            String className = new String(problem.getOriginatingFileName()).replace("/", ".");
	            className = className.substring(0, className.length() - 5);
	            String message = problem.getMessage();
	            if (problem.getID() == IProblem.CannotImportPackage) {
	                // Non sense !
	                message = problem.getArguments()[0] + " cannot be resolved";
	            }
	            RendererClass rc = this.rendererCompiler.japidClasses.get(className);
	            if (rc.getSrcFile() == null)
	            	throw new RuntimeException("no source file for compiling " + className);
	            
	            if (rc.getOriSourceCode() == null)
	            	throw new RuntimeException("no original source code for compiling " + className);
	            
	            JapidTemplate tmpl = new JapidTemplate(rc.getSrcFile().getPath(), rc.getOriSourceCode());
	            throw new JapidCompilationException(
	            		tmpl, 
	            		DirUtil.mapJavaLineToSrcLine(rc.getSourceCode(), problem.getSourceLineNumber()), 
	            		message + " while compiling " + className
	            		);
	        }
	    }
	    // Something has been compiled
	    ClassFile[] clazzFiles = result.getClassFiles();
	    for (int i = 0; i < clazzFiles.length; i++) {
	        final ClassFile clazzFile = clazzFiles[i];
	        final char[][] compoundName = clazzFile.getCompoundName();
	        final StringBuffer clazzName = new StringBuffer();
	        for (int j = 0; j < compoundName.length; j++) {
	            if (j != 0) {
	                clazzName.append('.');
	            }
	            clazzName.append(compoundName[j]);
	        }
	        byte[] bytes = clazzFile.getBytes();
	        JapidFlags.debug("Bytecode generated for " + clazzName);
	        // XXX address anonymous inner class issue!! ....$1...
	        String cname = clazzName.toString();
			RendererClass rc = this.rendererCompiler.japidClasses.get(cname);
	        if (rc == null) {
	        	if (cname.contains("$")) {
	        		// inner class
	        		rc = new RendererClass();
	        		rc.setClassName(cname);
	        		this.rendererCompiler.japidClasses.put(cname, rc);
	        	}
	        	else {
	        		throw new RuntimeException("name not in the classes container: " + cname);
	        	}
	        }
			rc.setBytecode(bytes);
	    }
	}
}