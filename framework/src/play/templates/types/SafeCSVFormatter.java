package play.templates.types;

import org.apache.commons.lang.StringEscapeUtils;
import play.templates.SafeFormatter;
import play.templates.Template;

public class SafeCSVFormatter implements SafeFormatter {

    @Override
    public String format(Template template, Object value) {
        if (value != null) {
            return StringEscapeUtils.escapeCsv(value.toString());   
        }
        return "";
    }
}
