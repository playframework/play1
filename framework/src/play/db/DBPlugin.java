package play.db;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.Play;
import play.PlayPlugin;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;

/**
 * The DB plugin
 */
public class DBPlugin extends PlayPlugin {

    org.h2.tools.Server h2Server;

    @Override
    public boolean rawInvocation(Request request, Response response) throws Exception {
        if (Play.mode.isDev() && request.path.equals("/@db")) {
            response.status = Http.StatusCode.MOVED;

            // For H2 embeded database, we'll also start the Web console
            if (h2Server != null) {
                h2Server.stop();
            }
            h2Server = org.h2.tools.Server.createWebServer();
            h2Server.start();

            response.setHeader("Location", "http://localhost:8082/");
            return true;
        }
        return false;
    }

    @Override
    public void onApplicationStart() {
        System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
        System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "OFF");

        List<String> dbConfigNames = new ArrayList<String>(1);

        // first we must look for and configure the default dbConfig
        if (isDefaultDBConfigPresent(Play.configuration)) {
            // Can only add default db config-name if it is present in config-file.
            // Must do this to be able to detect if user removes default db config from config file
            dbConfigNames.add(DBConfig.defaultDbConfigName);
        }

        //look for other configurations
        dbConfigNames.addAll(detectedExtraDBConfigs(Play.configuration));

        DB.setConfigurations(dbConfigNames);

    }

    /**
     * @return true if default db config properties is found
     */
    protected boolean isDefaultDBConfigPresent(Properties props) {
        Pattern pattern = Pattern.compile("^db(?:$|\\..*)");
        for( String propName : props.stringPropertyNames()) {
            Matcher m = pattern.matcher(propName);
            if (m.find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Looks for extra db configs in config.
     *
     * Properties starting with 'db_'
     * @return list of all extra db config names found
     */
    protected Set<String> detectedExtraDBConfigs(Properties props) {
        Set<String> names = new LinkedHashSet<String>(0); //preserve order
        Pattern pattern = Pattern.compile("^db\\_([^\\.]+)(?:$|\\..*)");
        for( String propName : props.stringPropertyNames()) {
            Matcher m = pattern.matcher(propName);
            if (m.find()) {
                String configName = m.group(1);
                if (!names.contains(configName)) {
                    names.add(configName);
                }
            }
        }
        return names;
    }

    @Override
    public void onApplicationStop() {
        if (Play.mode.isProd()) {
            DB.destroy();
        }
    }

    @Override
    public String getStatus() {
        return DB.getStatus();
    }

    @Override
    public void invocationFinally() {
        DB.close();
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
