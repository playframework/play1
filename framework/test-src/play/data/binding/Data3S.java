package play.data.binding;


import java.util.HashMap;
import java.util.Map;

class Data3S {
    public String a;

    @AttributeStripping
    public Map<String, String> map = new HashMap<String, String>();
}
