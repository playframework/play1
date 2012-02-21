import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import play.templates.GroovyTemplate;
import play.templates.GroovyTemplateCompiler;
import play.test.UnitTest;

public class TemplateClassBindingTest extends UnitTest {

    public static class StaticInnerClass {
        public final int val;
        public StaticInnerClass() {
            val = 42;
        }
    }

    @Test
    public void testDynamicClassBindingWithNew() {
        final String source = "${ new TemplateClassBindingTest.StaticInnerClass().val }";
        GroovyTemplate groovyTemplate = new GroovyTemplate("dynamic_class_binding_with_new", source);
        new GroovyTemplateCompiler().compile(groovyTemplate);
        assertEquals("42", groovyTemplate.render());
    }

    @Test
    public void testDynamicClassBindingWithInstanceOf() {
        StaticInnerClass staticInnerClass = new StaticInnerClass();
        final String source = "${ staticInnerClass instanceof TemplateClassBindingTest.StaticInnerClass }";
        GroovyTemplate groovyTemplate = new GroovyTemplate("dynamic_class_binding_with_instanceof", source);
        new GroovyTemplateCompiler().compile(groovyTemplate);
        Map<String, Object> args = new HashMap<String,Object>();
        args.put("staticInnerClass", staticInnerClass);
        assertEquals("true", groovyTemplate.render(args));
    }

    @Test
    public void testDynamicClassBindingWithDotClass() {
        StaticInnerClass staticInnerClass = new StaticInnerClass();
        final String source = "${ staticInnerClass.getClass() == TemplateClassBindingTest.StaticInnerClass.class }";
        GroovyTemplate groovyTemplate = new GroovyTemplate("dynamic_class_binding_with_dot_class", source);
        new GroovyTemplateCompiler().compile(groovyTemplate);
        Map<String, Object> args = new HashMap<String,Object>();
        args.put("staticInnerClass", staticInnerClass);
        assertEquals("true", groovyTemplate.render(args));
    }

}
