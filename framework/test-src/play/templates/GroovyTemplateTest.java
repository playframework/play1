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
        // make sure our test line is longer then maxPlainTextLength
        assertThat(groovySrc.length()).isGreaterThan( GroovyTemplateCompiler.maxPlainTextLength + 100);

        GroovyTemplate t = new GroovyTemplate("Template_123", groovySrc);
        new GroovyTemplateCompiler().compile(t);

        Map<String, Object> args = new HashMap<String,Object>();
        args.put("name", "Morten");
        assertThat( t.render( args ) ).isEqualTo("hello world"+longString+": Morten");

    }

    @Test
    public void verifyCompilingExtremelyLongLinesWithLinefeed() {

        new PlayBuilder().build();

        // when printing text from template, newlines (0x0d) is transformed into the string '\n'.
        // when breaking lines it is a problem if the '\' is at the end on one line and 'n'
        // is at the beginning of the next line.


        //first we test with just a '\' as last char
        internalVerifyCompilingExtremelyLongLinesWithSpecialCharAsLastCharBeforeBreak('\\');

        // now we test with 0x0d '\n' as last char
        internalVerifyCompilingExtremelyLongLinesWithSpecialCharAsLastCharBeforeBreak('\n');

    }

    private void internalVerifyCompilingExtremelyLongLinesWithSpecialCharAsLastCharBeforeBreak(char lastChar) {
        StringBuilder longString = new StringBuilder();
        for (int i=0;i<1000;i++) {
            longString.append("11111111112222222222333333333344444444445555555555");
            longString.append("11111111112222222222333333333344444444445555555555");
        }

        // now insert a special char on the last line before we split the plainText with new print
        longString.insert(GroovyTemplateCompiler.maxPlainTextLength-1, lastChar);

        String groovySrc = longString+": ${name}";
        // make sure our test line is longer then maxPlainTextLength
        assertThat(groovySrc.length()).isGreaterThan( GroovyTemplateCompiler.maxPlainTextLength + 100);

        GroovyTemplate t = new GroovyTemplate("Template_123", groovySrc);
        new GroovyTemplateCompiler().compile(t);

        Map<String, Object> args = new HashMap<String,Object>();
        args.put("name", "Morten");
        assertThat( t.render( args ) ).isEqualTo(longString+": Morten");
    }

}
