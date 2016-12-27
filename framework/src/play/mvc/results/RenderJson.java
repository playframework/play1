package play.mvc.results;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * 200 OK with application/json
 */
public class RenderJson extends Result {

    private static final Gson GSON = new Gson();
    
    private final String json;
    private final Object response;

    public RenderJson(Object response) {
        this.response = response;
        json = GSON.toJson(response);
    }

    public RenderJson(Object response, Type type) {
        this.response = response;
        json = GSON.toJson(response, type);
    }

    public RenderJson(Object response, JsonSerializer<?>... adapters) {
        this.response = response;
        GsonBuilder gson = new GsonBuilder();
        for (Object adapter : adapters) {
            Type t = getMethod(adapter.getClass(), "serialize").getParameterTypes()[0];
            gson.registerTypeAdapter(t, adapter);
        }
        json = gson.create().toJson(response);
    }

    public RenderJson(String jsonString) {
        json = jsonString;
        this.response = null;
    }

    public RenderJson(Object response, Gson gson) {
        this.response = response;
        if (gson != null) {
            json = gson.toJson(response);
        } else {
            json = GSON.toJson(response);
        }
    }

    @Override
    public void apply(Request request, Response response) {
        try {
            String encoding = getEncoding();
            setContentTypeIfNotSet(response, "application/json; charset=" + encoding);
            response.out.write(json.getBytes(encoding));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getJson() {
        return json;
    }

    public Object getResponse() {
        return response;
    }

    private static Method getMethod(Class clazz, String methodName) {
        Method bestMatch = null;
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && !m.isBridge()) {
                if (bestMatch == null || !Object.class.equals(m.getParameterTypes()[0])) {
                    bestMatch = m;
                }
            }
        }
        return bestMatch;
    }
}
