package play.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import jregex.Matcher;
import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.db.jpa.JPAConfig;
import play.db.jpa.JPAContext;
import play.exceptions.DatabaseException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class DBConfig {

    /**
     * This is the name of the default db- and jpa-config name
     */
    public static final String defaultDbConfigName = "play";

    /**
     * Name of the db-config - should match jpaConfig-name if present
     */
    private final String dbConfigName;

    private String url = "";

    /**
     * The loaded datasource.
     */
    private DataSource datasource = null;

    /**
     * The method used to destroy the datasource
     */
    private String destroyMethod = "";

    private ThreadLocal<Connection> localConnection = new ThreadLocal<Connection>();


    protected DBConfig(String dbConfigName) {
        this.dbConfigName = dbConfigName;
    }

    public String getDBConfigName() {
        return dbConfigName;
    }

    /**
     * Close the connection opened for the current thread.
     */
    public void close() {
        if (localConnection.get() != null) {
            try {
                Connection connection = localConnection.get();
                localConnection.set(null);
                connection.close();
            } catch (Exception e) {
                throw new DatabaseException("It's possible that the connection was not properly closed !", e);
            }
        }
    }


    /**
     * Open a connection for the current thread.
     * @return A valid SQL connection
     */
    @SuppressWarnings("deprecation")
    public Connection getConnection() {
        try {
            // do we have a present JPAContext for this db-config in current thread?
            JPAConfig jpaConfig = JPA.getJPAConfig(dbConfigName);
            if (jpaConfig!=null) {
                JPAContext jpaContext = jpaConfig.getJPAContext();
                return ((org.hibernate.ejb.EntityManagerImpl) jpaContext.em()).getSession().connection();
            }

            // do we have a current raw connection bound to thread?
            if (localConnection.get() != null) {
                return localConnection.get();
            }

            // must create connection
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
    public boolean execute(String SQL) {
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
    public ResultSet executeQuery(String SQL) {
        try {
            return getConnection().createStatement().executeQuery(SQL);
        } catch (SQLException ex) {
            throw new DatabaseException(ex.getMessage(), ex);
        }
    }

    /**
     * Tries to destroy the datasource
     */
    public void destroy() {
        try {
            if (datasource != null && destroyMethod != null && !destroyMethod.equals("")) {
                Method close = datasource.getClass().getMethod(destroyMethod, new Class[] {});
                if (close != null) {
                    close.invoke(datasource, new Object[] {});
                    datasource = null;
                    if (Logger.isTraceEnabled()) {
                        Logger.trace("Datasource destroyed for db config " + dbConfigName);
                    }
                }
            }
        } catch (Throwable t) {
             Logger.error("Couldn't destroy the datasource for db config " + dbConfigName, t);
        }
    }

    private void check(Properties p, String mode, String property) {
        if (!StringUtils.isEmpty(p.getProperty(property))) {
            Logger.warn("Ignoring " + property + " because running the in " + mode + " db.");
        }
    }

    /**
     * Detects changes and reconfigures this dbConfig.
     * Returns true if the database was configured (Config info present.
     * An exception is throwns if the config process fails.
     */
    protected boolean configure() {

        // prefix used before all properties when loafing config. default is 'db'
        String propsPrefix;
        if (defaultDbConfigName.equals(dbConfigName)) {
            propsPrefix = "db";
        } else {
            propsPrefix = "db_"+dbConfigName;
        }

        boolean dbConfigured = false;

        if (changed(propsPrefix)) {
            try {

                // We now know that we will either config the db, or fail with exception
                dbConfigured = true;

                Properties p = Play.configuration;

                if (datasource != null) {
                    destroy();
                }

                if (p.getProperty(propsPrefix, "").startsWith("java:")) {

                    Context ctx = new InitialContext();
                    datasource = (DataSource) ctx.lookup(p.getProperty(propsPrefix));

                } else {

                    // Try the driver
                    String driver = p.getProperty(propsPrefix+".driver");
                    try {
                        Driver d = (Driver) Class.forName(driver, true, Play.classloader).newInstance();
                        DriverManager.registerDriver(new ProxyDriver(d));
                    } catch (Exception e) {
                        throw new Exception("Driver not found (" + driver + ")");
                    }

                    // Try the connection
                    Connection fake = null;
                    try {
                        if (p.getProperty(propsPrefix+".user") == null) {
                            fake = DriverManager.getConnection(p.getProperty(propsPrefix+".url"));
                        } else {
                            fake = DriverManager.getConnection(p.getProperty(propsPrefix+".url"), p.getProperty(propsPrefix+".user"), p.getProperty(propsPrefix+".pass"));
                        }
                    } finally {
                        if (fake != null) {
                            fake.close();
                        }
                    }

                    ComboPooledDataSource ds = new ComboPooledDataSource();
                    ds.setDriverClass(p.getProperty(propsPrefix+".driver"));
                    ds.setJdbcUrl(p.getProperty(propsPrefix + ".url"));
                    ds.setUser(p.getProperty(propsPrefix + ".user"));
                    ds.setPassword(p.getProperty(propsPrefix + ".pass"));
                    ds.setAcquireRetryAttempts(10);
                    ds.setCheckoutTimeout(Integer.parseInt(p.getProperty(propsPrefix + ".pool.timeout", "5000")));
                    ds.setBreakAfterAcquireFailure(false);
                    ds.setMaxPoolSize(Integer.parseInt(p.getProperty(propsPrefix + ".pool.maxSize", "30")));
                    ds.setMinPoolSize(Integer.parseInt(p.getProperty(propsPrefix + ".pool.minSize", "1")));
                    ds.setMaxIdleTimeExcessConnections(Integer.parseInt(p.getProperty(propsPrefix + ".pool.maxIdleTimeExcessConnections", "0")));
                    ds.setIdleConnectionTestPeriod(10);
                    ds.setTestConnectionOnCheckin(true);
                    datasource = ds;
                    url = ds.getJdbcUrl();
                    Connection c = null;
                    try {
                        c = ds.getConnection();
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }
                    Logger.info("Connected to %s", ds.getJdbcUrl());

                }

                destroyMethod = p.getProperty(propsPrefix+".destroyMethod", "");

            } catch (Exception e) {
                datasource = null;
                Logger.error(e, "Cannot connected to the database"+getConfigInfoString()+" : %s", e.getMessage());
                if (e.getCause() instanceof InterruptedException) {
                    throw new DatabaseException("Cannot connected to the database"+getConfigInfoString()+". Check the configuration.", e);
                }
                throw new DatabaseException("Cannot connected to the database"+getConfigInfoString()+", " + e.getMessage(), e);
            }
        }

        return dbConfigured;
    }

    /**
     * returns empty string if default config.
     * returns descriptive string about config name if not default config
     */
    protected String getConfigInfoString() {
        if (defaultDbConfigName.equals(dbConfigName)) {
            return "";
        } else {
            return " (db config name: "+dbConfigName+")";
        }
    }


    protected String getStatus() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        if (datasource == null || !(datasource instanceof ComboPooledDataSource)) {
            out.println("Datasource"+getConfigInfoString()+":");
            out.println("~~~~~~~~~~~");
            out.println("(not yet connected)");
            return sw.toString();
        }
        ComboPooledDataSource ds = (ComboPooledDataSource) datasource;
        out.println("Datasource"+getConfigInfoString()+":");
        out.println("~~~~~~~~~~~");
        out.println("Jdbc url: " + ds.getJdbcUrl());
        out.println("Jdbc driver: " + ds.getDriverClass());
        out.println("Jdbc user: " + ds.getUser());
        out.println("Jdbc password: " + ds.getPassword());
        out.println("Min pool size: " + ds.getMinPoolSize());
        out.println("Max pool size: " + ds.getMaxPoolSize());
        out.println("Initial pool size: " + ds.getInitialPoolSize());
        out.println("Checkout timeout: " + ds.getCheckoutTimeout());
        return sw.toString();
    }

    /**
     * Returns true if config has changed.
     * This method does also set additional properties resolved from
     * other settings.
     */
    private boolean changed(String propsPrefix) {
        Properties p = Play.configuration;

        if ("mem".equals(p.getProperty(propsPrefix)) && p.getProperty(propsPrefix+".url") == null) {
            p.put(propsPrefix+".driver", "org.h2.Driver");
            p.put(propsPrefix+".url", "jdbc:h2:mem:"+dbConfigName+";MODE=MYSQL");
            p.put(propsPrefix+".user", "sa");
            p.put(propsPrefix+".pass", "");
        }

        if ("fs".equals(p.getProperty(propsPrefix)) && p.getProperty(propsPrefix+".url") == null) {
            p.put(propsPrefix+".driver", "org.h2.Driver");
            p.put(propsPrefix+".url", "jdbc:h2:" + (new File(Play.applicationPath, "db/h2/"+dbConfigName).getAbsolutePath()) + ";MODE=MYSQL");
            p.put(propsPrefix+".user", "sa");
            p.put(propsPrefix+".pass", "");
        }

        if (p.getProperty(propsPrefix, "").startsWith("java:") && p.getProperty(propsPrefix+".url") == null) {
            if (datasource == null) {
                return true;
            }
        } else {
            // Internal pool is c3p0, we should call the close() method to destroy it.
            check(p, "internal pool", propsPrefix+".destroyMethod");

            p.put(propsPrefix + ".destroyMethod", "close");
        }

        Matcher m = new jregex.Pattern("^mysql:(({user}[\\w]+)(:({pwd}[^@]+))?@)?({name}[\\w]+)$").matcher(p.getProperty(propsPrefix, ""));
        if (m.matches()) {
            String user = m.group("user");
            String password = m.group("pwd");
            String name = m.group("name");
            p.put(propsPrefix+".driver", "com.mysql.jdbc.Driver");
            p.put(propsPrefix+".url", "jdbc:mysql://localhost/" + name + "?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci");
            if (user != null) {
                p.put(propsPrefix+".user", user);
            }
            if (password != null) {
                p.put(propsPrefix+".pass", password);
            }
        }

        if(p.getProperty(propsPrefix+".url") != null && p.getProperty(propsPrefix+".url").startsWith("jdbc:h2:mem:")) {
            p.put(propsPrefix+".driver", "org.h2.Driver");
            p.put(propsPrefix+".user", "sa");
            p.put(propsPrefix+".pass", "");
        }

        if ((p.getProperty(propsPrefix+".driver") == null) || (p.getProperty(propsPrefix+".url") == null)) {
            return false;
        }
        if (datasource == null) {
            return true;
        } else {
            ComboPooledDataSource ds = (ComboPooledDataSource) datasource;
            if (!p.getProperty(propsPrefix+".driver").equals(ds.getDriverClass())) {
                return true;
            }
            if (!p.getProperty(propsPrefix+".url").equals(ds.getJdbcUrl())) {
                return true;
            }
            if (!p.getProperty(propsPrefix+".user", "").equals(ds.getUser())) {
                return true;
            }
            if (!p.getProperty(propsPrefix+".pass", "").equals(ds.getPassword())) {
                return true;
            }
        }

        if (!p.getProperty(propsPrefix+".destroyMethod", "").equals(destroyMethod)) {
            return true;
        }

        return false;
    }

    public DataSource getDatasource() {
        return datasource;
    }

    public String getUrl() {
        return url;
    }

    /**
     * Needed because DriverManager will not load a driver ouside of the system classloader
     */
    public static class ProxyDriver implements Driver {

        private Driver driver;

        ProxyDriver(Driver d) {
            this.driver = d;
        }

        public boolean acceptsURL(String u) throws SQLException {
            return this.driver.acceptsURL(u);
        }

        public Connection connect(String u, Properties p) throws SQLException {
            return this.driver.connect(u, p);
        }

        public int getMajorVersion() {
            return this.driver.getMajorVersion();
        }

        public int getMinorVersion() {
            return this.driver.getMinorVersion();
        }

        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
            return this.driver.getPropertyInfo(u, p);
        }

        public boolean jdbcCompliant() {
            return this.driver.jdbcCompliant();
        }
    }


}
