package play.db;

import java.util.*;

import javax.sql.DataSource;

import play.*;
import jregex.Matcher;
import jregex.Pattern;

public class Configuration {
        
    public String configName;
      
    
    public boolean isDefault(){
        return DB.DEFAULT.equals(this.configName);
    }
        
    public Configuration(String configurationName){
        this.configName = configurationName;
    }
    

     public static Set<String> getDbNames() {
         TreeSet<String> dbNames = new TreeSet<String>();
         // search for case db= or db.url= as at least one of these property is required
         final String DB_CONFIG_PATTERN = "^db\\.([^\\.]*)$|^db\\.([^\\.]*)\\.url$";
         Pattern pattern = new jregex.Pattern(DB_CONFIG_PATTERN);
         
         // List of properties with 2 words
         List<String> dbProperties =  Arrays.asList("db.driver", "db.url", "db.user", "db.pass", "db.isolation", "db.destroyMethod", "db.testquery");

         for (String property : Play.configuration.stringPropertyNames()) {
             Matcher m = pattern.matcher(property);
             if (m.matches() && !dbProperties.contains(property)) {
                 String dbName = (m.group(1) != null ? m.group(1) : m.group(2));
                 dbNames.add(dbName);
             }
             // Special case db=... and 
             if ("db".equals(property) || "db.url".equals(property)) {
                 dbNames.add("default");
             }  
         }
         return new TreeSet<String>(dbNames);
     }
     
     public String getProperty(String key) {
         return this.getProperty(key, null);
     }
     
     
     public String getProperty(String key, String defaultString) {
         if(key != null){
             String newKey = key;
             Pattern pattern = new jregex.Pattern("^(db|jpa|hibernate){1}(\\.)*(.)*$");
             Matcher m = pattern.matcher(key);
             if(m.matches() && m.groupCount() > 1){
                 newKey = m.group(1) + "." + this.configName + key.substring(m.group(1).length());
             }
             Object value = Play.configuration.get(newKey);
             if(value == null && this.isDefault()){
                 value = Play.configuration.get(key);
             }
             if(value != null){
                 return value.toString();
             }
         }
         
         return defaultString;    
     }
     
     /**
      * 
      * @param key
      * @param value
      * @return the previous value of the specified key in this hashtable, or null if it did not have one
      */
     public Object put(String key, String value) {
         if(key != null){
             String newKey = key;
             Pattern pattern = new jregex.Pattern("^([db|jpa|hibernate]{1})(\\.[\\da-zA-Z.-_]*)$");
             Matcher m = pattern.matcher(key);
             if(m.matches()){
                 newKey = m.group(0) + "." + this.configName;
             }
             
             return Play.configuration.put(newKey, value);
         }
         return null;
     }
     

    public Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<String, String>();
        Properties props = Play.configuration;
        
        for (Object key : Collections.list(props.keys())) {
            String keyName = key.toString();
            if (keyName.startsWith("db") || keyName.startsWith("hibernate") ) {
                if (keyName.startsWith("db." + this.configName) || keyName.startsWith("hibernate." + this.configName) ) {
                    String type = keyName.substring(0, keyName.indexOf('.'));
                    String newKey = type;
                    if(keyName.length() > (type + "." + this.configName).length()){
                        newKey  += "." + keyName.substring((type + "." + this.configName).length() + 1);
                    }
                    properties.put(newKey, props.get(key).toString());
                }else if(this.isDefault()){
                    boolean isDefaultProperty = true; 
                    Set<String> dBNames = Configuration.getDbNames();
                    for(String dbName : dBNames){
                        if(key.toString().startsWith("db." + dbName) || key.toString().startsWith("hibernate." + dbName)){
                            isDefaultProperty = false;
                            break;
                        }
                    }
                    if(isDefaultProperty){
                        properties.put(key.toString(), props.get(key).toString());
                    }
                }
            }
        } 
        return properties;
    }
}
