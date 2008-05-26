package play.templates;

import java.util.HashMap;
import java.util.Map;

public class Template {
    
    String name;
    String source;
    String groovySource;
    Map<Integer,Integer> linesMatrix = new HashMap<Integer, Integer>();
    
    public Template(String name, String source) {
        this.name = name;
        this.source = source;
    }

    public String render(Map<String,Object> args) {
        return source;
    }

}
