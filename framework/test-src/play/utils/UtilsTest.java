package play.utils;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UtilsTest {

    @Test
    public void mergeSingleValueInMap() {
        Map<String, String[]> map = new HashMap<>();

        Utils.Maps.mergeValueInMap(map, "key1", "value");
        assertThat(map)
            .containsOnly(Map.entry("key1", new String[] { "value" }));

        Utils.Maps.mergeValueInMap(map, "key1", "value");
        assertThat(map)
            .containsOnly(Map.entry("key1", new String[] { "value", "value" }));

        Utils.Maps.mergeValueInMap(map, "key2", "value");
        assertThat(map).containsOnly(
            Map.entry("key1", new String[] { "value", "value" }),
            Map.entry("key2", new String[] { "value" })
        );
    }

    @Test
    public void mergeArrayValuesInMap() {
        Map<String, String[]> map = new HashMap<>();

        Utils.Maps.mergeValueInMap(map, "key1", new String[] { "value" });
        assertThat(map)
            .containsOnly(Map.entry("key1", new String[] { "value" }));

        Utils.Maps.mergeValueInMap(map, "key1", new String[] { "value", "value" });
        assertThat(map)
            .containsOnly(Map.entry("key1", new String[] { "value", "value", "value" }));

        Utils.Maps.mergeValueInMap(map, "key2", new String[]{ "value", "value", "value" });
        assertThat(map).containsOnly(
            Map.entry("key1", new String[] { "value", "value", "value" }),
            Map.entry("key2", new String[] { "value", "value", "value" })
        );
    }

}