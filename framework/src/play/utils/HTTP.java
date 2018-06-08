package play.utils;

import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.libs.IO;

import java.io.InputStream;
import java.text.ParseException;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class HTTP {

    public static class ContentTypeWithEncoding {
        public final String contentType;
        public final String encoding;

        public ContentTypeWithEncoding(String contentType, String encoding) {
            this.contentType = contentType;
            this.encoding = encoding;
        }
    }

    public static ContentTypeWithEncoding parseContentType(String contentType) {
        if (contentType == null) {
            return new ContentTypeWithEncoding("text/html".intern(), null);
        } else {
            String[] contentTypeParts = contentType.split(";");
            String _contentType = contentTypeParts[0].trim().toLowerCase();
            String _encoding = null;
            // check for encoding-info
            if (contentTypeParts.length >= 2) {
                String[] encodingInfoParts = contentTypeParts[1].split(("="));
                if (encodingInfoParts.length == 2 && encodingInfoParts[0].trim().equalsIgnoreCase("charset")) {
                    // encoding-info was found in request
                    _encoding = encodingInfoParts[1].trim();

                    if (StringUtils.isNotBlank(_encoding) && ((_encoding.startsWith("\"") && _encoding.endsWith("\""))
                            || (_encoding.startsWith("'") && _encoding.endsWith("'")))) {
                        _encoding = _encoding.substring(1, _encoding.length() - 1).trim();
                    }
                }
            }
            return new ContentTypeWithEncoding(_contentType, _encoding);
        }

    }

    private static final Map<String, String> lower2UppercaseHttpHeaders = initLower2UppercaseHttpHeaders();

    private static Map<String, String> initLower2UppercaseHttpHeaders() {
        Map<String, String> map = new HashMap<>();

        String path = "/play/utils/http_headers.properties";
        InputStream in = HTTP.class.getResourceAsStream(path);
        if (in == null) {
            throw new RuntimeException("Error reading " + path);
        }
        List<String> lines = IO.readLines(in);
        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("#")) {
                map.put(line.toLowerCase(), line);
            }
        }

        return Collections.unmodifiableMap(map);
    }

    /**
     * Use this method to make sure you have the correct casing of a http header name. eg: fixes 'content-type' to
     * 'Content-Type'
     *
     * @param headerName
     *            The given header name to check
     * @return The correct header name
     */
    public static String fixCaseForHttpHeader(String headerName) {
        if (headerName == null) {
            return null;
        }
        String correctCase = lower2UppercaseHttpHeaders.get(headerName.toLowerCase());
        if (correctCase != null) {
            return correctCase;
        }
        // Didn't find it - return it as it is
        return headerName;
    }

    /**
     * <p>Checks if an entity was modified or not</p>
     *
     * @param etag            the entity tag
     * @param last            a Last-Modified value
     * @param browserEtag     an entity tag from request header
     * @param ifModifiedSince a Last-Modified value from request header 'If-Modified-Since'
     * @return <code>true</code> if the entity was modified
     * @see <a href="http://www.faqs.org/rfcs/rfc2616.html">RFC 2616 - Hypertext Transfer Protocol - Section 14.26</a>
     */
    public static boolean isModified(String etag, long last, String browserEtag, String ifModifiedSince) {
        if (browserEtag != null && !browserEtag.equals(etag)) {
            return true;
        }

        if (!isEmpty(ifModifiedSince)) {
            try {
                Date browserDate = Utils.getHttpDateFormatter().parse(ifModifiedSince);
                return browserDate.getTime() < last;
            } catch (ParseException ex) {
                Logger.warn("Can't parse 'If-Modified-Since' header date: %s", ex.getMessage());
            }
        }
        return true;
    }
}
