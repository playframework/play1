package play.db.hikaricp;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

public class HikariDataSourceFactory implements DataSourceFactory {

  @Override
  public DataSource createDataSource(Configuration dbConfig) throws PropertyVetoException, SQLException {
    HikariDataSource ds = new HikariDataSource();
    ds.setDriverClassName(dbConfig.getProperty("db.driver"));
    ds.setJdbcUrl(dbConfig.getProperty("db.url"));
    ds.setUsername(dbConfig.getProperty("db.user"));
    ds.setPassword(dbConfig.getProperty("db.pass"));
    ds.setAutoCommit(false);
    ds.setConnectionTimeout(parseLong(dbConfig.getProperty("db.pool.timeout", "5000")));
    ds.setMaximumPoolSize(parseInt(dbConfig.getProperty("db.pool.maxSize", "30")));
    ds.setMinimumIdle(parseInt(dbConfig.getProperty("db.pool.minSize", "1")));
    ds.setIdleTimeout(parseLong(dbConfig.getProperty("db.pool.maxIdleTime", "0"))); // NB! Now in milliseconds
    ds.setLeakDetectionThreshold(parseLong(dbConfig.getProperty("db.pool.unreturnedConnectionTimeout", "0")));
    ds.setValidationTimeout(parseLong(dbConfig.getProperty("db.pool.validationTimeout", "5000")));
    ds.setLoginTimeout(parseInt(dbConfig.getProperty("db.pool.loginTimeout", "0"))); // in seconds
    ds.setMaxLifetime(parseLong(dbConfig.getProperty("db.pool.maxConnectionAge", "0"))); // in ms

    if (dbConfig.getProperty("db.pool.connectionInitSql") != null) {
      ds.setConnectionInitSql(dbConfig.getProperty("db.pool.connectionInitSql"));
    }
    
    // not used in HikariCP:
    // db.pool.initialSize
    // db.pool.idleConnectionTestPeriod
    // db.pool.maxIdleTimeExcessConnections
    // db.pool.acquireIncrement - HikariCP opens connections one at a time, as needed
    // db.pool.cache.statements - HikariCP does not offer statement caching
    // db.pool.idle.testInterval - HikariCP tests connections when they're leased, not on a timer
    // db.pool.connection.threshold - HikariCP doesn't have a percentile idle threshold; db.pool.size.idle can be used to keep a fixed number of connections idle
    // db.pool.threads - HikariCP does not use extra threads to "aid" connection release
    // db.pool.maxStatements - HikariCP does not offer PreparedStatement caching
    // db.pool.maxStatementsPerConnection - HikariCP does not offer PreparedStatement caching

    // I could not find an analogue for HikariCP:
//    ds.setAcquireRetryAttempts(parseInt(dbConfig.getProperty("db.pool.acquireRetryAttempts", "10")));
//    ds.setAcquireRetryDelay(parseInt(dbConfig.getProperty("db.pool.acquireRetryDelay", "1000")));
//    ds.setBreakAfterAcquireFailure(Boolean.parseBoolean(dbConfig.getProperty("db.pool.breakAfterAcquireFailure", "false")));
//    ds.setTestConnectionOnCheckin(Boolean.parseBoolean(dbConfig.getProperty("db.pool.testConnectionOnCheckin", "true")));
//    ds.setTestConnectionOnCheckout(Boolean.parseBoolean(dbConfig.getProperty("db.pool.testConnectionOnCheckout", "false")));
//    ds.setMaxAdministrativeTaskTime(parseInt(dbConfig.getProperty("db.pool.maxAdministrativeTaskTime", "0")));
//    ds.setNumHelperThreads(parseInt(dbConfig.getProperty("db.pool.numHelperThreads", "3")));
//    ds.setDebugUnreturnedConnectionStackTraces(Boolean.parseBoolean(dbConfig.getProperty("db.pool.debugUnreturnedConnectionStackTraces", "false")));
//    ds.setContextClassLoaderSource("library");
//    ds.setPrivilegeSpawnedThreads(true);

    if (dbConfig.getProperty("db.testquery") != null) {
      ds.setConnectionTestQuery(dbConfig.getProperty("db.testquery"));
    } else {
      String driverClass = dbConfig.getProperty("db.driver");
            /*
             * Pulled from http://dev.mysql.com/doc/refman/5.5/en/connector-j-usagenotes-j2ee-concepts-connection-pooling.html
             * Yes, the select 1 also needs to be in there.
             */
      if (driverClass.equals("com.mysql.jdbc.Driver")) {
        ds.setConnectionTestQuery("/* ping */ SELECT 1");
      }
    }

    // This check is not required, but here to make it clear that nothing changes for people
    // that don't set this configuration property. It may be safely removed.
    if(dbConfig.getProperty("db.isolation") != null) {
//    TODO not yet migrated from c3p0 to Hikari CP:
//      ds.setConnectionCustomizerClassName(PlayConnectionCustomizer.class.getName());
    }
    return ds;
  }

  @Override
  public String getStatus() throws SQLException {
    StringWriter sw = new StringWriter();
    PrintWriter out = new PrintWriter(sw);
    Set<String> dbNames = Configuration.getDbNames();

    for (String dbName : dbNames) {
      DataSource ds = DB.getDataSource(dbName);
      if (ds == null || !(ds instanceof HikariDataSource)) {
        out.println("Datasource:");
        out.println("~~~~~~~~~~~");
        out.println("(not yet connected)");
        return sw.toString();
      }
      HikariDataSource datasource = (HikariDataSource) ds;
      out.println("Datasource (" + dbName + "):");
      out.println("~~~~~~~~~~~");
      out.println("Jdbc url: " + getJdbcUrl(datasource));
      out.println("Jdbc driver: " + getDriverClass(datasource));
      out.println("Jdbc user: " + getUser(datasource));
      if (Play.mode.isDev()) {
        out.println("Jdbc password: " + datasource.getPassword());
      }
      out.println("Min idle: " + datasource.getMinimumIdle());
      out.println("Max pool size: " + datasource.getMaximumPoolSize());

      out.println("Max lifetime: " + datasource.getMaxLifetime());
      out.println("Leak detection threshold: " + datasource.getLeakDetectionThreshold());
      out.println("Initialization fail timeout: " + datasource.getInitializationFailTimeout());
      out.println("Validation timeout: " + datasource.getValidationTimeout());
      out.println("Idle timeout: " + datasource.getIdleTimeout());
      out.println("Login timeout: " + datasource.getLoginTimeout());
      out.println("Connection timeout: " + datasource.getConnectionTimeout());
      out.println("Test query : " + datasource.getConnectionTestQuery());
      out.println("\r\n");
    }
    return sw.toString();
  }

  @Override
  public String getDriverClass(DataSource ds) {
    return ((HikariConfig) ds).getDriverClassName();
  }

  @Override
  public String getJdbcUrl(DataSource ds) {
    return ((HikariConfig) ds).getJdbcUrl();
  }

  @Override
  public String getUser(DataSource ds) {
    return ((HikariConfig) ds).getUsername();
  }
}

