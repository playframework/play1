package utils;

import play.Play;
import play.templates.JavaExtensions;
import scala.collection.SeqLike;
import scala.ScalaObject;

/**
 * Specific extensions for Scala data structures
 */
public class ScalaExtensions extends JavaExtensions {

    public static String pluralize(SeqLike n) {
        return pluralize(n.size());
    }

    public static String pluralize(SeqLike n, String plural) {
        return pluralize(n.size(), plural);
    }

    public static String pluralize(SeqLike n, String[] forms) {
        return pluralize(n.size(), forms);
    }
    
    public static Object object(Class so) throws Exception {
        return Play.classloader.loadClass(so.getName()+"$").getField("MODULE$").get(null);
    }

}
