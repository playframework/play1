package play.templates;

import java.util.Map;

public abstract class Template {

    public String name;
    public String source;

    public abstract void compile();
    public abstract String render(Map<String, Object> args);

}
