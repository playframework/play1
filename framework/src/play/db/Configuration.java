package play.db;

import java.util.*;
import java.util.Collections;

import play.*;
import jregex.Matcher;
import jregex.Pattern;

public class Configuration {

     public static Properties convertToMultiDB(Properties p) {
        final String OLD_DB_CONFIG_PATTERN = "^db\\.([^\\.]*)$";
        final String OLD_JPA_CONFIG_PATTERN = "^jpa\\.([^\\.]*)$";
        final String OLD_HIBERNATE_CONFIG_PATTERN = "^hibernate\\.([a-zA-Z.-_]*)$";
        
        Properties newProperties = convertPattern(p, OLD_DB_CONFIG_PATTERN, "db.default");
        newProperties = convertPattern(newProperties, OLD_JPA_CONFIG_PATTERN, "jpa.default");  
        newProperties = convertPattern(newProperties, OLD_HIBERNATE_CONFIG_PATTERN, "default.hibernate"); 
        
       return newProperties;
    }
     
     public static Properties convertPattern(Properties p, String regex, String newFormat) {
         Pattern pattern = new jregex.Pattern(regex);
         Set<String> propertiesNames = p.stringPropertyNames();
         for (String property : propertiesNames) {
             Matcher m = pattern.matcher(property);
             if (m.matches()) {
                 //String[] name = property.split("\\.");
                 p.put(newFormat + "." + m.group(1), p.get(property));
                 //newProperties.remove(property);
             }
              // Special case db=...
             if ("db".equals(property)) {
                 p.put(newFormat, p.get(property));
                // newProperties.remove(property);
             }
         }
                  
        return p;
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
            if (key.toString().startsWith("db." + name) || key.toString().startsWith(name + ".hibernate") ) {
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
                String newKey = key.toString().substring(dbname.length() + 1);
                properties.put(newKey, props.get(key).toString());
            }
        } 
        return properties;
    }

}
