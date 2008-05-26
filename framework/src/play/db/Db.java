package play.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.util.Properties;
import javax.sql.DataSource;
import play.Logger;
import play.Play;

public class Db {
    public static DataSource datasource = null;
    
    public static void init () {
        if (changed()) {
            try {
                System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
                System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "OFF");
                Properties p = Play.configuration;
                ComboPooledDataSource ds = new ComboPooledDataSource ();
                ds.setDriverClass(p.getProperty("db.driver"));
                ds.setJdbcUrl(p.getProperty("db.url")); 
                ds.setUser(p.getProperty("db.user")); 
                ds.setPassword(p.getProperty("db.pass"));
                ds.setAcquireRetryAttempts(1);
                ds.setAcquireRetryDelay(0);
                datasource=ds;
            } catch (Exception e) {
                Logger.debug(e);
            }
        }
    }

    private static boolean changed () {
       Properties p = Play.configuration;
       
       if ( (p.getProperty("db.driver")==null) 
               || (p.getProperty("db.url")==null)
               || (p.getProperty("db.user")==null)
               || (p.getProperty("db.pass")==null) )
           return false;
       
       if (datasource==null)
            return true;
       else {
           ComboPooledDataSource ds = (ComboPooledDataSource) datasource;
           if (!p.getProperty("db.driver").equals(ds.getDriverClass()))
               return true;
           if (!p.getProperty("db.url").equals(ds.getJdbcUrl()))
               return true;
           if (!p.getProperty("db.user").equals(ds.getUser()))
               return true;
           if (!p.getProperty("db.pass").equals(ds.getPassword()))
               return true;
       }
       return false;
    }  
}
