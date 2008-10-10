package play.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.exceptions.DatabaseException;

public class DBPlugin extends PlayPlugin {

    @Override
    public void onApplicationStart() {
        if (changed()) {
            try {
                
                Properties p = Play.configuration;
                
                // Try the driver
                String driver = p.getProperty("db.driver");
                try {
                    Class.forName(driver);                    
                } catch (Exception e) {
                    throw new Exception("Driver not found (" + driver + ")");
                }
                
                // Try the connection
                Connection fake = null;
                try {
                    if(p.getProperty("db.user") == null) {
                        DriverManager.getConnection(p.getProperty("db.url"));
                    } else {
                        DriverManager.getConnection(p.getProperty("db.url"), p.getProperty("db.user"), p.getProperty("db.pass"));
                    }
                } finally {
                    if(fake != null) {
                        fake.close();
                    }
                }
                
                System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
                System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "OFF");
                ComboPooledDataSource ds = new ComboPooledDataSource();
                ds.setDriverClass(p.getProperty("db.driver"));
                ds.setJdbcUrl(p.getProperty("db.url"));
                ds.setUser(p.getProperty("db.user"));
                ds.setPassword(p.getProperty("db.pass"));
                ds.setAcquireRetryAttempts(1);
                ds.setAcquireRetryDelay(0);
                ds.setCheckoutTimeout(Integer.parseInt(p.getProperty("db.pool.timeout", "5000")));
                ds.setBreakAfterAcquireFailure(true);
                ds.setMaxPoolSize(Integer.parseInt(p.getProperty("db.pool.maxSize", "30")));
                ds.setMinPoolSize(Integer.parseInt(p.getProperty("db.pool.minSize", "1")));
                ds.setTestConnectionOnCheckout(true);
                DB.datasource = ds;
                Connection c = null;
                try {
                    c = ds.getConnection();
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
                Logger.info("Connected to %s", ds.getJdbcUrl());
            } catch (Exception e) {
                DB.datasource = null;
                Logger.error(e, "Cannot connected to the database : %s", e.getMessage());
                if(e.getCause() instanceof InterruptedException) {
                    throw new DatabaseException("Cannot connected to the database. Check the configuration.", e);
                }
                throw new DatabaseException("Cannot connected to the database, "+e.getMessage(), e);
            }
        }
    }

    @Override
    public void invocationFinally() {
        DB.close();
    }

    private static boolean changed() {
        Properties p = Play.configuration;

        if ("mem".equals(p.getProperty("db"))) {
            p.put("db.driver", "org.hsqldb.jdbcDriver");
            p.put("db.url", "jdbc:hsqldb:mem:playembed");
            p.put("db.user", "sa");
            p.put("db.pass", "");
        }

        if ("fs".equals(p.getProperty("db"))) {
            p.put("db.driver", "org.hsqldb.jdbcDriver");
            p.put("db.url", "jdbc:hsqldb:file:" + (new File(Play.applicationPath, "db/db").getAbsolutePath()));
            p.put("db.user", "sa");
            p.put("db.pass", "");
        }

        if ((p.getProperty("db.driver") == null) || (p.getProperty("db.url") == null)) {
            return false;
        }
        if (DB.datasource == null) {
            return true;
        } else {
            ComboPooledDataSource ds = (ComboPooledDataSource) DB.datasource;
            if (!p.getProperty("db.driver").equals(ds.getDriverClass())) {
                return true;
            }
            if (!p.getProperty("db.url").equals(ds.getJdbcUrl())) {
                return true;
            }
            if (!p.getProperty("db.user", "").equals(ds.getUser())) {
                return true;
            }
            if (!p.getProperty("db.pass", "").equals(ds.getPassword())) {
                return true;
            }
        }
        return false;
    }
}
