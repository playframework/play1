package play.db;

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
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;

import jregex.Matcher;
import org.apache.log4j.Level;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.exceptions.DatabaseException;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.ConnectionCustomizer;

import play.db.DB.ExtendedDatasource;

public class DBPlugin extends PlayPlugin {

    public static String url = "";
    org.h2.tools.Server h2Server;

    @Override
    public boolean rawInvocation(Request request, Response response) throws Exception {
        if (Play.mode.isDev() && request.path.equals("/@db")) {
            response.status = Http.StatusCode.FOUND;
            String serverOptions[] = new String[] { };

            // For H2 embedded database, we'll also start the Web console
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
            String dbName = "";
            try {
                // Destroy all connections
                if (!DB.datasources.isEmpty()) {
                    DB.destroyAll();
                }
                
                // Define common parameter here
                if (play.Logger.usesJuli()) {
                    System.setProperty("com.mchange.v2.log.MLog", "jul");
                } else {
                    System.setProperty("com.mchange.v2.log.MLog", "log4j");
                }
                
                Set<String> dbNames = Configuration.getDbNames();
                Iterator<String> it = dbNames.iterator();
                while(it.hasNext()) {
                    dbName = it.next();
                    Configuration dbConfig = new Configuration(dbName);
                    
                    boolean isJndiDatasource = false;
                    String datasourceName = dbConfig.getProperty("db", "");

                    // Identify datasource JNDI lookup name by 'jndi:' or 'java:' prefix 
                    if (datasourceName.startsWith("jndi:")) {
                        datasourceName = datasourceName.substring("jndi:".length());
                        isJndiDatasource = true;
                    }

                    if (isJndiDatasource || datasourceName.startsWith("java:")) {
                        Context ctx = new InitialContext();
                        DataSource ds =  (DataSource) ctx.lookup(datasourceName);
                        DB.datasource = ds;
                        DB.destroyMethod = "";
                        DB.ExtendedDatasource extDs = new DB.ExtendedDatasource(ds, "");
                        DB.datasources.put(dbName, extDs);  
                    } else {

                        // Try the driver
                        String driver = dbConfig.getProperty("db.driver");
                        try {
                            Driver d = (Driver) Class.forName(driver, true, Play.classloader).newInstance();
                            DriverManager.registerDriver(new ProxyDriver(d));
                        } catch (Exception e) {
                            throw new Exception("Database [" + dbName + "] Driver not found (" + driver + ")", e);
                        }

                        // Try the connection
                        Connection fake = null;
                        try {
                            if (dbConfig.getProperty("db.user") == null) {
                                fake = DriverManager.getConnection(dbConfig.getProperty("db.url"));
                            } else {
                                fake = DriverManager.getConnection(dbConfig.getProperty("db.url"), dbConfig.getProperty("db.user"), dbConfig.getProperty("db.pass"));
                            }
                        } finally {
                            if (fake != null) {
                                fake.close();
                            }
                        }

                        ComboPooledDataSource ds = new ComboPooledDataSource();
                        ds.setDriverClass(dbConfig.getProperty("db.driver"));
                        ds.setJdbcUrl(dbConfig.getProperty("db.url"));
                        ds.setUser(dbConfig.getProperty("db.user"));
                        ds.setPassword(dbConfig.getProperty("db.pass"));
                        ds.setAcquireIncrement(Integer.parseInt(dbConfig.getProperty("db.pool.acquireIncrement", "3")));
                        ds.setAcquireRetryAttempts(Integer.parseInt(dbConfig.getProperty("db.pool.acquireRetryAttempts", "10")));
                        ds.setAcquireRetryDelay(Integer.parseInt(dbConfig.getProperty("db.pool.acquireRetryDelay", "1000")));
                        ds.setCheckoutTimeout(Integer.parseInt(dbConfig.getProperty("db.pool.timeout", "5000")));
                        ds.setBreakAfterAcquireFailure(Boolean.parseBoolean(dbConfig.getProperty("db.pool.breakAfterAcquireFailure", "false")));
                        ds.setMaxPoolSize(Integer.parseInt(dbConfig.getProperty("db.pool.maxSize", "30")));
                        ds.setMinPoolSize(Integer.parseInt(dbConfig.getProperty("db.pool.minSize", "1")));
                        ds.setInitialPoolSize(Integer.parseInt(dbConfig.getProperty("db.pool.initialSize", "1")));
                        ds.setMaxIdleTimeExcessConnections(Integer.parseInt(dbConfig.getProperty("db.pool.maxIdleTimeExcessConnections", "0")));
                        ds.setIdleConnectionTestPeriod(Integer.parseInt(dbConfig.getProperty("db.pool.idleConnectionTestPeriod", "10")));
                        ds.setMaxIdleTime(Integer.parseInt(dbConfig.getProperty("db.pool.maxIdleTime", "0")));
                        ds.setTestConnectionOnCheckin(Boolean.parseBoolean(dbConfig.getProperty("db.pool.testConnectionOnCheckin", "true")));
                        ds.setTestConnectionOnCheckout(Boolean.parseBoolean(dbConfig.getProperty("db.pool.testConnectionOnCheckout", "false")));
                        ds.setLoginTimeout(Integer.parseInt(dbConfig.getProperty("db.pool.loginTimeout", "0")));
                        ds.setMaxAdministrativeTaskTime(Integer.parseInt(dbConfig.getProperty("db.pool.maxAdministrativeTaskTime", "0")));
                        ds.setMaxConnectionAge(Integer.parseInt(dbConfig.getProperty("db.pool.maxConnectionAge", "0")));
                        ds.setMaxStatements(Integer.parseInt(dbConfig.getProperty("db.pool.maxStatements", "0")));
                        ds.setMaxStatementsPerConnection(Integer.parseInt(dbConfig.getProperty("db.pool.maxStatementsPerConnection", "0")));
                        ds.setNumHelperThreads(Integer.parseInt(dbConfig.getProperty("db.pool.numHelperThreads", "3")));
                        ds.setUnreturnedConnectionTimeout(Integer.parseInt(dbConfig.getProperty("db.pool.unreturnedConnectionTimeout", "0")));
                        ds.setDebugUnreturnedConnectionStackTraces(Boolean.parseBoolean(dbConfig.getProperty("db.pool.debugUnreturnedConnectionStackTraces", "false")));

                        if (dbConfig.getProperty("db.testquery") != null) {
                            ds.setPreferredTestQuery(dbConfig.getProperty("db.testquery"));
                        } else {
                            String driverClass = dbConfig.getProperty("db.driver");
                            /*
                             * Pulled from http://dev.mysql.com/doc/refman/5.5/en/connector-j-usagenotes-j2ee-concepts-connection-pooling.html
                             * Yes, the select 1 also needs to be in there.
                             */
                            if (driverClass.equals("com.mysql.jdbc.Driver")) {
                                ds.setPreferredTestQuery("/* ping */ SELECT 1");
                            }
                        }

                        // This check is not required, but here to make it clear that nothing changes for people
                        // that don't set this configuration property. It may be safely removed.
                        if(dbConfig.getProperty("db.isolation") != null) {
                            ds.setConnectionCustomizerClassName(play.db.DBPlugin.PlayConnectionCustomizer.class.getName());
                        }
                       
                        // Current datasource. This is actually deprecated. 
                        String destroyMethod = dbConfig.getProperty("db.destroyMethod", "");
                        DB.datasource = ds;
                        DB.destroyMethod = destroyMethod;

                        DB.ExtendedDatasource extDs = new DB.ExtendedDatasource(ds, destroyMethod);

                        url = ds.getJdbcUrl();
                        Connection c = null;
                        try {
                            c = ds.getConnection();
                        } finally {
                            if (c != null) {
                                c.close();
                            }
                        }
                        Logger.info("Connected to %s for %s", ds.getJdbcUrl(), dbName);
                        DB.datasources.put(dbName, extDs);
                    }
                }
                
            } catch (Exception e) {
                DB.datasource = null;
                Logger.error(e, "Database [%s] Cannot connected to the database : %s", dbName, e.getMessage());
                if (e.getCause() instanceof InterruptedException) {
                    throw new DatabaseException("Cannot connected to the database["+ dbName + "]. Check the configuration.", e);
                }
                throw new DatabaseException("Cannot connected to the database["+ dbName + "], " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onApplicationStop() {
        if (Play.mode.isProd()) {
            DB.destroyAll();
        }
    }

    @Override
    public String getStatus() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);     
        Set<String> dbNames = Configuration.getDbNames();
               
        for (String dbName : dbNames) {
            DataSource ds = DB.getDataSource(dbName);
            if (ds == null || !(ds instanceof ComboPooledDataSource)) {
                out.println("Datasource:");
                out.println("~~~~~~~~~~~");
                out.println("(not yet connected)");
                return sw.toString();
            }
            ComboPooledDataSource datasource = (ComboPooledDataSource) ds;
            out.println("Datasource (" + dbName + "):");
            out.println("~~~~~~~~~~~");
            out.println("Jdbc url: " + datasource.getJdbcUrl());
            out.println("Jdbc driver: " + datasource.getDriverClass());
            out.println("Jdbc user: " + datasource.getUser());
    	    if (Play.mode.isDev()) {
              out.println("Jdbc password: " + datasource.getPassword());
            }
            out.println("Min pool size: " + datasource.getMinPoolSize());
            out.println("Max pool size: " + datasource.getMaxPoolSize());
            out.println("Initial pool size: " + datasource.getInitialPoolSize());
            out.println("Checkout timeout: " + datasource.getCheckoutTimeout());
            out.println("Test query : " + datasource.getPreferredTestQuery());
            out.println("\r\n");
        }
        return sw.toString();
    }

    @Override
    public void invocationFinally() {
        DB.closeAll();
    }

    private static void check(Configuration config, String mode, String property) {
        if (!StringUtils.isEmpty(config.getProperty(property))) {
            Logger.warn("Ignoring " + property + " because running the in " + mode + " db.");
        }
    }

    private static boolean changed() {
        Set<String> dbNames = Configuration.getDbNames();
        
        for (String dbName : dbNames) {
            Configuration dbConfig = new Configuration(dbName);
            
            if ("mem".equals(dbConfig.getProperty("db")) && dbConfig.getProperty("db.url") == null) {
                dbConfig.put("db.driver", "org.h2.Driver");
                dbConfig.put("db.url", "jdbc:h2:mem:play;MODE=MYSQL");
                dbConfig.put("db.user", "sa");
                dbConfig.put("db.pass", "");
            }

            if ("fs".equals(dbConfig.getProperty("db")) && dbConfig.getProperty("db.url") == null) {
                dbConfig.put("db.driver", "org.h2.Driver");
                dbConfig.put("db.url", "jdbc:h2:" + (new File(Play.applicationPath, "db/h2/play").getAbsolutePath()) + ";MODE=MYSQL");
                dbConfig.put("db.user", "sa");
                dbConfig.put("db.pass", "");
            }
            String datasourceName = dbConfig.getProperty("db", "");
            DataSource ds = DB.getDataSource(dbName);
                     
            if ((datasourceName.startsWith("java:")) && dbConfig.getProperty("db.url") == null) {
                if (ds == null) {
                    return true;
                }
            } else {
                // Internal pool is c3p0, we should call the close() method to destroy it.
                check(dbConfig, "internal pool", "db.destroyMethod");

                dbConfig.put("db.destroyMethod", "close");
            }

            Matcher m = new jregex.Pattern("^mysql:(//)?(({user}[a-zA-Z0-9_]+)(:({pwd}[^@]+))?@)?(({host}[^/]+)/)?({name}[a-zA-Z0-9_]+)(\\?)?({parameters}[^\\s]+)?$").matcher(dbConfig.getProperty("db", ""));
            if (m.matches()) {
                String user = m.group("user");
                String password = m.group("pwd");
                String name = m.group("name");
                String host = m.group("host");
                String parameters = m.group("parameters");
        		
                Map<String, String> paramMap = new HashMap<>();
                paramMap.put("useUnicode", "yes");
                paramMap.put("characterEncoding", "UTF-8");
                paramMap.put("connectionCollation", "utf8_general_ci");
                addParameters(paramMap, parameters);
                
                dbConfig.put("db.driver", "com.mysql.jdbc.Driver");
                dbConfig.put("db.url", "jdbc:mysql://" + (host == null ? "localhost" : host) + "/" + name + "?" + toQueryString(paramMap));
                if (user != null) {
                    dbConfig.put("db.user", user);
                }
                if (password != null) {
                    dbConfig.put("db.pass", password);
                }
            }
            
            m = new jregex.Pattern("^postgres:(//)?(({user}[a-zA-Z0-9_]+)(:({pwd}[^@]+))?@)?(({host}[^/]+)/)?({name}[^\\s]+)$").matcher(dbConfig.getProperty("db", ""));
            if (m.matches()) {
                String user = m.group("user");
                String password = m.group("pwd");
                String name = m.group("name");
                String host = m.group("host");
                dbConfig.put("db.driver", "org.postgresql.Driver");
                dbConfig.put("db.url", "jdbc:postgresql://" + (host == null ? "localhost" : host) + "/" + name);
                if (user != null) {
                    dbConfig.put("db.user", user);
                }
                if (password != null) {
                    dbConfig.put("db.pass", password);
                }
            }

            if(dbConfig.getProperty("db.url") != null && dbConfig.getProperty("db.url").startsWith("jdbc:h2:mem:")) {
                dbConfig.put("db.driver", "org.h2.Driver");
                dbConfig.put("db.user", "sa");
                dbConfig.put("db.pass", "");
            }

            if ((dbConfig.getProperty("db.driver") == null) || (dbConfig.getProperty("db.url") == null)) {
                return false;
            }
            
            if (ds == null) {
                return true;
            } else {
                ComboPooledDataSource cds = (ComboPooledDataSource) ds;
                if (!dbConfig.getProperty("db.driver").equals(cds.getDriverClass())) {
                    return true;
                }
                if (!dbConfig.getProperty("db.url").equals(cds.getJdbcUrl())) {
                    return true;
                }
                if (!dbConfig.getProperty("db.user", "").equals(cds.getUser())) {
                    return true;
                }
                if (!dbConfig.getProperty("db.pass", "").equals(cds.getPassword())) {
                    return true;
                }
            }

            ExtendedDatasource extDataSource = DB.datasources.get(dbName);

            if (extDataSource != null && !dbConfig.getProperty("db.destroyMethod", "").equals(extDataSource.getDestroyMethod())) {
                return true;
            }
        }
        return false;
    }
    
    private static void addParameters(Map<String, String> paramsMap, String urlQuery) {
    	if (!StringUtils.isBlank(urlQuery)) {
	    	String[] params = urlQuery.split("[\\&]");
	    	for (String param : params) {
				String[] parts = param.split("[=]");
				if (parts.length > 0 && !StringUtils.isBlank(parts[0])) {
				    paramsMap.put(parts[0], parts.length > 1 ? StringUtils.stripToNull(parts[1]) : null);
				}
			}
    	}
    }
    
    private static String toQueryString(Map<String, String> paramMap) {
    	StringBuilder builder = new StringBuilder();
    	for (Map.Entry<String, String> entry : paramMap.entrySet()) {
    		if (builder.length() > 0) builder.append("&");
			builder.append(entry.getKey()).append("=").append(entry.getValue() != null ? entry.getValue() : "");
		}
    	return builder.toString();
    }

    /**
     * Needed because DriverManager will not load a driver ouside of the system classloader
     */
    public static class ProxyDriver implements Driver {

        private Driver driver;

        ProxyDriver(Driver d) {
            this.driver = d;
        }

        @Override
        public boolean acceptsURL(String u) throws SQLException {
            return this.driver.acceptsURL(u);
        }

        @Override
        public Connection connect(String u, Properties p) throws SQLException {
            return this.driver.connect(u, p);
        }

        @Override
        public int getMajorVersion() {
            return this.driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return this.driver.getMinorVersion();
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
            return this.driver.getPropertyInfo(u, p);
        }

        @Override
        public boolean jdbcCompliant() {
            return this.driver.jdbcCompliant();
        }
      
        // Method not annotated with @Override since getParentLogger() is a new method
        // in the CommonDataSource interface starting with JDK7 and this annotation
        // would cause compilation errors with JDK6.
        @Override
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
            isolationLevels = new HashMap<>();
            isolationLevels.put("NONE", Connection.TRANSACTION_NONE);
            isolationLevels.put("READ_UNCOMMITTED", Connection.TRANSACTION_READ_UNCOMMITTED);
            isolationLevels.put("READ_COMMITTED", Connection.TRANSACTION_READ_COMMITTED);
            isolationLevels.put("REPEATABLE_READ", Connection.TRANSACTION_REPEATABLE_READ);
            isolationLevels.put("SERIALIZABLE", Connection.TRANSACTION_SERIALIZABLE);
        }

        @Override
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

        @Override
        public void onDestroy(Connection c, String parentDataSourceIdentityToken) {}

        @Override
        public void onCheckOut(Connection c, String parentDataSourceIdentityToken) {}

        @Override
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
