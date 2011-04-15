package play.libs.ws;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import play.Logger;
import play.libs.IO;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSImpl;
import play.libs.WS.WSRequest;
import play.mvc.Http.Header;

/**
 * Implementation of the WS interface based on Java URL Fetch API.
 * This is to be used for example in Google App Engine, where the 
 * async http client can't be used.
 */
public class WSUrlFetch implements WSImpl {

    public WSUrlFetch() {
    }

    public void stop() {
    }

    public play.libs.WS.WSRequest newRequest(String url) {
        return new WSUrlfetchRequest(url);
    }

    public class WSUrlfetchRequest extends WSRequest {

        protected WSUrlfetchRequest(String url) {
            this.url = url;
        }

        /** Execute a GET request synchronously. */
        @Override
        public HttpResponse get() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(url), "GET"));
            } catch (Exception e) {
                Logger.error(e.toString());
                throw new RuntimeException(e);
            }
        }

        /** Execute a POST request.*/
        @Override
        public HttpResponse post() {
            try {
                HttpURLConnection conn = prepare(new URL(url), "POST");
                return new HttpUrlfetchResponse(conn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a PUT request.*/
        @Override
        public HttpResponse put() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(url), "PUT"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a DELETE request.*/
        @Override
        public HttpResponse delete() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(url), "DELETE"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a OPTIONS request.*/
        @Override
        public HttpResponse options() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(url), "OPTIONS"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a HEAD request.*/
        @Override
        public HttpResponse head() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(url), "HEAD"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a TRACE request.*/
        @Override
        public HttpResponse trace() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(url), "TRACE"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private HttpURLConnection prepare(URL url, String method) {
            if (this.username != null && this.password != null && this.scheme != null) {
                String authString = null;
                switch (this.scheme) {
                    case BASIC:
                        authString = basicAuthHeader();
                        break;
                    default:
                        throw new RuntimeException("Scheme " + this.scheme + " not supported by the UrlFetch WS backend.");
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
                checkFileBody(connection);
                if (this.oauthToken != null && this.oauthSecret != null) {
                    OAuthConsumer consumer = new DefaultOAuthConsumer(oauthInfo.consumerKey, oauthInfo.consumerSecret);
                    consumer.setTokenWithSecret(oauthToken, oauthSecret);
                    consumer.sign(connection);
                }
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
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(createQueryString());
                writer.close();
            }
            if (this.body != null) {
                if (this.parameters != null && !this.parameters.isEmpty()) {
                    throw new RuntimeException("POST or PUT method with parameters AND body are not supported.");
                }
                if (this.mimeType != null) {
                    connection.setRequestProperty("Content-Type", this.mimeType);
                }
                connection.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(this.body.toString());
                writer.close();
            }
        }
    }

    /**
     * An HTTP response wrapper
     */
    public static class HttpUrlfetchResponse extends HttpResponse {

        private String body;
        private Integer status;
        private Map<String, List<String>> headersMap;

        /**
         * you shouldnt have to create an HttpResponse yourself
         * @param method
         */
        public HttpUrlfetchResponse(HttpURLConnection connection) {
            try {
                this.status = connection.getResponseCode();
                this.headersMap = connection.getHeaderFields();
                this.body = IO.readContentAsString(connection.getInputStream());
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
                return new ByteArrayInputStream(body.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
