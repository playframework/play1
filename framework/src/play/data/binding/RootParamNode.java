package play.data.binding;

import java.util.Map;

public class RootParamNode extends ParamNode {

    public final Map<String, String[]> originalParams;
    public RootParamNode(Map<String, String[]> originalParams) {
        super("root");
        this.originalParams = originalParams;
    }
}
