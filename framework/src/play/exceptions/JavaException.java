package play.exceptions;

import play.Play;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public abstract class JavaException extends PlayException implements SourceAttachment {
	
	private String fileName;
	private Integer lineNumber;
	
	public JavaException(String fileName, Integer lineNumber, String message) {
		super(message);
		this.fileName = fileName;
		this.lineNumber = lineNumber;
	}
	
	public JavaException(String fileName, Integer lineNumber, String message, Throwable cause) {
		super(message, cause);
		this.fileName = fileName;
		this.lineNumber = lineNumber;
	}
		
	public Integer getLineNumber() {
		return lineNumber;
	}
	
	public List<String> getSource() {
		String javaSource = "app/"+fileName;
		/*Play.VirtualFile javaSourceFile = (Play.getApplicationRoot(), javaSource);
		if(javaSourceFile.exists()) {
			List<String> source = new ArrayList<String>();
			BufferedReader reader = new BufferedReader( new InputStreamReader(new FileInputStream(javaSourceFile), "utf-8") );
			String line = "";
			while ( (line = reader.readLine()) != null) {
				source.add(line);
			}
			reader.close();
			return source;
		}*/
		return null;
	}
	
	public String getSourceFile() {
		return fileName;
	}
	
	public String getSourceRealFile() {
		return "app/"+getSourceFile();
	}
	
	public String getLanguage() {
		return "java";
	}
	
	@Override
	public boolean isSourceAvailable() {
		return fileName != null && lineNumber != null;
	}

}
