/**
 * 
 */
package cn.bran.play.util;

import cn.bran.japid.util.DirUtil;
import play.exceptions.CompilationException;
import play.vfs.VirtualFile;

/**
 * @author bran
 * 
 */
public class PlayExceptionUtils {
	public static Exception mapJapidJavaCodeError(Exception ex) {
		if (ex instanceof CompilationException) {
			CompilationException e = (CompilationException) ex;
			if (!e.isSourceAvailable())
				return e;

			// now map java error to japidview source code

			String srcFilePath = e.getSourceFile();
			if (!srcFilePath.startsWith("/app/japidviews/")) {
				return e;
			}
			else if (!srcFilePath.endsWith("java")) {
				return e;
			}

			String viewSourceFilePath = DirUtil.mapJavaToSrc(srcFilePath);
			// File file = new File(viewSourceFilePath);
			VirtualFile vf = VirtualFile.fromRelativePath(viewSourceFilePath);

			int oriLineNumber = mapJavaErrorLineToSrcLine(e.getSourceVirtualFile().contentAsString(), e.getLineNumber());
			e = new CompilationException(vf, "\"" + e.getMessage() + "\"", oriLineNumber, 0, 0);
			return e;
		}
		return ex;
	}

	static int mapJavaErrorLineToSrcLine(String sourceCode, int lineNum) {
		String[] codeLines = sourceCode.split("\n");
		String line = codeLines[lineNum - 1];

		int lineMarker = line.lastIndexOf("// line ");
		if (lineMarker < 1) {
			return 0;
		}
		line = line.substring(lineMarker + 8).trim();
		String num = "";
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (Character.isDigit(c))
				num += c;
			else
				break;
		}
		int oriLineNumber = Integer.parseInt(num);
		return oriLineNumber;
	}
}
