package play.utils;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;

public class SafeFormatterTest {
    private void checkFormat(SafeFormatter s, String format, Object... args) {
        String expected = String.format(format, args);
        String actual = s.format(format, args);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testSafeFormatter() {
        SafeFormatter s = new SafeFormatter() {
            @Override
            public String appendArgument(String format, Object arg) {
                return HTML.htmlEscape(String.format(format, arg));
            }

            @Override
            public String append(String value) {
                return value;
            }
        };

        // Check if String.format and SafeFormatter.format return the same
        // result when there is nothing to escape
        checkFormat(s, "%n", new Object[] { null });
        checkFormat(s, "%n", (Object[]) null);

        checkFormat(s, "%s", new Object[] { null });
        checkFormat(s, "%s", (Object[]) null);

        checkFormat(s, "%4$s %3$s %2$s %1$s %4$s %3$s %2$s %1$s", "a", "b", "c", "d");
        checkFormat(s, "%s %s %<s %<s", "a", "b", "c", "d");
        checkFormat(s, "%s %s %s %s", "a", "b", "c", "d");
        checkFormat(s, "%2$s %s %<s %s", "a", "b", "c", "d");

        checkFormat(s, "%s %s %n", "a", "b");
        checkFormat(s, "%s %s %%", "a", "b");
        checkFormat(s, "%s %s %%n", "a", "b");
        checkFormat(s, "%s %s %%%n", "a", "b");

        checkFormat(s, "%1$tm %1$te,%1$tY", new Date());

        // Check if only the parameters are escaped
        Assert.assertEquals("<br>&lt;br&gt;<br>", s.format("<br>%s<br>", "<br>"));
    }
}