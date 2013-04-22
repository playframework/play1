package play.libs.ws;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import play.Logger;
import play.libs.IO;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSImpl;
import play.libs.WS.WSRequest;
import play.mvc.Http.Header;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the WS interface based on Java URL Fetch API.
 * This is to be used for example in Google App Engine, where the 
 * async http client can't be used.
 */
public class WSUrlFetch implements WSImpl {

    public WSUrlFetch() {}

    public void stop() {}

    public play.libs.WS.WSRequest newRequest(String url, String encoding) {
        return new WSUrlfetchRequest(url, encoding);
    }

    public class WSUrlfetchRequest extends WSRequest {

        protected WSUrlfetchRequest(String url, String encoding) {
            super(url, encoding);
        }

        private String getPreparedUrl(String method) {
            String u = url;
            if (parameters != null && !parameters.isEmpty()) {
                // If not PUT or POST, we must add these params to the queryString
                if (!("PUT".equals(method) || "POST".equals(method))) {
                    // must add params to queryString/url
                    StringBuilder sb = new StringBuilder(url);
                    if (url.indexOf("?")>0) {
                        sb.append('&');
                    } else {
                        sb.append('?');
                    }
                    int count=0;
                    for( Map.Entry<String, Object> e : parameters.entrySet()) {
                        count++;
                        String key = e.getKey();
                        Object value = e.getValue();
                        if (value == null) continue;

                        if (value instanceof Collection<?> || value.getClass().isArray()) {
                            Collection<?> values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
                            for (Object v: values) {
                                if (count > 1) {
                                    sb.append('&');
                                }
                                sb.append(encode(key));
                                sb.append('=');
                                sb.append(encode(v.toString()));
                            }
                        } else {
                            if (count > 1) {
                                sb.append('&');
                            }
                            sb.append(encode(key));
                            sb.append('=');
                            sb.append(encode(value.toString()));
                        }

                    }

                    u = sb.toString();

                    // Must clear the parameters to prevent us from using them again
                    parameters.clear();
                }
            }
            return u;
        }

        /** Execute a GET request synchronously. */
        public HttpResponse get() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(getPreparedUrl("GET")), "GET"));
            } catch (Exception e) {
                Logger.error(e.toString());
                throw new RuntimeException(e);
            }
        }

        /** Execute a POST request.*/
        public HttpResponse post() {
            try {
                HttpURLConnection conn = prepare(new URL(getPreparedUrl("POST")), "POST");
                return new HttpUrlfetchResponse(conn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a PUT request.*/
        public HttpResponse put() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(getPreparedUrl("PUT")), "PUT"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a DELETE request.*/
        public HttpResponse delete() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(getPreparedUrl("DELETE")), "DELETE"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a OPTIONS request.*/
        public HttpResponse options() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(getPreparedUrl("OPTIONS")), "OPTIONS"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a HEAD request.*/
        public HttpResponse head() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(getPreparedUrl("HEAD")), "HEAD"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a TRACE request.*/
        public HttpResponse trace() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(getPreparedUrl("TRACE")), "TRACE"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private HttpURLConnection prepare(URL url, String method) {
            if (this.username != null && this.password != null && this.scheme != null) {
                String authString = null;
                switch (this.scheme) {
                case BASIC: authString = basicAuthHeader(); break;
                default: throw new RuntimeException("Scheme " + this.scheme + " not supported by the UrlFetch WS backend.");
                }
                this.headers.put("Authorization", authString);
            }
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(this.followRedirects);
                connection.setReadTimeout(this.timeout * 1000);
                for (String key : this.headers.keySet()) {
                    connection.setRequestProperty(key, headers.get(key));
                }

                if (this.oauthToken != null && this.oauthSecret != null) {
                    OAuthConsumer consumer = new DefaultOAuthConsumer(oauthInfo.consumerKey, oauthInfo.consumerSecret);
                    consumer.setTokenWithSecret(oauthToken, oauthSecret);
                    consumer.sign(connection);
                }
                checkFileBody(connection);
                return connection;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void checkFileBody(HttpURLConnection connection) throws IOException {
            /*            if (this.fileParams != null) {
            connection.setDoOutput(true);
            //could be optimized, we know the size of this array.
            for (int i = 0; i < this.fileParams.length; i++) {
            builder.addBodyPart(new FilePart(this.fileParams[i].paramName,
            this.fileParams[i].file,
            MimeTypes.getMimeType(this.fileParams[i].file.getName()),
            null));
            }
            if (this.parameters != null) {
            for (String key : this.parameters.keySet()) {
            Object value = this.parameters.get(key);
            if (value instanceof Collection<?> || value.getClass().isArray()) {
            Collection<?> values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
            for (Object v : values) {
            builder.addBodyPart(new StringPart(key, v.toString()));
            }
            } else {
            builder.addBodyPart(new StringPart(key, value.toString()));
            }
            }
            }
            return;
            }*/
            if (this.parameters != null && !this.parameters.isEmpty()) {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset="+encoding);
                connection.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(createQueryString());
                writer.close();
            }
            if (this.body != null) {
                if (this.parameters != null && !this.parameters.isEmpty()) {
                    throw new RuntimeException("POST or PUT method with parameters AND body are not supported.");
                }
                connection.setDoOutput(true);
                
                if(this.mimeType != null) {
                    connection.setRequestProperty("Content-Type", this.mimeType+"; charset="+encoding);
                }
                OutputStream out = connection.getOutputStream();
                if(this.body instanceof InputStream) {
                    InputStream bodyStream = (InputStream)this.body;
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while( (bytesRead = bodyStream.read(buffer, 0, buffer.length)) > 0) {
                        out.write(buffer, 0, bytesRead);
                    }
                } else {
                    try {
                        byte[] bodyBytes = this.body.toString().getBytes( this.encoding );
                        out.write( bodyBytes );
                    } catch ( UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        }
    }

    /**
     * An HTTP response wrapper
     */
    public static class HttpUrlfetchResponse extends HttpResponse {

        private String body;
        private Integer status;
        private String statusText;
        private Map<String, List<String>> headersMap;

        /**
         * you shouldnt have to create an HttpResponse yourself
         * @param method
         */
        public HttpUrlfetchResponse(HttpURLConnection connection) {
            try {
                this.status = connection.getResponseCode();
                this.statusText = connection.getResponseMessage();
                this.headersMap = connection.getHeaderFields();
                InputStream is = null;
                if (this.status >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    // 4xx/5xx may return a response via getErrorStream()
                    is = connection.getErrorStream();
                } else {
                    is = connection.getInputStream();
                }
                if (is != null) this.body = IO.readContentAsString(is, getEncoding());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                connection.disconnect();
            }
        }

        /**
         * the HTTP status code
         * @return the status code of the http response
         */
        @Override
        public Integer getStatus() {
            return status;
        }

        /**
         * the HTTP status text
         * @return the status text of the http response
         */
        @Override
        public String getStatusText() {
            return statusText;
        }

        @Override
        public String getHeader(String key) {
            return headersMap.containsKey(key) ? headersMap.get(key).get(0) : null;
        }

        @Override
        public List<Header> getHeaders() {
            List<Header> result = new ArrayList<Header>();
            for (String key : headersMap.keySet()) {
                result.add(new Header(key, headersMap.get(key)));
            }
            return result;
        }

        /**
         * get the response body as a string
         * @return the body of the http response
         */
        @Override
        public String getString() {
            return body;
        }

        /**
         * get the response as a stream
         * @return an inputstream
         */
        @Override
        public InputStream getStream() {
            try {
                return new ByteArrayInputStream(body.getBytes( getEncoding() ));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}