package play.templates.types;

import play.templates.SafeFormatter;
import play.templates.TagContext;
import play.templates.Template;
import play.utils.HTML;

public class SafeHTMLFormatter implements SafeFormatter {

    @Override
    public String format(Template template, Object value) {
        if (value != null) {
            if (TagContext.hasParentTag("verbatim")) {
                return value.toString();
            }
            return HTML.htmlEscape(value.toString());
        }
        return "";
    }
}
