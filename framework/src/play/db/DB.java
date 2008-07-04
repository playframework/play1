package play.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.db.jpa.JPAContext;
import play.exceptions.DatabaseException;

/**
 * Database connection utilities.
 * @author guillaume
 */
public class DB {

    /**
     * The loaded datasource.
     */
    public static DataSource datasource = null;

    /**
     * Init the datasource.
     */
    public static void init() {
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
                datasource = ds;
                Logger.info("Connected to %s", ds.getJdbcUrl());
            } catch (Exception e) {
                Logger.error(e, "Cannot connected to the database");
            }
        }
    }

    /**
     * Close the connection opened for the current thread.
     */
    public static void close() {
        if (localConnection.get() != null) {
            try {
                Connection connection = localConnection.get();
                localConnection.set(null);
                connection.close();
            } catch (Exception e) {
                throw new DatabaseException("It's possible than the connection was not propertly closed !", e);
            }
        }
    }
    static ThreadLocal<Connection> localConnection = new ThreadLocal<Connection>();

    /**
     * Open a connection for the current thread.
     * @return A valid SQL connection
     */
    public static Connection getConnection() {
        try {
            if (JPA.isEnabled()) {
                return ((org.hibernate.ejb.EntityManagerImpl) JPAContext.getEntityManager()).getSession().connection();
            }
            if (localConnection.get() != null) {
                return localConnection.get();
            }
            Connection connection = datasource.getConnection();
            localConnection.set(connection);
            return connection;
        } catch (SQLException ex) {
            throw new DatabaseException("Cannot obtain a new connection (" + ex.getMessage() + ")", ex);
        } catch (NullPointerException e) {
            if (datasource == null) {
                throw new DatabaseException("No database found. Check the configuration of your application.", e);
            }
            throw e;
        }
    }

    /**
     * Execute an SQL update
     * @param SQL
     * @return false if update failed
     */
    public static boolean execute(String SQL) {
        try {
            return getConnection().createStatement().execute(SQL);
        } catch (SQLException ex) {
            throw new DatabaseException(ex.getMessage(), ex);
        }
    }

     /**
     * Execute an SQL query
     * @param SQL
     * @return The query resultSet
     */
    public static ResultSet executeQuery(String SQL) {
        try {
            return getConnection().createStatement().executeQuery(SQL);
        } catch (SQLException ex) {
            throw new DatabaseException(ex.getMessage(), ex);
        }
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
        if (datasource == null) {
            return true;
        } else {
            ComboPooledDataSource ds = (ComboPooledDataSource) datasource;
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
