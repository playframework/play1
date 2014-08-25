package cn.bran.japid.template;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cn.bran.japid.classmeta.MimeTypeEnum;
import cn.bran.japid.util.DirUtil;


/**
 * A template in Java. Code partly from Play! framework
 */
public class JapidTemplate {
    public String name;
    public String source;
    public String packageName;
    public String className;
    
    // supposed to the mapping of the generated Java source code to the original template file
    // but since lots of complex constructs are translated from simple syntax in the original, the mapping is not really useful
    public Map<Integer, Integer> linesMatrix = new HashMap<Integer, Integer>();
    public String compiledTemplateName;
    public Long timestamp = System.currentTimeMillis();
	public String contentTypeHeader;

	/**
	 * 
	 * @param name
	 *            the Japid script file name.  The name should start from the package root and the
	 *            path to the file will be used as the package name and the file
	 *            name as the class name. e.g.: a/b/c.html, a/b/c.xml. 
	 * @param source
	 *            the source of the template
	 */
    public JapidTemplate(String name, String source) {
        this.name = name;
        this.source = source;
        parseFullyQualifiedName();
    }

    public JapidTemplate(String fqName, MimeTypeEnum mimeType, String source) {
    	if (!fqName.startsWith("japidviews."))
    		throw new RuntimeException("Japid script was not registered with a fully qualified class name starting with \"japidviews.\":  " + fqName);
    	this.name = fqName;
    	this.source = source;
    	int lastDot = fqName.lastIndexOf('.');
    	if (lastDot >= 0) {
			packageName = fqName.substring(0, lastDot);
			className = fqName.substring(lastDot + 1);
    	}
    	else {
    		packageName = "";
    		className = fqName;
    	}
    	if (mimeType != null) {
    		contentTypeHeader = mimeType.header;
    	}
    }

    /**
	 * @author Bing Ran (bing.ran@hotmail.com)
	 */
	private void parseFullyQualifiedName() {
		String tempName = name.replace("-", "_");// .replace('.', '_');
		contentTypeHeader = MimeTypeEnum.getHeader(tempName.substring(tempName.lastIndexOf('.')));
		tempName = DirUtil.mapSrcToJava(tempName);
		tempName = tempName.substring(0, tempName.lastIndexOf(".java"));
		tempName = tempName.replace('\\', '/');

		// extract path
		int lastSep = tempName.lastIndexOf('/');
		if (lastSep > 0) {
			// not default package
			String path = tempName.substring(0, lastSep);
			path = path.replace('/', '.');
			path = path.replace('\\', '.');
			if (path.startsWith("."))
				path = path.substring(1);
			packageName = path;
			className = tempName.substring(lastSep + 1);
		} else {
			className = tempName;
		}
		
	}

	public JapidTemplate(String source) {
        this.name = UUID.randomUUID().toString();
        this.source = source;
    }
    public String javaSource;
    public Class<JapidTemplateBaseWithoutPlay> compiledTemplate;

}
