package play.libs;

import play.*;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MimeTypes utils
 */
public class MimeTypes {

    private static Properties mimetypes;
    private static Pattern extPattern;
    

    static {
        try {
            InputStream is = MimeTypes.class.getClassLoader().getResourceAsStream("play/libs/mime-types.properties");
            mimetypes = new Properties();
            mimetypes.load(is);
            extPattern = Pattern.compile("^.*\\.([^.]+)$");
        } catch (Exception ex) {
            Logger.warn(ex.getMessage());
        }
    }

    /**
     * return the mimetype from a file name
     * @param filename the file name
     * @return the mimetype or the empty string if not found
     */
    public static String getMimeType(String filename) {
        return getMimeType(filename, "");
    }

    /**
     * return the mimetype from a file name.<br/>
     * @param filename the file name
     * @param defaultMimeType the default mime type to return when no matching mimetype is found
     * @return the mimetype
     */
    public static String getMimeType(String filename, String defaultMimeType) {
        Matcher matcher = extPattern.matcher(filename.toLowerCase());
        String ext = "";
        if (matcher.matches()) {
            ext = matcher.group(1);
        }
        if (ext.length() > 0) {
            String mimeType = mimetypes.getProperty(ext);
            if (mimeType == null) {
                return defaultMimeType;
            }
            return mimeType;
        }
        return defaultMimeType;
    }
    /**
     * return the content-type from a file name. If none is found returning application/octet-stream<br/>
     * For a text-based content-type, also return the encoding suffix eg. <em>"text/plain; charset=utf-8"</em>
     * @param filename the file name
     * @return the content-type deduced from the file extension.
     */
    public static String getContentType(String filename){
    	return getContentType(filename, "application/octet-stream");
    }
    /**
     * return the content-type from a file name.<br/>
     * For a text-based content-type, also return the encoding suffix eg. <em>"text/plain; charset=utf-8"</em>
     * @param filename the file name
     * @param defaultContentType the default content-type to return when no matching content-type is found
     * @return the content-type deduced from the file extension.
     */
    public static String getContentType(String filename, String defaultContentType){
    	String contentType = getMimeType(filename, null);
    	if (contentType == null){
    		contentType =  defaultContentType;
    	}
    	if (contentType != null && contentType.startsWith("text/")){
    		return contentType + "; charset=utf-8";
    	}
    	return contentType;
    }
    /**
     * check the mimetype is referenced in the mimetypes database
     * @param mimeType the mimeType to verify
     * @return
     */
    public static boolean isValidMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        } else if (mimeType.indexOf(";") != -1) {
            return mimetypes.contains(mimeType.split(";")[0]);
        } else {
            return mimetypes.contains(mimeType);
        }
    }
}
