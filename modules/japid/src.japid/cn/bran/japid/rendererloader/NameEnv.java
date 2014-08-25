/**
 * 
 */
package cn.bran.japid.rendererloader;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

import cn.bran.japid.util.JapidFlags;

/**
 * @author bran
 * 
 */
final class NameEnv implements INameEnvironment {
	/**
		 * 
		 */
	private final RendererCompiler rendererCompiler;

	/**
	 * @param rendererCompiler
	 */
	NameEnv(RendererCompiler rendererCompiler) {
		this.rendererCompiler = rendererCompiler;
	}

	@Override
	public NameEnvironmentAnswer findType(final char[][] compoundTypeName) {
		final StringBuffer result = new StringBuffer();
		for (int i = 0; i < compoundTypeName.length; i++) {
			if (i != 0) {
				result.append('.');
			}
			result.append(compoundTypeName[i]);
		}
		return findType(result.toString());
	}

	@Override
	public NameEnvironmentAnswer findType(final char[] typeName, final char[][] packageName) {
		final StringBuffer result = new StringBuffer();
		for (int i = 0; i < packageName.length; i++) {
			result.append(packageName[i]);
			result.append('.');
		}
		result.append(typeName);
		return findType(result.toString());
	}

	private NameEnvironmentAnswer findType(final String name) {
		try {
			if (!name.startsWith("japidviews.")) {
				// let super class loader to load the bytecode
//				byte[] bytes = this.rendererCompiler.crlr.getClassDefinition(name);
				byte[] bytes = this.rendererCompiler.crlr.getClassDefinition(name);
				return bytes == null? null : new NameEnvironmentAnswer(new ClassFileReader(bytes, name.toCharArray(), true), null);
			} else {
				char[] fileName = name.toCharArray();
				RendererClass applicationClass = this.rendererCompiler.japidClasses.get(name);

				// ApplicationClass exists
				if (applicationClass != null) {

					byte[] bytecode = applicationClass.getBytecode();
					if (bytecode != null) {
						ClassFileReader classFileReader = new ClassFileReader(bytecode, fileName, true);
						return new NameEnvironmentAnswer(classFileReader, null);
					}
					// Cascade compilation
					ICompilationUnit compilationUnit = new CompilationUnit(this.rendererCompiler, name);
					return new NameEnvironmentAnswer(compilationUnit, null);
				}
				return null;
			}
		} catch (ClassFormatException e) {
			// Something very very bad
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isPackage(char[][] parentPackageName, char[] packageName) {
		// Rebuild something usable
		StringBuilder sb = new StringBuilder();
		if (parentPackageName != null) {
			for (char[] p : parentPackageName) {
				sb.append(new String(p));
				sb.append(".");
			}
		}
		sb.append(new String(packageName));
		String name = sb.toString();
		if (this.rendererCompiler.packagesCache.containsKey(name)) {
			return this.rendererCompiler.packagesCache.get(name).booleanValue();
		}
		// Check if there is a .java or .class for this resource
		if (this.rendererCompiler.crlr.getClassDefinition(name) != null) {
			this.rendererCompiler.packagesCache.put(name, false);
			return false;
		}
		if (this.rendererCompiler.japidClasses.get(name) != null) {
			this.rendererCompiler.packagesCache.put(name, false);
			return false;
		}
		this.rendererCompiler.packagesCache.put(name, true);
		return true;
	}

	@Override
	public void cleanup() {
	}
}