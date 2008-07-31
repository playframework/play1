package play;

import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MimeTypes {
    private static Properties properties;
    private static Pattern extPattern;
    
    static {
    	 try {
             InputStream is = MimeTypes.class.getClassLoader().getResourceAsStream("play/mime-types.properties");
             properties = new Properties();
             properties.load(is);
             extPattern = Pattern.compile("^.*\\.([^.]+)$");
         } catch (Exception ex) {
             Logger.warn(ex.getMessage());
         }	
    }
        
    public static String getMimeType(String filename) {
        Matcher matcher = extPattern.matcher(filename.toLowerCase());
        String ext = "";
        if(matcher.matches()) {
            ext = matcher.group(1);
        }
        if(ext.length()>0) {
            String mimeType = properties.getProperty(ext);
            if(mimeType == null)
            	return "";
            if(mimeType.startsWith("text/")) {
                mimeType = mimeType + "; charset=utf-8";
            }
            
            return mimeType;
        }
        return "";
    }
 
    public static boolean isValidMimeType(String mimeType) {
    	if(mimeType == null)
    		return false;
    	return properties.contains(mimeType.split(";")[0]);
    }
}
