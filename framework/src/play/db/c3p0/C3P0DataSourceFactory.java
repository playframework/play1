package play.db.c3p0;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import play.Play;
import play.db.Configuration;
import play.db.DB;
import play.db.DataSourceFactory;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Set;

public class C3P0DataSourceFactory implements DataSourceFactory {
  @Override
  public DataSource createDataSource(Configuration dbConfig) throws PropertyVetoException, SQLException {
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
    ds.setContextClassLoaderSource("library");
    ds.setPrivilegeSpawnedThreads(true);

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
      ds.setConnectionCustomizerClassName(PlayConnectionCustomizer.class.getName());
    }
    return ds;
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
      try {
        out.println("Busy connection numbers: " + datasource.getNumBusyConnections());
        out.println("Idle connection numbers: " + datasource.getNumIdleConnections());
        out.println("Connection numbers: " + datasource.getNumConnections());
      } catch (SQLException e) {
        out.println("Connection status error: " + e.getMessage());
      }
      out.println("Initial pool size: " + datasource.getInitialPoolSize());
      out.println("Checkout timeout: " + datasource.getCheckoutTimeout());
      out.println("Test query : " + datasource.getPreferredTestQuery());
      out.println("\r\n");
    }
    return sw.toString();
  }

  @Override
  public String getDriverClass(DataSource ds) {
    return ((ComboPooledDataSource) ds).getDriverClass();
  }

  @Override
  public String getJdbcUrl(DataSource ds) {
    return ((ComboPooledDataSource) ds).getJdbcUrl();
  }

  @Override
  public String getUser(DataSource ds) {
    return ((ComboPooledDataSource) ds).getUser();
  }
}
