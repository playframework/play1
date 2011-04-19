package play.db;

import play.Logger;
import play.exceptions.DatabaseException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database connection utilities.
 *
 * This class holds reference to all DB configurations.
 * Each configuration has its own instance of DBConfig.
 *
 * dbConfigName corresponds to properties-names in application.conf.
 *
 * The default DBConfig is the one configured using 'db.' in application.conf
 *
 * dbConfigName = 'other' is configured like this:
 *
 * db_other = mem
 * db_other.user = batman
 *
 * This class also preserves backward compatibility by
 * directing static methods to the default DBConfig-instance
 */
public class DB {

    private static Map<String, DBConfig> dbConfigs = new HashMap<String, DBConfig>(1);
    private static DBConfig defaultDBConfig = null;


    /**
     * Sets the new list of db configurations.
     * Tries to preserve existing config if not changed
     * @param dbConfigNames
     */
    protected static void setConfigurations(List<String> dbConfigNames) {

        // remember old configs to detect what has been removed
        List<String> oldNames = new ArrayList<String>();
        oldNames.addAll( dbConfigs.keySet());

        for (String dbConfigName : dbConfigNames) {
            DBConfig dbConfig = dbConfigs.get(dbConfigName);
            if (dbConfig!=null) {
                // Config with this name already exists
                dbConfig.configure();
                oldNames.remove(dbConfigName);
            } else {
                // must add new config
                dbConfig = new DBConfig(dbConfigName);
                if (dbConfig.configure()) {
                    // The database was configured - lets add it..
                    dbConfigs.put(dbConfigName, dbConfig);
                    if (DBConfig.defaultDbConfigName.equals(dbConfigName)) {
                        defaultDBConfig = dbConfig;
                    }
                }
            }
        }

        // names left in oldNames should be removed
        for (String nameToRemove : oldNames) {
            dbConfigs.remove(nameToRemove);
        }
    }

    /**
     * The default DBConfig is the one configured using 'db.' in application.conf
     *
     * @return the default DBConfig
     */
    public static DBConfig getDBConfig() {
        return defaultDBConfig;
    }

    /**
     * dbConfigName corresponds to properties-names in application.conf.
     *
     * The default dbConfig is named 'play', and is the one configured using 'db.' in application.conf
     *
     * dbConfigName = 'other' is configured like this:
     *
     * db_other = mem
     * db_other.user = batman
     *
     * An exception is thrown if the config is not found
     *
     * @param dbConfigName name of the config
     * @return a DBConfig specified by name
     */
    public static DBConfig getDBConfig(String dbConfigName) {
        return getDBConfig(dbConfigName, false);
    }

    /**
     * dbConfigName corresponds to properties-names in application.conf.
     *
     * The default dbConfig is named 'play', and is the one configured using 'db.' in application.conf
     *
     * dbConfigName = 'other' is configured like this:
     *
     * db_other = mem
     * db_other.user = batman
     *
     * An exception is thrown if the config is not found, unless ignoreError == true
     *
     * @param dbConfigName name of the config
     * @param ignoreError set to true if null should be returned if config is missing
     * @return a DBConfig specified by name
     */
    public static DBConfig getDBConfig(String dbConfigName, boolean ignoreError) {
        DBConfig dbConfig = dbConfigs.get(dbConfigName);
        if (dbConfig==null && ignoreError == false) {
            throw new RuntimeException("No DBConfig found with the name " + dbConfigName);
        }
        return dbConfig;
    }

    /**
     * Close all connections opened for the current thread.
     */
    public static void close() {
        boolean error = false;
        for (DBConfig dbConfig : dbConfigs.values()) {
            // do our best to close all connections
            try {
                dbConfig.close();
            } catch (Exception e) {
                Logger.error("Error closing connection", e);
                error = true;
            }
        }
        if (error) {
            throw new DatabaseException("Error closing one or more connections");
        }
    }

    /**
     * Open a connection for the current thread.
     * @return A valid SQL connection
     */
    public static Connection getConnection() {
        return defaultDBConfig.getConnection();
    }

    /**
     * Execute an SQL update
     * @param SQL
     * @return false if update failed
     */
    public static boolean execute(String SQL) {
        return defaultDBConfig.execute(SQL);
    }

    /**
     * Execute an SQL query
     * @param SQL
     * @return The query resultSet
     */
    public static ResultSet executeQuery(String SQL) {
        return defaultDBConfig.executeQuery(SQL);
    }

    /**
     * Destroy the datasources
     */
    public static void destroy() {
        for (DBConfig dbConfig : dbConfigs.values()) {
            dbConfig.destroy();
        }
        dbConfigs.clear();
    }

    /**
     * Detects changes and reconfigures all dbConfigs
     */
    protected static void configure() {
        for (DBConfig dbConfig : dbConfigs.values()) {
            dbConfig.configure();
        }
    }

    /**
     * @return status string for all configured dbConfigs
     */
    protected static String getStatus() {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);

        for (DBConfig dbConfig : dbConfigs.values()) {
            out.print(dbConfig.getStatus());
        }

        return sw.toString();
    }

    public static Collection<DBConfig> getDBConfigs() {
        return dbConfigs.values();
    }

}
