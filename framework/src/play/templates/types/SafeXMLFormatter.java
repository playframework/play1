package play.templates.types;

import org.apache.commons.lang.StringEscapeUtils;
import play.templates.SafeFormatter;
import play.templates.TagContext;
import play.templates.Template;

public class SafeXMLFormatter implements SafeFormatter {

    public String format(Template template, Object value) {
        if (value != null) {
            if (TagContext.hasParentTag("verbatim")) {
                return value.toString();
            }
            return StringEscapeUtils.escapeXml(value.toString());
        }
        return "";
    }
}
