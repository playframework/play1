package play.mvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jregex.Matcher;
import jregex.Pattern;
import play.Logger;
import play.Play.VirtualFile;
import play.mvc.results.NotFound;

public class Router {
    
    static Pattern routePattern = new Pattern("^({method}[A-Za-z\\*]+)?\\s+({path}/[^\\s]*)\\s*({action}[^\\s(]+)({params}.+)?$");

    public static void load(VirtualFile routesFile) {  
        routes.clear();
        String[] lines = routesFile.contentAsString().split("\n");
        for(String line : lines) {
            line = line.trim().replaceAll("\\s+", " ");
            if(line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            Matcher matcher = routePattern.matcher(line);
            if(matcher.matches()) {
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
    }
    
    static List<Route> routes = new ArrayList<Route>();
    
    public static void route(Http.Request request) {
        for(Route route : routes) {
            Map<String,String> args = route.matches(request);
            if(args != null) {
                request.action = route.action;
                return;
            } 
        }
        throw new NotFound();
    }
    
    static class Route {
        public String method;
        public String path;
        public String action;
        public Pattern pattern;
        public Map<String,String> args = new HashMap<String, String>();
        
        static Pattern customRegexPattern = new Pattern("\\{([a-zA-Z_0-9]+)\\}");
        static Pattern argsPattern = new Pattern("\\{<([^>]+)>([a-zA-Z_0-9]+)\\}");
        
        public void compute() {
            String patternString = path;
            patternString = customRegexPattern.replacer("\\{<[^/]+>$1\\}").replace(patternString);
            Matcher matcher = argsPattern.matcher(patternString);
            while(matcher.find()) {
                args.put(matcher.group(2), null);
            }
            patternString = argsPattern.replacer("({$2}$1)").replace(patternString);
            this.pattern = new Pattern(patternString);
        }
        
        public Map<String,String> matches(Http.Request request) {
            if(method == null || method.equals("*") || method.equalsIgnoreCase(request.method)) {
                Matcher matcher = pattern.matcher(request.path);
                if(matcher.matches()) {
                    Map<String,String> localArgs = new HashMap<String, String>(this.args);
                    for(String group : args.keySet()) {
                        localArgs.put(group, matcher.group(group));
                    }
                    return localArgs;
                }
            }
            return null;
        }
        
    }

}
