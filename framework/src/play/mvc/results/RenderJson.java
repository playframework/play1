package play.mvc.results;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.exceptions.UnexpectedException;
import play.libs.optimization.Compression;

/**
 * 200 OK with application/json
 */
public class RenderJson extends Result {

    String json;

    public RenderJson(Object o) {
        json = new Gson().toJson(o);
    }

    public RenderJson(Object o, Type type) {
        json = new Gson().toJson(o, type);
    }

    public RenderJson(Object o, JsonSerializer<?>... adapters) {
        GsonBuilder gson = new GsonBuilder();
        for (Object adapter : adapters) {
            Type t = getMethod(adapter.getClass(), "serialize").getParameterTypes()[0];
            gson.registerTypeAdapter(t, adapter);
        }
        json = gson.create().toJson(o);
    }

    public RenderJson(String jsonString) {
        json = jsonString;
    }

    public void apply(Request request, Response response) {
        try {
            String encoding = getEncoding();
            setContentTypeIfNotSet(response, "application/json; charset="+encoding);

            if (gzipIsSupported(request)) {
                final ByteArrayOutputStream gzip = Compression.gzip(json);
                response.setHeader("Content-Encoding", "gzip");
                response.setHeader("Content-Length", gzip.size() + "");
                response.out = gzip;
            } else {
                response.out.write(json.getBytes(getEncoding()));
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    //
    static Method getMethod(Class<?> clazz, String name) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }
}
