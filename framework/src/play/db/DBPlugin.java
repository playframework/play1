package play.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.ConnectionCustomizer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import jregex.Matcher;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.exceptions.DatabaseException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * The DB plugin
 */
public class DBPlugin extends PlayPlugin {

    public static String url = "";
    org.h2.tools.Server h2Server;

    @Override
    public boolean rawInvocation(Request request, Response response) throws Exception {
        if (Play.mode.isDev() && request.path.equals("/@db")) {
            response.status = Http.StatusCode.MOVED;
            String serverOptions[] = new String[] { };

            // For H2 embeded database, we'll also start the Web console
            if (h2Server != null) {
                h2Server.stop();
            }

            String domain = request.domain;
            if (domain.equals("")) {
                domain = "localhost";
            }

            if (!domain.equals("localhost")) {
                serverOptions = new String[] {"-webAllowOthers"};
            }
            
            h2Server = org.h2.tools.Server.createWebServer(serverOptions);
            h2Server.start();

            response.setHeader("Location", "http://" + domain + ":8082/");
            return true;
        }
        return false;
    }

    @Override
    public void onApplicationStart() {
        if (changed()) {
            try {

                Properties p = Play.configuration;

                if (DB.datasource != null) {
                    DB.destroy();
                }

	        boolean isJndiDatasource = false;
                String datasourceName = p.getProperty("db", "");
                // Identify datasource JNDI lookup name by 'jndi:' or 'java:' prefix 
                if (datasourceName.startsWith("jndi:")) {
                    datasourceName = datasourceName.substring("jndi:".length());
                    isJndiDatasource = true;
                }

                if (isJndiDatasource || datasourceName.startsWith("java:")) {

                    Context ctx = new InitialContext();
                    DB.datasource = (DataSource) ctx.lookup(datasourceName);

                } else {

                    // Try the driver
                    String driver = p.getProperty("db.driver");
                    try {
                        Driver d = (Driver) Class.forName(driver, true, Play.classloader).newInstance();
                        DriverManager.registerDriver(new ProxyDriver(d));
                    } catch (Exception e) {
                        throw new Exception("Driver not found (" + driver + ")");
                    }

                    // Try the connection
                    Connection fake = null;
                    try {
                        if (p.getProperty("db.user") == null) {
                            fake = DriverManager.getConnection(p.getProperty("db.url"));
                        } else {
                            fake = DriverManager.getConnection(p.getProperty("db.url"), p.getProperty("db.user"), p.getProperty("db.pass"));
                        }
                    } finally {
                        if (fake != null) {
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
                    ds.setAcquireRetryAttempts(10);
                    ds.setCheckoutTimeout(Integer.parseInt(p.getProperty("db.pool.timeout", "5000")));
                    ds.setBreakAfterAcquireFailure(false);
                    ds.setMaxPoolSize(Integer.parseInt(p.getProperty("db.pool.maxSize", "30")));
                    ds.setMinPoolSize(Integer.parseInt(p.getProperty("db.pool.minSize", "1")));
                    ds.setMaxIdleTimeExcessConnections(Integer.parseInt(p.getProperty("db.pool.maxIdleTimeExcessConnections", "0")));
                    ds.setIdleConnectionTestPeriod(10);
                    ds.setTestConnectionOnCheckin(true);
                    
                    // This check is not required, but here to make it clear that nothing changes for people
                    // that don't set this configuration property. It may be safely removed.
                    if(p.getProperty("db.isolation") != null) {
                        ds.setConnectionCustomizerClassName(play.db.DBPlugin.PlayConnectionCustomizer.class.getName());
                    }
                    
                    DB.datasource = ds;
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

                DB.destroyMethod = p.getProperty("db.destroyMethod", "");

            } catch (Exception e) {
                DB.datasource = null;
                Logger.error(e, "Cannot connected to the database : %s", e.getMessage());
                if (e.getCause() instanceof InterruptedException) {
                    throw new DatabaseException("Cannot connected to the database. Check the configuration.", e);
                }
                throw new DatabaseException("Cannot connected to the database, " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onApplicationStop() {
        if (Play.mode.isProd()) {
            DB.destroy();
        }
    }

    @Override
    public String getStatus() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        if (DB.datasource == null || !(DB.datasource instanceof ComboPooledDataSource)) {
            out.println("Datasource:");
            out.println("~~~~~~~~~~~");
            out.println("(not yet connected)");
            return sw.toString();
        }
        ComboPooledDataSource datasource = (ComboPooledDataSource) DB.datasource;
        out.println("Datasource:");
        out.println("~~~~~~~~~~~");
        out.println("Jdbc url: " + datasource.getJdbcUrl());
        out.println("Jdbc driver: " + datasource.getDriverClass());
        out.println("Jdbc user: " + datasource.getUser());
        out.println("Jdbc password: " + datasource.getPassword());
        out.println("Min pool size: " + datasource.getMinPoolSize());
        out.println("Max pool size: " + datasource.getMaxPoolSize());
        out.println("Initial pool size: " + datasource.getInitialPoolSize());
        out.println("Checkout timeout: " + datasource.getCheckoutTimeout());
        return sw.toString();
    }

    @Override
    public void invocationFinally() {
        DB.close();
    }

    private static void check(Properties p, String mode, String property) {
        if (!StringUtils.isEmpty(p.getProperty(property))) {
            Logger.warn("Ignoring " + property + " because running the in " + mode + " db.");
        }
    }

    private static boolean changed() {
        Properties p = Play.configuration;

        if ("mem".equals(p.getProperty("db")) && p.getProperty("db.url") == null) {
            p.put("db.driver", "org.h2.Driver");
            p.put("db.url", "jdbc:h2:mem:play;MODE=MYSQL");
            p.put("db.user", "sa");
            p.put("db.pass", "");
        }

        if ("fs".equals(p.getProperty("db")) && p.getProperty("db.url") == null) {
            p.put("db.driver", "org.h2.Driver");
            p.put("db.url", "jdbc:h2:" + (new File(Play.applicationPath, "db/h2/play").getAbsolutePath()) + ";MODE=MYSQL");
            p.put("db.user", "sa");
            p.put("db.pass", "");
        }
        boolean isJndiDatasource = false;
        String datasourceName = p.getProperty("db", "");
             
        if ((isJndiDatasource || datasourceName.startsWith("java:")) && p.getProperty("db.url") == null) {
            if (DB.datasource == null) {
                return true;
            }
        } else {
            // Internal pool is c3p0, we should call the close() method to destroy it.
            check(p, "internal pool", "db.destroyMethod");

            p.put("db.destroyMethod", "close");
        }

        Matcher m = new jregex.Pattern("^mysql:(//)?(({user}[a-zA-Z0-9_]+)(:({pwd}[^@]+))?@)?(({host}[^/]+)/)?({name}[^\\s]+)$").matcher(p.getProperty("db", ""));
        if (m.matches()) {
            String user = m.group("user");
            String password = m.group("pwd");
            String name = m.group("name");
            String host = m.group("host");
            p.put("db.driver", "com.mysql.jdbc.Driver");
            p.put("db.url", "jdbc:mysql://" + (host == null ? "localhost" : host) + "/" + name + "?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci");
            if (user != null) {
                p.put("db.user", user);
            }
            if (password != null) {
                p.put("db.pass", password);
            }
        }
        
        m = new jregex.Pattern("^postgres:(//)?(({user}[a-zA-Z0-9_]+)(:({pwd}[^@]+))?@)?(({host}[^/]+)/)?({name}[^\\s]+)$").matcher(p.getProperty("db", ""));
        if (m.matches()) {
            String user = m.group("user");
            String password = m.group("pwd");
            String name = m.group("name");
            String host = m.group("host");
            p.put("db.driver", "org.postgresql.Driver");
            p.put("db.url", "jdbc:postgresql://" + (host == null ? "localhost" : host) + "/" + name);
            if (user != null) {
                p.put("db.user", user);
            }
            if (password != null) {
                p.put("db.pass", password);
            }
        }

        if(p.getProperty("db.url") != null && p.getProperty("db.url").startsWith("jdbc:h2:mem:")) {
            p.put("db.driver", "org.h2.Driver");
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

        if (!p.getProperty("db.destroyMethod", "").equals(DB.destroyMethod)) {
            return true;
        }

        return false;
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
        
        // Method not annotated with @Override since getParentLogger() is a new method
        // in the CommonDataSource interface starting with JDK7 and this annotation
        // would cause compilation errors with JDK6.
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            try {
                return (java.util.logging.Logger) Driver.class.getDeclaredMethod("getParentLogger").invoke(this.driver);
            } catch (Throwable e) {
                return null;
            }
        }
    }

    public static class PlayConnectionCustomizer implements ConnectionCustomizer {

        public static Map<String, Integer> isolationLevels;

        static {
            isolationLevels = new HashMap<String, Integer>();
            isolationLevels.put("NONE", Connection.TRANSACTION_NONE);
            isolationLevels.put("READ_UNCOMMITTED", Connection.TRANSACTION_READ_UNCOMMITTED);
            isolationLevels.put("READ_COMMITTED", Connection.TRANSACTION_READ_COMMITTED);
            isolationLevels.put("REPEATABLE_READ", Connection.TRANSACTION_REPEATABLE_READ);
            isolationLevels.put("SERIALIZABLE", Connection.TRANSACTION_SERIALIZABLE);
        }

        public void onAcquire(Connection c, String parentDataSourceIdentityToken) {
            Integer isolation = getIsolationLevel();
            if (isolation != null) {
                try {
                    Logger.trace("Setting connection isolation level to %s", isolation);
                    c.setTransactionIsolation(isolation);
                } catch (SQLException e) {
                    throw new DatabaseException("Failed to set isolation level to " + isolation, e);
                }
            }
        }

        public void onDestroy(Connection c, String parentDataSourceIdentityToken) {}
        public void onCheckOut(Connection c, String parentDataSourceIdentityToken) {}
        public void onCheckIn(Connection c, String parentDataSourceIdentityToken) {}

        /**
         * Get the isolation level from either the isolationLevels map, or by
         * parsing into an int.
         */
        private Integer getIsolationLevel() {
            String isolation = Play.configuration.getProperty("db.isolation");
            if (isolation == null) {
                return null;
            }
            Integer level = isolationLevels.get(isolation);
            if (level != null) {
                return level;
            }

            try {
                return Integer.valueOf(isolation);
            } catch (NumberFormatException e) {
                throw new DatabaseException("Invalid isolation level configuration" + isolation, e);
            }
        }
    }
}
