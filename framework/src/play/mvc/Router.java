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
                if(!route.action.startsWith("controllers.")) {
                    route.action = "controllers."+route.action;
                }
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
                request.routeArgs = args;
                request.action = route.action;
                return;
            } 
        }
        throw new NotFound();
    }
    
    public static String reverse(String action, Map<String,String> args) {
        for(Route route : routes) {
            if(route.action.equals(action)) {
                List<String> inPathArgs = new ArrayList<String>();
                boolean allRequiredArgsAreHere = true;
                for(Route.Arg arg : route.args) {
                    inPathArgs.add(arg.name);
                    String value = args.get(arg.name);
                    if(!arg.constraint.matches(value)) {
                        allRequiredArgsAreHere = false;
                        break;
                    }
                }
                if(allRequiredArgsAreHere) {
                    StringBuilder queryString = new StringBuilder();
                    for(String key : args.keySet()) {
                        if(!inPathArgs.contains(key) && args.get(key) != null) {
                            try {
                                queryString.append(URLEncoder.encode(key, "utf-8"));
                                queryString.append("=");
                                queryString.append(URLEncoder.encode(args.get(key), "utf-8"));
                                queryString.append("&");
                            } catch (UnsupportedEncodingException ex) {
                                //
                            }
                        }
                    }
                    String qs = queryString.toString();
                    if(qs.endsWith("&")) {
                        qs = qs.substring(0, qs.length()-1);
                    }
                    return qs.length() == 0 ? route.path : route.path+"?"+qs;
                }
            }
        }
        throw new RuntimeException("No route found");
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
            while(matcher.find()) {
                Arg arg = new Arg();
                arg.name = matcher.group(2);
                arg.constraint = new Pattern(matcher.group(1));
                args.add(arg);
            }
            patternString = argsPattern.replacer("({$2}$1)").replace(patternString);
            this.pattern = new Pattern(patternString);
        }
        
        public Map<String,String> matches(Http.Request request) {
            if(method == null || method.equals("*") || method.equalsIgnoreCase(request.method)) {
                Matcher matcher = pattern.matcher(request.path);
                if(matcher.matches()) {
                    Map<String,String> localArgs = new HashMap<String, String>();
                    for(Arg arg : args) {
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
        
    }

}
