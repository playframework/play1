package play.libs;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class WS {

    private static HttpClient httpClient;
    private static GetMethod getMethod;
    private static PostMethod postMethod;
    private static DeleteMethod deleteMethod;
    private static OptionsMethod optionsMethod;
    private static TraceMethod traceMethod;
    private static HeadMethod headMethod;

    static {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        httpClient = new HttpClient(connectionManager);
    }

    public static HttpResponse GET(String url, Object... params) {
        return GET(null, url, params);
    }

    public static HttpResponse GET(Map<String, Object> headers, String url, Object... params) {
        url = String.format(url, params);
        if(getMethod != null)
            getMethod.releaseConnection();
        getMethod = new GetMethod(url);
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    getMethod.addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(getMethod);
            return new HttpResponse(getMethod);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse POST(String url, File body) {
        String mimeType = MimeTypes.getMimeType(body.getName());
        return POST(null, url, body, mimeType);
    }
    
    public static HttpResponse POST(String url, File body, String mimeType) {
        return POST(null, url, body, mimeType);
    }
    
    public static HttpResponse POST(Map<String, String> headers, String url, File body) {
        return POST(headers, url, body, null);
    }

    private static HttpResponse POST(Map<String, String> headers, String url, File body, String mimeType) {
        if (postMethod != null) {
            postMethod.releaseConnection();
        }
        postMethod = new PostMethod(url);

        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    postMethod.addRequestHeader(key, headers.get(key) + "");
                }
            }
            if(mimeType != null) {
                postMethod.addRequestHeader("content-type", mimeType);
            }
            
            Part[] parts = {
                new FilePart(body.getName(), body)
            };
            
            postMethod.setRequestEntity(new MultipartRequestEntity(parts, postMethod.getParams()));
            
            httpClient.executeMethod(postMethod);
            return new HttpResponse(postMethod);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
   public static HttpResponse POST(String url, Map<String, Object> body) {
        return POST(null, url, body);
    }
    
    public static HttpResponse POST(Map<String, Object> headers, String url, Map<String, Object> body) {
        if (postMethod != null) {
            postMethod.releaseConnection();
        }
        postMethod = new PostMethod(url);
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    postMethod.addRequestHeader(key, headers.get(key) + "");
                }
            }
            
            postMethod.addRequestHeader("content-type", "application/x-www-form-urlencoded");
            
            ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
            Set<String> keySet = body.keySet();
            for(String key : keySet) {
                Object value = body.get(key);
                if(value instanceof Collection) {
                    for(Object v : (Collection)value) {
                        NameValuePair nvp = new NameValuePair();
                        nvp.setName(key);
                        nvp.setValue(v.toString());                   
                    }
                } else {
                    NameValuePair nvp = new NameValuePair();
                    nvp.setName(key);
                    nvp.setValue(value.toString());                   
                }
            }

            postMethod.setRequestBody((NameValuePair[]) nvps.toArray());
            
            httpClient.executeMethod(postMethod);
            return new HttpResponse(postMethod);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
                
    public static HttpResponse POST(String url, String body, String mimeType) {
        return POST((Map<String, Object>)null, url, body);
    }
    
    public static HttpResponse POST(Map<String, Object> headers, String url, String body) {
        if (postMethod != null) {
            postMethod.releaseConnection();
        }
        postMethod = new PostMethod(url);
        
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    postMethod.addRequestHeader(key, headers.get(key) + "");
                }
            }
            
            postMethod.setRequestEntity(new StringRequestEntity(body));
            
            httpClient.executeMethod(postMethod);
            return new HttpResponse(postMethod);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static HttpResponse DELETE(String url) {
        return DELETE(null, url);
    }

    public static HttpResponse DELETE(Map<String, String> headers, String url) {
        if (deleteMethod != null) {
            deleteMethod.releaseConnection();
        }
        deleteMethod = new DeleteMethod(url);
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    deleteMethod.addRequestHeader(key, headers.get(key) + "");
                }
            }
            
            httpClient.executeMethod(deleteMethod);
            return new HttpResponse(deleteMethod);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static HttpResponse HEAD(String url, Object... params) {
        return HEAD(null, url, params);
    }

    public static HttpResponse HEAD(Map<String, String> headers, String url, Object... params) {
        url = String.format(url, params);
        if (headMethod != null) {
            headMethod.releaseConnection();
        }
        headMethod = new HeadMethod(url);

        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    headMethod.addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(headMethod);
            return new HttpResponse(headMethod);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse TRACE(String url, Object... params) {
        return TRACE(null, url, params);
    }

    public static HttpResponse TRACE(Map<String, String> headers, String url, Object... params) {
        url = String.format(url, params);
        if (traceMethod != null) {
            traceMethod.releaseConnection();
        }
        traceMethod = new TraceMethod(url);

        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    traceMethod.addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(traceMethod);
            return new HttpResponse(traceMethod);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static HttpResponse OPTIONS(String url, Object... params) {
        return OPTIONS(null, url, params);
    }

    public static HttpResponse OPTIONS(Map<String, String> headers, String url, Object... params) {
        url = String.format(url, params);
        if (optionsMethod != null) {
            optionsMethod.releaseConnection();
        }
        optionsMethod = new OptionsMethod(url);

        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    optionsMethod.addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(optionsMethod);
            return new HttpResponse(optionsMethod);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String encode(String part) {
        try {
            return URLEncoder.encode(part, "utf-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reponse d'un Service Web.
     */
    public static class HttpResponse {

        private HttpMethodBase methodBase;

        public HttpResponse(HttpMethodBase method) {
            this.methodBase = method;
        }

        public Integer getStatus() {
            return this.methodBase.getStatusCode();
        }

        public Document getXml() {
            try {
                String xml = methodBase.getResponseBodyAsString();
                methodBase.releaseConnection();
                StringReader reader = new StringReader(xml);
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(reader));
                return doc;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        /*
         * Parses the xml with the given encoding. 
         */
        public Document getXml(String encoding){
             try {
                InputSource source = new InputSource(methodBase.getResponseBodyAsStream());
                source.setEncoding(encoding);
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
                methodBase.releaseConnection();
                return doc;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        public String getString() {
            try {
                return methodBase.getResponseBodyAsString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public InputStream getStream() {
            try {
                return methodBase.getResponseBodyAsStream();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        public JSONObject getJSONObject() {
            try {
                String json = methodBase.getResponseBodyAsString();
                return JSONObject.fromObject(json);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public JSONArray getJSONArray() {
            try {
                String json = methodBase.getResponseBodyAsString();
                return JSONArray.fromObject(json);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
