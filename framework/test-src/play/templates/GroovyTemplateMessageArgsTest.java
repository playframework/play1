package play.templates;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import play.PlayBuilder;
import play.i18n.Messages;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Arguments of &amp;{'key', arg} are escaped like ${arg} in HTML templates (#1038).
 */
public class GroovyTemplateMessageArgsTest {

    private Properties previousMessages;

    @BeforeEach
    public void init() {
        new PlayBuilder().build();
        previousMessages = Messages.defaults;
        Messages.defaults = new Properties();
        Messages.defaults.setProperty("greet", "Hello %s");
        Messages.defaults.setProperty("greet.html", "<b>Hello</b> %s");
        Messages.defaults.setProperty("count", "%d items for %s");
        Messages.defaults.setProperty("year", "Year %tY");
        Messages.defaults.setProperty("plain", "<i>no args</i>");
    }

    @AfterEach
    public void restore() {
        Messages.defaults = previousMessages;
    }

    private static String render(String templateName, String source, Object name) {
        GroovyTemplate t = new GroovyTemplate(templateName, source);
        new GroovyTemplateCompiler().compile(t);
        Map<String, Object> args = new HashMap<>();
        args.put("name", name);
        return t.render(args);
    }

    @Test
    public void stringArgIsEscapedInHtmlTemplate() {
        assertThat(render("msg.html", "&{'greet', name}", "<b>Tom & Jerry</b>"))
                .isEqualTo("Hello &lt;b&gt;Tom &amp; Jerry&lt;/b&gt;");
    }

    @Test
    public void gstringArgIsEscapedInHtmlTemplate() {
        assertThat(render("msg.html", "&{'greet', \"$name!\"}", "<script>"))
                .isEqualTo("Hello &lt;script&gt;!");
    }

    @Test
    public void otherObjectArgIsEscapedLikeDollarExpression() {
        Object user = new Object() {
            @Override
            public String toString() {
                return "<img src=x>";
            }
        };
        assertThat(render("msg.html", "&{'greet', name}", user)).isEqualTo("Hello &lt;img src=x&gt;");
        assertThat(render("msg.html", "${name}", user)).isEqualTo("&lt;img src=x&gt;");
    }

    @Test
    public void rawArgIsNotEscaped() {
        assertThat(render("msg.html", "&{'greet', name.raw()}", "<b>bold</b>")).isEqualTo("Hello <b>bold</b>");
        assertThat(render("msg.html", "&{'greet', name}", new BaseTemplate.RawData("<b>bold</b>"))).isEqualTo("Hello <b>bold</b>");
    }

    @Test
    public void messageTextIsNotEscaped() {
        assertThat(render("msg.html", "&{'greet.html', name}", "<i>")).isEqualTo("<b>Hello</b> &lt;i&gt;");
        assertThat(render("msg.html", "&{'plain'}", null)).isEqualTo("<i>no args</i>");
    }

    @Test
    public void numberAndDateFormatsStillWork() {
        assertThat(render("msg.html", "&{'count', 42, name}", "<x>")).isEqualTo("42 items for &lt;x&gt;");
        assertThat(render("msg.html", "&{'count', '7', name}", "y")).isEqualTo("7 items for y");
        Calendar cal = new GregorianCalendar(2026, Calendar.JANUARY, 2);
        assertThat(render("msg.html", "&{'year', name}", cal)).isEqualTo("Year 2026");
        assertThat(render("msg.html", "&{'year', name}", cal.getTime())).isEqualTo("Year 2026");
        assertThat(render("msg.html", "&{'greet', name}", null)).isEqualTo("Hello null");
    }

    @Test
    public void escapedArgIsNotExpandedAsMessageReference() {
        assertThat(render("msg.html", "&{'greet', name}", "&{plain}")).isEqualTo("Hello &amp;{plain}");
    }

    @Test
    public void argIsNotEscapedInNonHtmlTemplate() {
        assertThat(render("msg.txt", "&{'greet', name}", "<b>Tom & Jerry</b>")).isEqualTo("Hello <b>Tom & Jerry</b>");
    }

    @Test
    public void argIsNotEscapedInsideVerbatim() {
        assertThat(render("msg.html", "#{verbatim}&{'greet', name}#{/verbatim}", "<b>bold</b>").trim())
                .isEqualTo("Hello <b>bold</b>");
    }
}
