/**
 * 
 */
package cn.bran.japid.rendererloader;

import java.util.StringTokenizer;

import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

/**
 * Something to compile
 */
final class CompilationUnit implements ICompilationUnit {

    /**
	 * 
	 */
	private final RendererCompiler rendererCompiler;
	final private String clazzName;
    final private String fileName;
    final private char[] typeName;
    final private char[][] packageName;

    CompilationUnit(RendererCompiler rendererCompiler, String pClazzName) {
        this.rendererCompiler = rendererCompiler;
		clazzName = pClazzName;
        if (pClazzName.contains("$")) {
            pClazzName = pClazzName.substring(0, pClazzName.indexOf("$"));
        }
        fileName = pClazzName.replace('.', '/') + ".java";
        int dot = pClazzName.lastIndexOf('.');
        if (dot > 0) {
            typeName = pClazzName.substring(dot + 1).toCharArray();
        } else {
            typeName = pClazzName.toCharArray();
        }
        StringTokenizer izer = new StringTokenizer(pClazzName, ".");
        packageName = new char[izer.countTokens() - 1][];
        for (int i = 0; i < packageName.length; i++) {
            packageName[i] = izer.nextToken().toCharArray();
        }
    }

    @Override
	public char[] getFileName() {
        return fileName.toCharArray();
    }

    @Override
	public char[] getContents() {
        return this.rendererCompiler.japidClasses.get(clazzName).getSourceCode().toCharArray();
    }

    @Override
	public char[] getMainTypeName() {
        return typeName;
    }

    @Override
	public char[][] getPackageName() {
        return packageName;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.compiler.env.ICompilationUnit#ignoreOptionalProblems()
	 */
	@Override
	public boolean ignoreOptionalProblems() {
		return false;
	}
}