package play.classloading.enhancers.fixtures;

/**
 * Plain class with public fields and NO hand-written accessors.
 * Used by PropertiesEnhancerTest to verify that the enhancer generates
 * getters, setters, and a no-arg constructor, and rewrites field access.
 */
public class SimpleBeanFixture {
    public String name;
    public int count;
    public boolean active;

    // No explicit constructor — enhancer should leave the implicit one alone.
    // No getters/setters — enhancer should generate them.
}
