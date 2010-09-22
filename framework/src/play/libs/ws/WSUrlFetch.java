package play.libs.ws;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import play.Logger;
import play.libs.Codec;
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

    private static WSUrlFetch uniqueInstance;

    private WSUrlFetch() {}

    public static WSUrlFetch getInstance() {
        if (uniqueInstance == null) {
            uniqueInstance = new WSUrlFetch();
        }
        return uniqueInstance;
    }

    @Override
    public void init() {}

    @Override
    public void stop() {}

    @Override
    public play.libs.WS.WSRequest newRequest(String url) {
        return new WSUrlfetchRequest(url);
    }

    public static class WSUrlfetchRequest extends WSRequest {

        private WSUrlfetchRequest(String url) {
            this.url = url;
        }

        /** Execute a GET request synchronously. */
        public HttpResponse get() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(url), "GET"));
            } catch (Exception e) {
                Logger.error(e.toString());
                throw new RuntimeException(e);
            }
        }

        /** Execute a POST request.*/
        public HttpResponse post() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(url), "POST"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a PUT request.*/
        public HttpResponse put() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(url), "PUT"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

       /** Execute a DELETE request.*/
        public HttpResponse delete() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(url), "DELETE"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a OPTIONS request.*/
        public HttpResponse options() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(url), "OPTIONS"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a HEAD request.*/
        public HttpResponse head() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(url), "HEAD"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a TRACE request.*/
        public HttpResponse trace() {
            try {
                return new HttpUrlfetchResponse(prepare(new URL(url), "TRACE"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private HttpURLConnection prepare(URL url, String method) {
            if (this.username != null && this.password != null) {
                this.headers.put("Authorization", "Basic " + Codec.encodeBASE64(this.username + ":" + this.password));
            }
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                checkFileBody(connection);
                connection.setRequestMethod(method);
                for (String key: this.headers.keySet()) {
                    connection.setRequestProperty(key, headers.get(key));
                }
                return connection;
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void checkFileBody(HttpURLConnection connection) throws IOException {
/*            if (this.fileParams != null) {
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
                connection.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(this.body);
                writer.close();
                if(this.mimeType != null) {
                    connection.setRequestProperty("Content-Type", this.mimeType);
                }
            }
        }

    }

    /**
     * An HTTP response wrapper
     */
    public static class HttpUrlfetchResponse extends HttpResponse {

        private HttpURLConnection connection;

        /**
         * you shouldnt have to create an HttpResponse yourself
         * @param method
         */
        public HttpUrlfetchResponse(HttpURLConnection connection) {
            this.connection = connection;
            connection.setDoInput(true);
        }

        /**
         * the HTTP status code
         * @return the status code of the http response
         */
        public Integer getStatus() {
            try {
                return this.connection.getResponseCode();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public String getHeader(String key) {
            return connection.getHeaderField(key);
        }

        public List<Header> getHeaders() {
            Map<String, List<String>> hdrs = connection.getHeaderFields();
            List<Header> result = new ArrayList<Header>();
            for (String key: hdrs.keySet()) {
                result.add(new Header(key, hdrs.get(key)));
            }
            return result;
        }

        /**
         * get the response body as a string
         * @return the body of the http response
         */
        public String getString() {
            try {
                return IO.readContentAsString(connection.getInputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * get the response as a stream
         * @return an inputstream
         */
        public InputStream getStream() {
            try {
                return connection.getInputStream();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
