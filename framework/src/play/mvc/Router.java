package play.mvc;

import java.io.File;
import java.io.IOException;
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
import play.PlayPlugin;
import play.vfs.VirtualFile;
import play.exceptions.NoRouteFoundException;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.templates.TemplateLoader;
import play.utils.Default;

/**
 * The router matches HTTP requests to action invocations
 */
public class Router {

    static Pattern routePattern = new Pattern("^({method}GET|POST|PUT|DELETE|OPTIONS|HEAD|\\*)[(]?({headers}[^)]*)(\\))?\\s+({path}.*/[^\\s]*)\\s+({action}[^\\s(]+)({params}.+)?(\\s*)$");
    /**
     * Pattern used to locate a method override instruction in request.querystring
     */
    static Pattern methodOverride = new Pattern("^.*x-http-method-override=({method}GET|PUT|POST|DELETE).*$");
    public static long lastLoading = -1;

    public static void load(String prefix) {
        routes.clear();
        parse(Play.routes, prefix);
        lastLoading = System.currentTimeMillis();
        // Plugins
        for (PlayPlugin plugin : Play.plugins) {
            plugin.onRoutesLoaded();
        }
    }

    /**
     * This one can be called to add new route. Last added is first in the route list.
     */
    public static void prependRoute(String method, String path, String action, String headers) {
        prependRoute(method, path, action, null, headers);
    }

    /**
     * This one can be called to add new route. Last added is first in the route list.
     */
    public static void prependRoute(String method, String path, String action) {
        prependRoute(method, path, action, null, null);
    }

    /**
     * Add a route at the given position
     */
    public static void addRoute(int position, String method, String path, String action, String params, String headers) {
        if (position > routes.size()) {
            position = routes.size();
        }
        routes.add(position, getRoute(method, path, action, params, headers));
    }

    /**
     * Add a route at the given position
     */
    public static void addRoute(int position, String method, String path, String headers) {
        addRoute(position, method, path, null, null, headers);
    }

    /**
     * Add a route at the given position
     */
    public static void addRoute(int position, String method, String path, String action, String headers) {
        addRoute(position, method, path, action, null, headers);
    }

    /**
     * Add a new route. Will be first in the route list 
     */
    public static void addRoute(String method, String path, String action) {
        prependRoute(method, path, action);
    }

    /**
     * Add a route at the given position
     */
    public static void addRoute(String method, String path, String action, String headers) {
        addRoute(method, path, action, null, headers);
    }

    /**
     * Add a route
     */
    public static void addRoute(String method, String path, String action, String params, String headers) {
        appendRoute(method, path, action, params, headers, null, 0);
    }

    /**
     * This is used internally when reading the route file. The order the routes are added matters and
     * we want the method to append the routes to the list.
     */
    public static void appendRoute(String method, String path, String action, String params, String headers, String sourceFile, int line) {
        routes.add(getRoute(method, path, action, params, headers, sourceFile, line));
    }

    public static Route getRoute(String method, String path, String action, String params, String headers) {
        return getRoute(method, path, action, params, headers, null, 0);
    }

    public static Route getRoute(String method, String path, String action, String params, String headers, String sourceFile, int line) {
        Route route = new Route();
        route.method = method;
        route.path = path.replace("//", "/");
        route.action = action;
        route.routesFile = sourceFile;
        route.routesFileLine = line;
        route.addFormat(headers);
        route.addParams(params);
        route.compute();
        Logger.trace("Adding [" + route.toString() + "] with params [" + params + "] and headers [" + headers + "]");
        return route;
    }

    /**
     * Add a new route at the beginning of the route list
     */
    public static void prependRoute(String method, String path, String action, String params, String headers) {
        routes.add(0, getRoute(method, path, action, params, headers));
    }

    /**
     * Parse a route file.
     * If an action starts with <i>"plugin:name"</i>, replace that route by the ones declared
     * in the plugin route file denoted by that <i>name</i>, if found.
     *
     * @param routeFile
     * @param prefix    The prefix that the path of all routes in this route file start with. This prefix should not
     *                  end with a '/' character.
     */
    static void parse(VirtualFile routeFile, String prefix) {
        String fileAbsolutePath = routeFile.getRealFile().getAbsolutePath();
        int lineNumber = 0;
        String content = routeFile.contentAsString();
        if (content.indexOf("${") > -1 || content.indexOf("#{") > -1) {
            content = TemplateLoader.load(routeFile).render(new HashMap<String, Object>());
        }
        for (String line : content.split("\n")) {
            lineNumber++;
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
                    String method = matcher.group("method");
                    String path = prefix + matcher.group("path");
                    String params = matcher.group("params");
                    String headers = matcher.group("headers");
                    appendRoute(method, path, action, params, headers, fileAbsolutePath, lineNumber);
                }
            } else {
                Logger.error("Invalid route definition : %s", line);
            }
        }
    }

    public static void detectChanges(String prefix) {
        if (Play.mode == Mode.PROD && lastLoading > 0) {
            return;
        }
        if (Play.routes.lastModified() > lastLoading) {
            load(prefix);
        } else {
            for (VirtualFile file : Play.modulesRoutes.values()) {
                if (file.lastModified() > lastLoading) {
                    load(prefix);
                    return;
                }
            }
        }
    }
    public static List<Route> routes = new ArrayList<Route>(500);

    public static void routeOnlyStatic(Http.Request request) {
        for (Route route : routes) {
            try {
                String format = request.format;
                String host = request.host;
                if (route.matches(request.method, request.path, format, host) != null) {
                    break;
                }
            } catch (Throwable t) {
                if (t instanceof RenderStatic) {
                    throw (RenderStatic) t;
                }
            }
        }
    }

    public static Route route(Http.Request request) {
        Logger.trace("Route: " + request.path + " - " + request.querystring);
        // request method may be overriden if a x-http-method-override parameter is given
        if (request.querystring != null && methodOverride.matches(request.querystring)) {
            Matcher matcher = methodOverride.matcher(request.querystring);
            if (matcher.matches()) {
                Logger.trace("request method %s overriden to %s ", request.method, matcher.group("method"));
                request.method = matcher.group("method");
            }
        }
        for (Route route : routes) {
            String format = request.format;
            String host = request.host;
            Map<String, String> args = route.matches(request.method, request.path, format, host);
            if (args != null) {
                request.routeArgs = args;
                request.action = route.action;
                if (args.containsKey("format")) {
                    request.format = args.get("format");
                }
                if (request.action.indexOf("{") > -1) { // more optimization ?
                    for (String arg : request.routeArgs.keySet()) {
                        request.action = request.action.replace("{" + arg + "}", request.routeArgs.get(arg));
                    }
                }
                return route;
            }
        }
        // Not found - if the request was a HEAD, let's see if we can find a corresponding GET
        if (request.method.equalsIgnoreCase("head")) {
            request.method = "GET";
            Route route = route(request);
            request.method = "HEAD";
            if (route != null) {
                return route;
            }
        }
        throw new NotFound(request.method, request.path);
    }

    public static Map<String, String> route(String method, String path) {
        return route(method, path, null, null);
    }

    public static Map<String, String> route(String method, String path, String headers) {
        return route(method, path, headers, null);
    }

    public static Map<String, String> route(String method, String path, String headers, String host) {
        for (Route route : routes) {
            Map<String, String> args = route.matches(method, path, headers, host);
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

    public static String reverse(VirtualFile file) {
        return reverse(file, false);
    }

    public static String reverse(VirtualFile file, boolean absolute) {
        if (file == null || !file.exists()) {
            throw new NoRouteFoundException("File not found (" + file + ")");
        }
        String path = file.relativePath();
        path = path.substring(path.indexOf("}") + 1);
        for (Route route : routes) {
            String staticDir = route.staticDir;
            if (staticDir != null) {
                if (!staticDir.startsWith("/")) {
                    staticDir = "/" + staticDir;
                }
                if (!staticDir.equals("/") && !staticDir.endsWith("/")) {
                    staticDir = staticDir + "/";
                }
                if (path.startsWith(staticDir)) {
                    String to = route.path + path.substring(staticDir.length());
                    if (to.endsWith("/index.html")) {
                        to = to.substring(0, to.length() - "/index.html".length() + 1);
                    }
                    if(absolute) {
                        if(route.host != null && !route.host.equals(".*")) {
                            to = "http://" + route.host + to;
                        } else {
                            to = Http.Request.current().getBase() + to;
                        }
                    }
                    return to;
                }
            }
        }
        throw new NoRouteFoundException(file.relativePath());
    }

    public static String reverseWithCheck(String name, VirtualFile file, boolean absolute) {
        if (file == null || !file.exists()) {
            throw new NoRouteFoundException(name + " (file not found)");
        }
        return reverse(file, absolute);
    }

    public static ActionDefinition reverse(String action, Map<String, Object> args) {
        if (action.startsWith("controllers.")) {
            action = action.substring(12);
        }
        Map<String, Object> argsbackup = args;
        // Add routeArgs
        if (Scope.RouteArgs.current() != null) {
            argsbackup.putAll(Scope.RouteArgs.current().data);
        }
        for (Route route : routes) {
            args = new HashMap<String, Object>(argsbackup);
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
                            if (value instanceof List<?>) {
                                @SuppressWarnings("unchecked")
                                List<Object> l = (List<Object>) value;
                                value = l.get(0);
                            }
                            if (!value.toString().startsWith(":") && !arg.constraint.matches(value.toString())) {
                                allRequiredArgsAreHere = false;
                                break;
                            }
                        }
                    }
                    // les parametres codes en dur dans la route matchent-ils ?
                    for (String staticKey : route.staticArgs.keySet()) {
                        if (staticKey.equals("format")) {
                            if (!Http.Request.current().format.equals(route.staticArgs.get("format"))) {
                                allRequiredArgsAreHere = false;
                                break;
                            }
                            continue; // format is a special key
                        }
                        if (!args.containsKey(staticKey) || args.get(staticKey) == null || !args.get(staticKey).toString().equals(route.staticArgs.get(staticKey))) {
                            allRequiredArgsAreHere = false;
                            break;
                        }
                    }
                    if (allRequiredArgsAreHere) {
                        StringBuilder queryString = new StringBuilder();
                        String path = route.path;
                        String host = route.host;
                        if (path.endsWith("/?")) {
                            path = path.substring(0, path.length() - 2);
                        }
                        for (Map.Entry<String, Object> entry : args.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();
                            if (inPathArgs.contains(key) && value != null) {
                                if (List.class.isAssignableFrom(value.getClass())) {
                                    @SuppressWarnings("unchecked")
                                    List<Object> vals = (List<Object>) value;
                                    path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", vals.get(0).toString().replace("$", "\\$") + "");
                                } else {
                                    path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", value.toString().replace("$", "\\$") + "");
                                    host = host.replaceAll("\\{(<[^>]+>)?" + key + "\\}", value.toString().replace("$", "\\$") + "");
                                }
                            } else if (route.staticArgs.containsKey(key)) {
                                // Do nothing -> The key is static
                            } else if (value != null) {
                                if (List.class.isAssignableFrom(value.getClass())) {
                                    @SuppressWarnings("unchecked")
                                    List<Object> vals = (List<Object>) value;
                                    for (Object object : vals) {
                                        try {
                                            queryString.append(URLEncoder.encode(key, "utf-8"));
                                            queryString.append("=");
                                            if (object.toString().startsWith(":")) {
                                                queryString.append(object.toString() + "");
                                            } else {
                                                queryString.append(URLEncoder.encode(object.toString() + "", "utf-8"));
                                            }
                                            queryString.append("&");
                                        } catch (UnsupportedEncodingException ex) {
                                        }
                                    }
                                } else if (value.getClass().equals(Default.class)) {
                                    // Skip defaults in queryString
                                } else {
                                    try {
                                        queryString.append(URLEncoder.encode(key, "utf-8"));
                                        queryString.append("=");
                                        if (value.toString().startsWith(":")) {
                                            queryString.append(value.toString() + "");
                                        } else {
                                            queryString.append(URLEncoder.encode(value.toString() + "", "utf-8"));
                                        }
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
                        actionDefinition.star = "*".equals(route.method);
                        actionDefinition.action = action;
                        actionDefinition.args = args;
                        actionDefinition.host = host;
                        return actionDefinition;
                    }
                }
            }
        }
        throw new NoRouteFoundException(action, args);
    }

    public static class ActionDefinition {

        public String host;
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

        public ActionDefinition addRef(String fragment) {
            url += "#" + fragment;
            return this;
        }

        @Override
        public String toString() {
            return url;
        }

        public void absolute() {
            if(host == null || host.equals(".*")) {
                url = Http.Request.current().getBase() + url;
            } else {
                url = "http://" + host + url;
            }
        }

        public ActionDefinition secure() {
            if (!url.contains("http://") && !url.contains("https://")) {
                absolute();
            }
            url = url.replace("http:", "https:");
            return this;
        }
    }

    public static class Route {

        public String method;
        public String path;
        public String action;
        Pattern actionPattern;
        List<String> actionArgs = new ArrayList<String>();
        String staticDir;
        Pattern pattern;
        Pattern hostPattern;
        List<Arg> args = new ArrayList<Arg>();
        Map<String, String> staticArgs = new HashMap<String, String>();
        List<String> formats = new ArrayList<String>();
        String host;
        Arg hostArg = null;
        public int routesFileLine;
        public String routesFile;
        static Pattern customRegexPattern = new Pattern("\\{([a-zA-Z_0-9]+)\\}");
        static Pattern argsPattern = new Pattern("\\{<([^>]+)>([a-zA-Z_0-9]+)\\}");
        static Pattern paramPattern = new Pattern("([a-zA-Z_0-9]+):'(.*)'");

        public void compute() {
            this.host = ".*";
            this.hostPattern = new Pattern(host);
            // staticDir
            if (action.startsWith("staticDir:")) {
                // Is there is a host argument, append it.
                if (!path.startsWith("/")) {
                    String p = this.path;
                    this.path = p.substring(p.indexOf("/"));
                    this.host = p.substring(0, p.indexOf("/"));
                    if(this.host.contains("{")) {
                        Logger.warn("Static route cannot have a dynamic host name");
                        return;
                    }
                }
                if (!method.equalsIgnoreCase("*") && !method.equalsIgnoreCase("GET")) {
                    Logger.warn("Static route only support GET method");
                    return;
                }
                if (!this.path.endsWith("/") && !this.path.equals("/")) {
                    Logger.warn("The path for a staticDir route must end with / (%s)", this);
                    this.path += "/";
                }
                this.pattern = new Pattern("^" + path + "({resource}.*)$");
                this.staticDir = action.substring("staticDir:".length());
            } else {
                // URL pattern
                // Is there is a host argument, append it.
                if (!path.startsWith("/")) {
                    String p = this.path;
                    this.path = p.substring(p.indexOf("/"));
                    this.host = p.substring(0, p.indexOf("/"));

                    Matcher m = new Pattern(".*\\{({name}.*)\\}.*").matcher(host);

                    if (m.matches()) {
                        String name = m.group("name");
                        hostArg = new Arg();
                        hostArg.name = name;
                        hostArg.constraint = new Pattern(".*");
                        args.add(hostArg);
                    }
                    this.hostPattern = new Pattern(this.host.replaceFirst("\\{.*\\}", ""));
                }
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
            if (params == null || params.length() < 1) {
                return;
            }
            params = params.substring(1, params.length() - 1);
            for (String param : params.split(",")) {
                Matcher matcher = paramPattern.matcher(param);
                if (matcher.matches()) {
                    staticArgs.put(matcher.group(1), matcher.group(2));
                } else {
                    Logger.warn("Ignoring %s (static params must be specified as key:'value',...)", params);
                }
            }
        }

        // TODO: Add args names
        public void addFormat(String params) {
            if (params == null || params.length() < 1) {
                return;
            }
            params = params.trim();
            for (String param : params.split(",")) {
                formats.add(param);
            }
        }

        private boolean contains(String accept) {
            boolean contains = (accept == null);
            if (accept != null) {
                if (this.formats.size() == 0) {
                    return true;
                }
                for (String format : this.formats) {
                    contains = format.startsWith(accept);
                    if (contains) {
                        break;
                    }
                }
            }
            return contains;
        }

        public Map<String, String> matches(String method, String path) {
            return matches(method, path, null, null);
        }

        public Map<String, String> matches(String method, String path, String accept) {
            return matches(method, path, accept, null);
        }

        public Map<String, String> matches(String method, String path, String accept, String host) {
            // Normalize
            if(path.equals(Play.ctxPath)) {
                path = path + "/";
            }
            // If method is HEAD and we have a GET
            if (method == null || this.method.equals("*") || method.equalsIgnoreCase(this.method) || (method.equalsIgnoreCase("head") && ("get").equalsIgnoreCase(this.method))) {

                Matcher matcher = pattern.matcher(path);

                boolean hostMatches = (host == null);
                if (host != null) {
                    Matcher hostMatcher = hostPattern.matcher(host);
                    hostMatches = hostMatcher.matches();
                }
                // Extract the host variable
                if (matcher.matches() && contains(accept) && hostMatches) {
                    // Static dir
                    if (staticDir != null) {
                        String resource = matcher.group("resource");
                        try {
                            String root = new File(staticDir).getCanonicalPath();
                            String child = new File(staticDir + "/" + resource).getCanonicalPath();
                            if (child.startsWith(root)) {
                                throw new RenderStatic(staticDir + "/" + resource);
                            }
                        } catch (IOException e) {
                        }
                        throw new NotFound(resource);
                    } else {
                        Map<String, String> localArgs = new HashMap<String, String>();
                        for (Arg arg : args) {
                            localArgs.put(arg.name, matcher.group(arg.name));
                        }
                        if (hostArg != null && host != null) {
                            localArgs.put(hostArg.name, host);
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
