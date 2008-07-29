/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package play;

import java.io.StringReader;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
import play.data.parsing.JsonParser;

/**
 *
 * @author lwe
 */
public class JsonParserTest {

    Map<String, String[]> result = null;

    public JsonParserTest() {
    }

    public Map<String, String[]> parseProxy(String json) {
        JsonParser instance = new JsonParser();
        result = instance.parse(new StringReader(json));
        return result;
    }

    @Test
    public void testSimplekeys() {
        parseProxy("{\"name\":\"BORT\",\"firstname\":\"Guillaume\", \"age\":26 }");
        assertEquals("BORT", result.get("name")[0]);
        assertEquals("Guillaume", result.get("firstname")[0]);
        assertEquals("26", result.get("age")[0]);

    }

    @Test
    public void testParse() {

        parseProxy("{\"user\":{\"name\":\"BORT\",\"firstname\":\"Guillaume\", \"age\":26 }}");
        assertEquals("BORT", result.get("user.name")[0]);
        assertEquals("Guillaume", result.get("user.firstname")[0]);
        assertEquals("26", result.get("user.age")[0]);

        parseProxy("{\"user\":null}");
        assertNull("user should be null", result.get("user")[0]);

        parseProxy("{\"user\":{\"name\":\"BORT\",friends:[\"Leo\",\"Jeff\"]}, activeteams:[\"Zenexity\",\"Foo\"]}");
        assertEquals("Leo", result.get("user.friends[0]")[0]);
        assertEquals("Jeff", result.get("user.friends[1]")[0]);

        parseProxy("{\"user\":{\"name\":\"BORT\"}, teams:[{name:\"Zenexity\",role:\"PUBLISHER\"}]}");
        assertEquals("Zenexity", result.get("teams[0].name")[0]);
        assertEquals("PUBLISHER", result.get("teams[0].role")[0]);

    }

    @Test
    public void testMatrix() {
        parseProxy("{\"tabintab\":[ [\"0,0\",\"0,1\",\"0,2\"] , [\"1,0\",\"1,1\",\"1,2\"]]}");
        assertEquals("0,0", result.get("tabintab[0][0]")[0]);
        assertEquals("1,2", result.get("tabintab[1][2]")[0]);
    }

    @Test
    public void testComposedKeys() {

        parseProxy("{user.name:\"BORT\",user.firstname:\"Guillaume\" }");
        assertEquals("BORT", result.get("user.name")[0]);
    }
}