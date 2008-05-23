package play.mvc;

import java.util.ArrayList;
import java.util.List;
import jregex.Matcher;
import jregex.Pattern;
import play.Logger;
import play.Play.VirtualFile;

public class Router {
    
    static Pattern routePattern = new Pattern("");

    public static void load(VirtualFile routesFile) {  
        routes.clear();
        String[] lines = routesFile.contentAsString().split("\n");
        for(String line : lines) {
            line = line.trim();
            if(line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            Matcher matcher = routePattern.matcher(line);
            if(matcher.matches()) {
                
            } else {
                Logger.warn("Invalid route definition : %s", line);
            }
        }
    }
    
    static List<Route> routes = new ArrayList<Route>();
    
    public static void route(Http.Request request) {
        
    }
    
    static class Route {
        public String method;
        public String path;
        public String action;        
        
    }

}
