package play.templates;

import groovy.lang.Closure;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.Scope;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class FastTagsTest {

    private StringWriter out = new StringWriter();

    @Before
    public void setUp() throws Exception {

        Http.Response.current.set(new Http.Response());
        Http.Response.current().encoding = "UTF-8";

        Scope.Session.current.set(new Scope.Session());
        Scope.Session.current().put("___AT", "1234");
    }

    @Test
    public void _form_simple() throws Exception {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "GET";

        Map<String, ?> args = new HashMap<>() {{
            put("arg", actionDefinition);
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), null, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"get\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >" + System.lineSeparator() +
                System.lineSeparator() +
                "</form>", out.toString());
    }

    @Test
    public void _form_withName() throws Exception {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "GET";

        Map<String, ?> args = new HashMap<>() {{
            put("arg", actionDefinition);
            put("name", "my-form");
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), null, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"get\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" name=\"my-form\">" + System.lineSeparator() +
                System.lineSeparator() +
                "</form>", out.toString());
    }

    @Test
    public void _form_post() throws Exception {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "POST";

        Map<String, ?> args = new HashMap<>() {{
            put("arg", actionDefinition);
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), null, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"post\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >" + System.lineSeparator() +
                "<input type=\"hidden\" name=\"authenticityToken\" value=\"1234\"/>" + System.lineSeparator() +
                System.lineSeparator() +
                "</form>", out.toString());
    }

    @Test
    public void _form_starIsPost() throws Exception {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.star = true;

        Map<String, ?> args = new HashMap<>() {{
            put("arg", actionDefinition);
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), null, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"post\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >" + System.lineSeparator() +
                "<input type=\"hidden\" name=\"authenticityToken\" value=\"1234\"/>" + System.lineSeparator() +
                System.lineSeparator() +
                "</form>", out.toString());
    }

    @Test
    public void _form_argMethodOverridesActionDefinitionMethod() throws Exception {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "GET";

        Map<String, ?> args = new HashMap<>() {{
            put("arg", actionDefinition);
            put("method", "POST");
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), null, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"post\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >" + System.lineSeparator() +
                "<input type=\"hidden\" name=\"authenticityToken\" value=\"1234\"/>" + System.lineSeparator() +
                System.lineSeparator() +
                "</form>", out.toString());
    }

    @Test
    public void _form_customArgs() throws Exception {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "GET";

        Map<String, ?> args = new HashMap<>() {{
            put("arg", actionDefinition);
            put("data-customer", "12");
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), null, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"get\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" data-customer=\"12\" >" + System.lineSeparator() +
                System.lineSeparator() +
                "</form>", out.toString());
    }

    @Test
    public void _form_actionAsActionArg() throws Exception {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "GET";

        Map<String, ?> args = new HashMap<>() {{
            put("action", actionDefinition);
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), null, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"get\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >" + System.lineSeparator() +
                System.lineSeparator() +
                "</form>", out.toString());
    }

    @Test
    public void _form_customEnctype() throws Exception {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "GET";

        Map<String, ?> args = new HashMap<>() {{
            put("arg", actionDefinition);
            put("enctype", "xyz");
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), null, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"get\" accept-charset=\"UTF-8\" enctype=\"xyz\" >" + System.lineSeparator() +
                System.lineSeparator() +
                "</form>", out.toString());
    }

    @Test
    public void _form_argAsUrlInsteadOfActionDefinition() throws Exception {
        Map<String, ?> args = new HashMap<>() {{
            put("arg", "/foo/bar");
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), null, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"post\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >" + System.lineSeparator() +
                        "<input type=\"hidden\" name=\"authenticityToken\" value=\"1234\"/>" + System.lineSeparator() +
                        System.lineSeparator() +
                        "</form>", out.toString());
    }
}