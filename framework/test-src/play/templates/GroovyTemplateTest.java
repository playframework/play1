package play.templates;

import org.junit.Test;
import play.PlayBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class GroovyTemplateTest {

    @Test
    public void verifyRenderingTwice() {

        new PlayBuilder().build();

        String groovySrc = "hello world: ${name}";

        GroovyTemplate t = new GroovyTemplate("Template_123", groovySrc);
        new GroovyTemplateCompiler().compile(t);

        Map<String, Object> args = new HashMap<String,Object>();
        args.put("name", "Morten");
        assertThat( t.render( args ) ).isEqualTo("hello world: Morten");

        //do it again
        assertThat( t.render( args ) ).isEqualTo("hello world: Morten");

    }
}
