package play.mvc;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jregex.Matcher;
import jregex.Pattern;
import play.Logger;
import play.Play;
import play.exceptions.EmptyAppException;
import play.vfs.VirtualFile;
import play.exceptions.NoRouteFoundException;
import play.mvc.results.NotFound;

public class Router {

    static Pattern routePattern = new Pattern("^({method}[A-Za-z\\*]+)?\\s+({path}/[^\\s]*)\\s*({action}[^\\s(]+)({params}.+)?$");
    static long lastLoading;

    public static void load() {
        routes.clear();
        String config = "";
        for (VirtualFile file : Play.routes) {
            config += file.contentAsString() + "\n";
        }
        String[] lines = config.split("\n");
        for (String line : lines) {
            line = line.trim().replaceAll("\\s+", " ");
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            Matcher matcher = routePattern.matcher(line);
            if (matcher.matches()) {
                Route route = new Route();
                route.method = matcher.group("method");
                route.path = matcher.group("path");
                route.action = matcher.group("action");
                route.compute();
                routes.add(route);
            } else {
                Logger.warn("Invalid route definition : %s", line);
            }
        }
        lastLoading = System.currentTimeMillis();
    }

    public static void detectChanges() {
        for (VirtualFile file : Play.routes) {
            if (file.lastModified() > lastLoading) {
                load();
                return;
            }
        }
    }
    static List<Route> routes = new ArrayList<Route>();

    public static void route(Http.Request request) {
        if (routes.isEmpty()) {
            throw new EmptyAppException();
        }
        for (Route route : routes) {
            Map<String, String> args = route.matches(request.method, request.path);
            if (args != null) {
                request.routeArgs = args;
                request.action = route.action;
                return;
            }
        }
        throw new NotFound(request.method, request.path);
    }

    public static Map<String, String> route(String method, String path) {
        if (routes.isEmpty()) {
            throw new EmptyAppException();
        }
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
            if (route.action.equals(action)) {
                List<String> inPathArgs = new ArrayList<String>();
                boolean allRequiredArgsAreHere = true;
                for (Route.Arg arg : route.args) {
                    inPathArgs.add(arg.name);
                    String value = args.get(arg.name) == null ? null : args.get(arg.name) + "";
                    if (value == null || !arg.constraint.matches(value)) {
                        allRequiredArgsAreHere = false;
                        break;
                    }
                }
                if (allRequiredArgsAreHere) {
                    StringBuilder queryString = new StringBuilder();
                    String path = route.path;
                    for (String key : args.keySet()) {
                        if (inPathArgs.contains(key) && args.get(key) != null) {
                            path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", args.get(key) + "");
                        } else if (args.get(key) != null) {
                            try {
                                queryString.append(URLEncoder.encode(key, "utf-8"));
                                queryString.append("=");
                                queryString.append(URLEncoder.encode(args.get(key) + "", "utf-8"));
                                queryString.append("&");
                            } catch (UnsupportedEncodingException ex) {
                                //
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
                    return actionDefinition;
                }
            }
        }
        throw new NoRouteFoundException(action, args);
    }

    public static class ActionDefinition {

        public String method;
        public String url;

        @Override
        public String toString() {
            return url;
        }
    }

    static class Route {

        String method;
        String path;
        String action;
        Pattern pattern;
        List<Arg> args = new ArrayList<Arg>();
        static Pattern customRegexPattern = new Pattern("\\{([a-zA-Z_0-9]+)\\}");
        static Pattern argsPattern = new Pattern("\\{<([^>]+)>([a-zA-Z_0-9]+)\\}");

        public void compute() {
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
        }

        public Map<String, String> matches(String method, String path) {
            if (method == null || this.method.equals("*") || method.equalsIgnoreCase(this.method)) {
                Matcher matcher = pattern.matcher(path);
                if (matcher.matches()) {
                    Map<String, String> localArgs = new HashMap<String, String>();
                    for (Arg arg : args) {
                        localArgs.put(arg.name, matcher.group(arg.name));
                    }
                    return localArgs;
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
