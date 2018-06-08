package play.data.binding;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

// ActionInvoker.getActionMethodArgs() is called twice when using validation
// so we use this ThreadLocal cache to store the binding-result pr method pr request.
// This way we don't have to do it twice.
public class CachedBoundActionMethodArgs {

    private static final ThreadLocal<CachedBoundActionMethodArgs> current = new ThreadLocal<>();

    private Map<Method, Object[]> preBoundActionMethodArgs = new HashMap<>(1);

    public static void init() {
        current.set( new CachedBoundActionMethodArgs());
    }

    public static void clear() {
        current.remove();
    }

    public static CachedBoundActionMethodArgs current() {
        return current.get();
    }

    public void storeActionMethodArgs( Method method, Object[] rArgs) {
        preBoundActionMethodArgs.put(method, rArgs);
    }

    public Object[] retrieveActionMethodArgs( Method method) {
        return preBoundActionMethodArgs.get(method);
    }

}
