package play.templates;

public class JavaExtensions {
    
    public static String escape(String htmlToEscape) {
        return htmlToEscape.replace("<", "&lt;").replace(">", "&gt;");
    }

}
