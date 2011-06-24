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

    @Test
    public void verifyCompilingExtremelyLongLines() {

        new PlayBuilder().build();

        StringBuilder longString = new StringBuilder();
        for (int i=0;i<1000;i++) {
            longString.append("11111111112222222222333333333344444444445555555555");
            longString.append("11111111112222222222333333333344444444445555555555");
        }

        String groovySrc = "hello world"+longString+": ${name}";
        assertThat(groovySrc.length()).isGreaterThan(65535);

        GroovyTemplate t = new GroovyTemplate("Template_123", groovySrc);
        new GroovyTemplateCompiler().compile(t);

        Map<String, Object> args = new HashMap<String,Object>();
        args.put("name", "Morten");
        assertThat( t.render( args ) ).isEqualTo("hello world"+longString+": Morten");

    }
}
