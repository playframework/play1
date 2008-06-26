package play.libs;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class WS {

    private static HttpClient httpClient;

    static {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        httpClient = new HttpClient(connectionManager);
    }

    public static HttpResponse GET(String url, Object... params) {
        return GET(null, url, params);
    }

    public static HttpResponse GET(Map<String, String> headers, String url, Object... params) {
        url = String.format(url, params);
        try {
            GetMethod getMethod = new GetMethod(url);
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
