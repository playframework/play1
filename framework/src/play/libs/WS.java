package play.libs;

import java.io.File;
import java.io.IOException;
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
import play.Logger;
import play.PlayPlugin;

public class WS extends PlayPlugin {

    private static HttpClient httpClient;
    private static ThreadLocal<GetMethod> getMethod = new ThreadLocal<GetMethod>();
    private static ThreadLocal<PostMethod> postMethod = new ThreadLocal<PostMethod>();
    private static ThreadLocal<DeleteMethod> deleteMethod = new ThreadLocal<DeleteMethod>();
    private static ThreadLocal<OptionsMethod> optionsMethod = new ThreadLocal<OptionsMethod>();
    private static ThreadLocal<TraceMethod> traceMethod = new ThreadLocal<TraceMethod>();
    private static ThreadLocal<HeadMethod> headMethod = new ThreadLocal<HeadMethod>();

    @Override
    public void invocationFinally() {
        Logger.trace("Releasing http client connections...");
        if (getMethod.get() != null) {
            getMethod.get().releaseConnection();
        }
        if (postMethod.get() != null) {
            postMethod.get().releaseConnection();
        }
        if (deleteMethod.get() != null) {
            deleteMethod.get().releaseConnection();
        }
        if (optionsMethod.get() != null) {
            optionsMethod.get().releaseConnection();
        }
        if (traceMethod.get() != null) {
            traceMethod.get().releaseConnection();
        }
        if (headMethod.get() != null) {
            headMethod.get().releaseConnection();
        }
    }
    

    static {
        MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        httpClient = new HttpClient(connectionManager);
    }

    public static HttpResponse GET(String url, Object... params) {
        return GET(null, url, params);
    }

    public static HttpResponse GET(Map<String, Object> headers, String url, Object... params) {
        url = String.format(url, params);
        if (getMethod.get() != null) {
            getMethod.get().releaseConnection();
        }
        getMethod.set(new GetMethod(url));
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    getMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(getMethod.get());
            return new HttpResponse(getMethod.get());
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
        if (postMethod.get() != null) {
            postMethod.get().releaseConnection();
        }
        postMethod.set(new PostMethod(url));

        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    postMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            if (mimeType != null) {
                postMethod.get().addRequestHeader("content-type", mimeType);
            }

            Part[] parts = {
                new FilePart(body.getName(), body)
            };

            postMethod.get().setRequestEntity(new MultipartRequestEntity(parts, postMethod.get().getParams()));

            httpClient.executeMethod(postMethod.get());
            return new HttpResponse(postMethod.get());
        } catch (Exception e) {
            postMethod.get().releaseConnection();
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse POST(String url, Map<String, Object> body) {
        return POST(null, url, body);
    }

    public static HttpResponse POST(Map<String, Object> headers, String url, Map<String, Object> body) {
        if (postMethod.get() != null) {
            postMethod.get().releaseConnection();
        }
        postMethod.set(new PostMethod(url));
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    postMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }

            postMethod.get().addRequestHeader("content-type", "application/x-www-form-urlencoded");

            ArrayList<NameValuePair> nvps = new ArrayList<NameValuePair>();
            Set<String> keySet = body.keySet();
            for (String key : keySet) {
                Object value = body.get(key);
                if (value instanceof Collection) {
                    for (Object v : (Collection) value) {
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

            postMethod.get().setRequestBody((NameValuePair[]) nvps.toArray());

            httpClient.executeMethod(postMethod.get());
            return new HttpResponse(postMethod.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse POST(String url, String body, String mimeType) {
        return POST((Map<String, Object>) null, url, body);
    }

    public static HttpResponse POST(Map<String, Object> headers, String url, String body) {
        if (postMethod.get() != null) {
            postMethod.get().releaseConnection();
        }
        postMethod.set(new PostMethod(url));

        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    postMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }

            postMethod.get().setRequestEntity(new StringRequestEntity(body));

            httpClient.executeMethod(postMethod.get());
            return new HttpResponse(postMethod.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse DELETE(String url) {
        return DELETE(null, url);
    }

    public static HttpResponse DELETE(Map<String, String> headers, String url) {
        if (deleteMethod.get() != null) {
            deleteMethod.get().releaseConnection();
        }
        deleteMethod.set(new DeleteMethod(url));
        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    deleteMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }

            httpClient.executeMethod(deleteMethod.get());
            return new HttpResponse(deleteMethod.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse HEAD(String url, Object... params) {
        return HEAD(null, url, params);
    }

    public static HttpResponse HEAD(Map<String, String> headers, String url, Object... params) {
        url = String.format(url, params);
        if (headMethod.get() != null) {
            headMethod.get().releaseConnection();
        }
        headMethod.set(new HeadMethod(url));

        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    headMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(headMethod.get());
            return new HttpResponse(headMethod.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse TRACE(String url, Object... params) {
        return TRACE(null, url, params);
    }

    public static HttpResponse TRACE(Map<String, String> headers, String url, Object... params) {
        url = String.format(url, params);
        if (traceMethod.get() != null) {
            traceMethod.get().releaseConnection();
        }
        traceMethod.set(new TraceMethod(url));

        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    traceMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(traceMethod.get());
            return new HttpResponse(traceMethod.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpResponse OPTIONS(String url, Object... params) {
        return OPTIONS(null, url, params);
    }

    public static HttpResponse OPTIONS(Map<String, String> headers, String url, Object... params) {
        url = String.format(url, params);
        if (optionsMethod.get() != null) {
            optionsMethod.get().releaseConnection();
        }
        optionsMethod.set(new OptionsMethod(url));

        try {
            if (headers != null) {
                for (String key : headers.keySet()) {
                    optionsMethod.get().addRequestHeader(key, headers.get(key) + "");
                }
            }
            httpClient.executeMethod(optionsMethod.get());
            return new HttpResponse(optionsMethod.get());
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
                StringReader reader = new StringReader(xml);
                return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(reader));
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                methodBase.releaseConnection();
            }
        }
        /*
         * Parses the xml with the given encoding. 
         */

        public Document getXml(String encoding) {
            try {
                InputSource source = new InputSource(methodBase.getResponseBodyAsStream());
                source.setEncoding(encoding);
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
                return doc;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                methodBase.releaseConnection();
            }
        }

        public String getString() {
            try {
                return methodBase.getResponseBodyAsString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                methodBase.releaseConnection();
            }
        }

        public InputStream getStream() {
            try {
                return new ConnectionReleaserStream(methodBase.getResponseBodyAsStream(), methodBase);
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
            } finally {
                methodBase.releaseConnection();
            }
        }

        public JSONArray getJSONArray() {
            try {
                String json = methodBase.getResponseBodyAsString();
                return JSONArray.fromObject(json);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                methodBase.releaseConnection();
            }
        }

        class ConnectionReleaserStream extends InputStream {

            private InputStream wrapped;
            private HttpMethodBase method;

            public ConnectionReleaserStream(InputStream wrapped, HttpMethodBase method) {
                this.wrapped = wrapped;
                this.method = method;
            }

            @Override
            public int read() throws IOException {
                return this.wrapped.read();
            }

            @Override
            public int read(byte[] arg0) throws IOException {
                return this.wrapped.read(arg0);
            }

            @Override
            public synchronized void mark(int arg0) {
                this.wrapped.mark(arg0);
            }

            @Override
            public int read(byte[] arg0, int arg1, int arg2) throws IOException {
                return this.wrapped.read(arg0, arg1, arg2);
            }

            @Override
            public synchronized void reset() throws IOException {
                this.wrapped.reset();
            }

            @Override
            public long skip(long arg0) throws IOException {
                return this.wrapped.skip(arg0);
            }

            @Override
            public int available() throws IOException {
                return this.wrapped.available();
            }

            @Override
            public boolean markSupported() {
                return this.wrapped.markSupported();
            }

            @Override
            public void close() throws IOException {
                try {
                    this.wrapped.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    if (method != null) {
                        method.releaseConnection();
                    }
                }

            }
        }
    }
}