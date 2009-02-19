package play.mvc;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jregex.Matcher;
import jregex.Pattern;
import jregex.REFlags;
import play.Logger;
import play.Play;
import play.Play.Mode;
import play.vfs.VirtualFile;
import play.exceptions.NoRouteFoundException;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.templates.TemplateLoader;

/**
 * The router matches HTTP requests to action invocations
 */
public class Router {

    static Pattern routePattern = new Pattern("^({method}GET|POST|PUT|DELETE|OPTIONS|HEAD|\\*)?\\s+({path}/[^\\s]*)\\s+({action}[^\\s(]+)({params}.+)?$");
    /**
     * Pattern used to locate a method override instruction in request.querystring
     */
    static Pattern methodOverride = new Pattern("^.*x-http-method-override=({method}GET|PUT|POST|DELETE).*$");
    public static long lastLoading = -1;

    public static void load() {
        routes.clear();
        parse(Play.routes, "");
        lastLoading = System.currentTimeMillis();
    }

    /**
     * Parse a route file.
     * If an action starts with <i>"plugin:name"</i>, replace that route by the ones declared
     * in the plugin route file denoted by that <i>name</i>, if found.
     * @param routeFile
     * @param prefix The prefix that the path of all routes in this route file start with. This prefix should not
     * end with a '/' character.
     */
    static void parse(VirtualFile routeFile, String prefix) {
        String content = TemplateLoader.load(routeFile).render(new HashMap<String, Object>());
        for (String line : content.split("\n")) {
            line = line.trim().replaceAll("\\s+", " ");
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            Matcher matcher = routePattern.matcher(line);
            if (matcher.matches()) {
                String action = matcher.group("action");
                // module:
                if (action.startsWith("module:")) {
                    String moduleName = action.substring("module:".length());
                    String newPrefix = prefix + matcher.group("path");
                    if (newPrefix.length() > 1 && newPrefix.endsWith("/")) {
                        newPrefix = newPrefix.substring(0, newPrefix.length() - 1);
                    }
                    if (moduleName.equals("*")) {
                        for (String p : Play.modulesRoutes.keySet()) {
                            parse(Play.modulesRoutes.get(p), newPrefix + p);
                        }
                    } else if (Play.modulesRoutes.containsKey(moduleName)) {
                        parse(Play.modulesRoutes.get(moduleName), newPrefix);
                    } else {
                        Logger.error("Cannot include routes for module %s (not found)", moduleName);
                    }
                } else {
                    Route route = new Route();
                    route.method = matcher.group("method");
                    route.path = prefix + matcher.group("path");
                    if (route.path.endsWith("/") && !route.path.equals("/")) {
                        route.path = route.path.substring(0, route.path.length() - 1);
                    }
                    route.action = action;
                    route.addParams(matcher.group("params"));
                    route.compute();
                    routes.add(route);
                }
            } else {
                Logger.error("Invalid route definition : %s", line);
            }
        }
    }

    public static void detectChanges() {
        if (Play.mode == Mode.PROD && lastLoading > 0) {
            return;
        }
        if (Play.routes.lastModified() > lastLoading) {
            load();
        } else {
            for (VirtualFile file : Play.modulesRoutes.values()) {
                if (file.lastModified() > lastLoading) {
                    load();
                    return;
                }
            }
        }
    }
    static List<Route> routes = new ArrayList<Route>();

    public static void route(Http.Request request) {
        // request method may be overriden if a x-http-method-override parameter is given
        if (request.querystring != null && methodOverride.matches(request.querystring)) {
            Matcher matcher = methodOverride.matcher(request.querystring);
            if (matcher.matches()) {
                Logger.debug("request method %s overriden to %s ", request.method, matcher.group("method"));
                request.method = matcher.group("method");
            }
        }
        for (Route route : routes) {
            Map<String, String> args = route.matches(request.method, request.path);
            if (args != null) {
                request.routeArgs = args;
                request.action = route.action;
                if (request.action.indexOf("{") > -1) { // more optimization ?
                    for (String arg : request.routeArgs.keySet()) {
                        request.action = request.action.replace("{" + arg + "}", request.routeArgs.get(arg));
                    }
                }
                return;
            }
        }
        throw new NotFound(request.method, request.path);
    }

    public static Map<String, String> route(String method, String path) {
        for (Route route : routes) {
            Map<String, String> args = route.matches(method, path);
            if (args != null) {
                args.put("action", route.action);
                return args;
            }
        }
        return new HashMap<String, String>();
    }

    public static ActionDefinition reverse(String action) {
        return reverse(action, new HashMap<String, Object>());
    }

    public static String getFullUrl(String action, Map<String, Object> args) {
        return Http.Request.current().getBase() + reverse(action, args);
    }

    public static String getFullUrl(String action) {
        return getFullUrl(action, new HashMap<String, Object>());
    }

    public static ActionDefinition reverse(String action, Map<String, Object> args) {
        if (action.startsWith("controllers.")) {
            action = action.substring(12);
        }
        for (Route route : routes) {
            if (route.actionPattern != null) {
                Matcher matcher = route.actionPattern.matcher(action);
                if (matcher.matches()) {
                    for (String group : route.actionArgs) {
                        String v = matcher.group(group);
                        if (v == null) {
                            continue;
                        }
                        args.put(group, v.toLowerCase());
                    }
                    List<String> inPathArgs = new ArrayList<String>();
                    boolean allRequiredArgsAreHere = true;
                    // les noms de parametres matchent ils ?
                    for (Route.Arg arg : route.args) {
                        inPathArgs.add(arg.name);
                        Object value = args.get(arg.name);
                        if (value == null) {
                            allRequiredArgsAreHere = false;
                            break;
                        } else {
                            if (value instanceof List) {
                                value = ((List<Object>) value).get(0);
                            }
                            if (!arg.constraint.matches(value.toString())) {
                                allRequiredArgsAreHere = false;
                                break;
                            }
                        }
                    }
                    // les parametres codes en dur dans la route matchent-ils ?
                    for (String staticKey : route.staticArgs.keySet()) {
                        if (!args.containsKey(staticKey) || !args.get(staticKey).equals(route.staticArgs.get(staticKey))) {
                            allRequiredArgsAreHere = false;
                            break;
                        }
                    }
                    if (allRequiredArgsAreHere) {
                        StringBuilder queryString = new StringBuilder();
                        String path = route.path;
                        for (String key : args.keySet()) {
                            if (inPathArgs.contains(key) && args.get(key) != null) {
                                if (List.class.isAssignableFrom(args.get(key).getClass())) {
                                    List<Object> vals = (List<Object>) args.get(key);
                                    path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", vals.get(0) + "");
                                } else {
                                    path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", args.get(key) + "");
                                }
                            } else if (route.staticArgs.containsKey(key)) {
                                // Do nothing -> The key is static
                            } else if (args.get(key) != null) {
                                if (List.class.isAssignableFrom(args.get(key).getClass())) {
                                    List<Object> vals = (List<Object>) args.get(key);
                                    for (Object object : vals) {
                                        try {
                                            queryString.append(URLEncoder.encode(key, "utf-8"));
                                            queryString.append("=");
                                            queryString.append(URLEncoder.encode(object.toString() + "", "utf-8"));
                                            queryString.append("&");
                                        } catch (UnsupportedEncodingException ex) {
                                        }
                                    }
                                } else {
                                    try {
                                        queryString.append(URLEncoder.encode(key, "utf-8"));
                                        queryString.append("=");
                                        queryString.append(URLEncoder.encode(args.get(key) + "", "utf-8"));
                                        queryString.append("&");
                                    } catch (UnsupportedEncodingException ex) {
                                    }
                                }
                            }
                        }
                        String qs = queryString.toString();
                        if (qs.endsWith("&")) {
                            qs = qs.substring(0, qs.length() - 1);
                        }
                        ActionDefinition actionDefinition = new ActionDefinition();
                        actionDefinition.url = qs.length() == 0 ? path : path + "?" + qs;
                        actionDefinition.method = route.method == null || route.method.equals("*") ? "GET" : route.method.toUpperCase();
                        actionDefinition.star = route.method.equals("*");
                        actionDefinition.action = action;
                        actionDefinition.args = args;
                        return actionDefinition;
                    }
                }
            }
        }
        throw new NoRouteFoundException(action, args);
    }

    public static class ActionDefinition {

        public String method;
        public String url;
        public boolean star;
        public String action;
        public Map<String, Object> args;

        public ActionDefinition add(String key, Object value) {
            args.put(key, value);
            return reverse(action, args);
        }

        public ActionDefinition remove(String key) {
            args.remove(key);
            return reverse(action, args);
        }

        @Override
        public String toString() {
            return url;
        }
    }

    static class Route {

        String method;
        String path;
        String action;
        Pattern actionPattern;
        List<String> actionArgs = new ArrayList<String>();
        String staticDir;
        Pattern pattern;
        List<Arg> args = new ArrayList<Arg>();
        Map<String, String> staticArgs = new HashMap<String, String>();
        static Pattern customRegexPattern = new Pattern("\\{([a-zA-Z_0-9]+)\\}");
        static Pattern argsPattern = new Pattern("\\{<([^>]+)>([a-zA-Z_0-9]+)\\}");
        static Pattern paramPattern = new Pattern("([a-zA-Z_0-9]+):'(.*)'");

        public void compute() {
            // staticDir
            if (action.startsWith("staticDir:")) {
                if (!method.equalsIgnoreCase("*") && !method.equalsIgnoreCase("GET")) {
                    Logger.warn("Static route only support GET method");
                    return;
                }
                if (!this.path.endsWith("/") && !this.path.equals("/")) {
                    this.path += "/";
                }
                this.pattern = new Pattern(path + "({resource}.*)");
                this.staticDir = action.substring("staticDir:".length());
            } else {
                // URL pattern
                String patternString = path;
                patternString = customRegexPattern.replacer("\\{<[^/]+>$1\\}").replace(patternString);
                Matcher matcher = argsPattern.matcher(patternString);
                while (matcher.find()) {
                    Arg arg = new Arg();
                    arg.name = matcher.group(2);
                    arg.constraint = new Pattern(matcher.group(1));
                    args.add(arg);
                }
                patternString = argsPattern.replacer("({$2}$1)").replace(patternString);
                this.pattern = new Pattern(patternString);
                // Action pattern
                patternString = action;
                patternString = patternString.replace(".", "[.]");
                for (Arg arg : args) {
                    if (patternString.contains("{" + arg.name + "}")) {
                        patternString = patternString.replace("{" + arg.name + "}", "({" + arg.name + "}" + arg.constraint.toString() + ")");
                        actionArgs.add(arg.name);
                    }
                }
                actionPattern = new Pattern(patternString, REFlags.IGNORE_CASE);
            }
        }

        public void addParams(String params) {
            if (params == null) {
                return;
            }
            params = params.substring(1, params.length() - 1);
            for (String param : params.split(",")) {
                Matcher matcher = paramPattern.matcher(param);
                if (matcher.matches()) {
                    staticArgs.put(matcher.group(1), matcher.group(2));
                }
            }
        }

        public Map<String, String> matches(String method, String path) {
            if (method == null || this.method.equals("*") || method.equalsIgnoreCase(this.method)) {
                Matcher matcher = pattern.matcher(path);
                if (matcher.matches()) {
                    // Static dir
                    if (staticDir != null) {
                        throw new RenderStatic(staticDir + "/" + matcher.group("resource"));
                    } else {
                        Map<String, String> localArgs = new HashMap<String, String>();
                        for (Arg arg : args) {
                            localArgs.put(arg.name, matcher.group(arg.name));
                        }
                        localArgs.putAll(staticArgs);
                        return localArgs;
                    }
                }
            }
            return null;
        }

        static class Arg {

            String name;
            Pattern constraint;
            String defaultValue;
            Boolean optional = false;
        }

        @Override
        public String toString() {
            return method + " " + path + " -> " + action;
        }
    }
}
