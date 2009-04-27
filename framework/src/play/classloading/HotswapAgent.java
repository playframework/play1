package play.classloading;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * Enable HotSwap when it's possible.
 */
public class HotswapAgent {

    static Instrumentation instrumentation;
    public static boolean enabled = false;

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        HotswapAgent.instrumentation = instrumentation;
        HotswapAgent.enabled = true;
    }

    public static void reload(ClassDefinition... definitions) throws UnmodifiableClassException, ClassNotFoundException {
        instrumentation.redefineClasses(definitions);
    }
}
