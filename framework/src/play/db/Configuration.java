package play.db;

import javax.persistence.*;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import org.apache.commons.lang.*;
import play.*;

import jregex.Matcher;

public class Configuration {

     public static Properties convertToMultiDB(Properties p) {
        Properties newProperties = new Properties();
        newProperties.putAll(p);
        final String OLD_DB_CONFIG_PATTERN = "^db\\.([^\\.]*)$";
        for (String property : newProperties.stringPropertyNames()) {
            Matcher m = new jregex.Pattern(OLD_DB_CONFIG_PATTERN).matcher(property);
            if (m.matches()) {
                //String[] name = property.split("\\.");
                newProperties.put("db.default." + m.group(1), newProperties.get(property));
                //newProperties.remove(property);
            }
             // Special case db=...
            if ("db".equals(property)) {
                newProperties.put("db.default", newProperties.get(property));
               // newProperties.remove(property);
            }
        }
       return newProperties;
    }

    public static List<String> getDbNames(Properties p) {
        TreeSet<String> dbNames = new TreeSet<String>();
        final String DB_CONFIG_PATTERN = "^db\\.([^\\.]*)\\.([^\\.]*)$";
        for (String property : p.stringPropertyNames()) {
            Matcher m = new jregex.Pattern(DB_CONFIG_PATTERN).matcher(property);
            if (m.matches()) {
                dbNames.add(m.group(1));
            }
            // Special case db=...
            if ("db".equals(property)) {
                dbNames.add("default");
            }
        }
        return new ArrayList<String>(dbNames);
    }


    public static Map<String, String> getProperties(String name) {
        Map<String, String> properties = new HashMap<String, String>();
        Properties props = Play.configuration;
        for (Object key : Collections.list(props.keys())) {
            if (key.toString().startsWith("db." + name)) {
                properties.put(key.toString(), props.get(key).toString());
            }
        } 
        return properties;
    }

    public static Map<String, String> addHibernateProperties(Map<String, String> props, String dbname) {
        Map<String, String> properties = new HashMap<String, String>();
        properties.putAll(props);
        for (Object key : new ArrayList(props.keySet())) {
            if (key.toString().startsWith(dbname + ".hibernate")) {
                String newKey = key.toString().replace("$([^\\.]*\\.)","");
                properties.put(newKey, props.get(key).toString());
            }
        } 
        return properties;
    }

}
