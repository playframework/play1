package play.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.io.File;
import java.util.Properties;
import play.Logger;
import play.Play;
import play.PlayPlugin;

public class DBPlugin extends PlayPlugin {

    @Override
    public void onApplicationStart() {
        if (changed()) {
            try {
                System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
                System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "OFF");
                Properties p = Play.configuration;
                ComboPooledDataSource ds = new ComboPooledDataSource();
                ds.setDriverClass(p.getProperty("db.driver"));
                ds.setJdbcUrl(p.getProperty("db.url"));
                ds.setUser(p.getProperty("db.user"));
                ds.setPassword(p.getProperty("db.pass"));
                ds.setAcquireRetryAttempts(1);
                ds.setAcquireRetryDelay(0);
                ds.setCheckoutTimeout(1000);
                ds.setBreakAfterAcquireFailure(true);
                ds.setMaxPoolSize(30);
                ds.setMinPoolSize(10);
                DB.datasource = ds;
                Logger.info("Connected to %s", ds.getJdbcUrl());
            } catch (Exception e) {
                Logger.error(e, "Cannot connected to the database");
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

        if ((p.getProperty("db.driver") == null) || (p.getProperty("db.url") == null) || (p.getProperty("db.user") == null) || (p.getProperty("db.pass") == null)) {
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
            if (!p.getProperty("db.user").equals(ds.getUser())) {
                return true;
            }
            if (!p.getProperty("db.pass").equals(ds.getPassword())) {
                return true;
            }
        }
        return false;
    }
}
