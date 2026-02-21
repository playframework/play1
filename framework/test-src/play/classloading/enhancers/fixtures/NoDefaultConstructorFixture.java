package play.classloading.enhancers.fixtures;

/**
 * Class with only a parameterized constructor — no no-arg constructor.
 * Used by PropertiesEnhancerTest to verify the enhancer adds a default
 * constructor, which JPA and data binding require.
 */
public class NoDefaultConstructorFixture {
    public String value;

    public NoDefaultConstructorFixture(String value) {
        this.value = value;
    }
}
