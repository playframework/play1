package play.db;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;
import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;

import org.hibernate.internal.SessionImpl;

import play.Logger;
import play.db.jpa.JPA;
import play.exceptions.DatabaseException;

/**
 * Database connection utilities.
 */
public class DB {

    /**
     * The loaded datasource.
     * 
     * @see ExtendedDatasource
     */
    protected static final Map<String, ExtendedDatasource> datasources = new ConcurrentHashMap<>();

    public static class ExtendedDatasource {

        /**
         * Connection to the physical data source
         */
        private DataSource datasource;

        /**
         * The method used to destroy the data source
         */
        private String destroyMethod;

        public ExtendedDatasource(DataSource ds, String destroyMethod) {
            this.datasource = ds;
            this.destroyMethod = destroyMethod;
        }

        public String getDestroyMethod() {
            return destroyMethod;
        }

        public DataSource getDataSource() {
            return datasource;
        }

    }

    /**
     * @deprecated Use datasources instead
     * @since 1.3.0
     * @see #datasources
     * @see ExtendedDatasource
     */
    @Deprecated
    public static DataSource datasource = null;
    /**
     * The method used to destroy the datasource
     * 
     * @deprecated Use datasources instead
     * @since 1.3.0
     * @see #datasources
     * @see ExtendedDatasource
     */
    @Deprecated
    public static String destroyMethod = "";

    public static final String DEFAULT = "default";

    static final ThreadLocal<Map<String, Connection>> localConnection = new ThreadLocal<>();

    public static DataSource getDataSource(String name) {
        if (datasources.get(name) != null) {
            return datasources.get(name).getDataSource();
        }
        return null;
    }

    public static DataSource getDataSource() {
        return getDataSource(DEFAULT);
    }

    public static Connection getConnection(String name, boolean autocommit) {
        try {
            Connection connection = getDataSource(name).getConnection();
            connection.setAutoCommit(autocommit);
            return connection;
        } catch (Exception e) {
            // Exception
            throw new DatabaseException(e.getMessage());
        }
    }

    private static Connection getLocalConnection(String name) {
        Map<String, Connection> map = localConnection.get();
        if (map != null) {
            Connection connection = map.get(name);
            return connection;
        }
        return null;
    }

    private static void registerLocalConnection(String name, Connection connection) {
        Map<String, Connection> map = localConnection.get();
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(name, connection);
        localConnection.set(map);
    }

    /**
     * Close all the open connections for the current thread.
     */
    public static void closeAll() {
        Map<String, Connection> map = localConnection.get();
        if (map != null) {
            Set<String> keySet = new HashSet<>(map.keySet());
            for (String name : keySet) {
                close(name);
            }
        }
    }

    /**
     * Close all the open connections for the current thread.
     */
    public static void close() {
        close(DEFAULT);
    }

    /**
     * Close an given open connections for the current thread
     * 
     * @param name
     *            Name of the DB
     */
    public static void close(String name) {
        Map<String, Connection> map = localConnection.get();
        if (map != null) {
            Connection connection = map.get(name);
            if (connection != null) {
                map.remove(name);
                localConnection.set(map);
                try {
                    connection.close();
                } catch (Exception e) {
                    throw new DatabaseException("It's possible than the connection '" + name + "'was not properly closed !", e);
                }
            }
        }
    }

    /**
     * Open a connection for the current thread.
     * 
     * @param name
     *            Name of the DB
     * @return A valid SQL connection
     */
    public static Connection getConnection(String name) {
        try {
            if (JPA.isEnabled()) {
                return ((SessionImpl) ((org.hibernate.Session) JPA.em(name)).getSession()).connection();
            }

            Connection localConnection = getLocalConnection(name);
            if (localConnection != null) {
                return localConnection;
            }

            // We have no connection
            Connection connection = getDataSource(name).getConnection();
            registerLocalConnection(name, connection);
            return connection;
        } catch (NullPointerException e) {
            if (getDataSource(name) == null) {
                throw new DatabaseException("No database found. Check the configuration of your application.", e);
            }
            throw e;
        } catch (Exception e) {
            // Exception
            throw new DatabaseException(e.getMessage());
        }
    }

    public static Connection getConnection() {
        return getConnection(DEFAULT);
    }

    /**
     * Execute an SQL update
     *
     * @param name
     *            the DB name
     * @param SQL
     *            the SQL statement
     * @return true if the next result is a ResultSet object; false if it is an update count or there are no more
     *         results
     */
    public static boolean execute(String name, String SQL) {
        Statement statement = null;
        try {
            statement = getConnection(name).createStatement();
            if (statement != null) {
                return statement.execute(SQL);
            }
        } catch (SQLException ex) {
            throw new DatabaseException(ex.getMessage(), ex);
        } finally {
            safeCloseStatement(statement);
        }
        return false;
    }

    /**
     * Execute an SQL update
     *
     * @param SQL
     *            the SQL statement
     * @return true if the next result is a ResultSet object; false if it is an update count or there are no more
     *         results
     */
    public static boolean execute(String SQL) {
        return execute(DEFAULT, SQL);
    }

    /**
     * Execute an SQL query
     *
     * @param SQL
     *            the SQL statement
     * @return The ResultSet object; false if it is an update count or there are no more results
     */
    public static RowSet executeQuery(String SQL) {
        return executeQuery(DEFAULT, SQL);
    }

    /**
     * Execute an SQL query
     * 
     * @param name
     *            the DB name
     * @param SQL
     *            the SQL statement
     * @return The rowSet of the query
     */
    public static RowSet executeQuery(String name, String SQL) {
        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = getConnection(name).createStatement();
            if (statement != null) {
                rs = statement.executeQuery(SQL);
            }

            // Need to use a CachedRowSet that caches its rows in memory, which
            // makes it possible to operate without always being connected to
            // its data source
            CachedRowSet rowset = RowSetProvider.newFactory().createCachedRowSet();
            rowset.populate(rs);
            return rowset;
        } catch (SQLException ex) {
            throw new DatabaseException(ex.getMessage(), ex);
        } finally {
            safeCloseResultSet(rs);
            safeCloseStatement(statement);
        }
    }

    public static void safeCloseResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException ex) {
                throw new DatabaseException(ex.getMessage(), ex);
            }
        }
    }

    public static void safeCloseStatement(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ex) {
                throw new DatabaseException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * Destroy the datasource
     * 
     * @param name
     *            the DB name
     */
    public static void destroy(String name) {
        try {
            ExtendedDatasource extDatasource = datasources.get(name);
            if (extDatasource != null && extDatasource.getDestroyMethod() != null) {
                Method close = extDatasource.datasource.getClass().getMethod(extDatasource.getDestroyMethod(), new Class[] {});
                if (close != null) {
                    close.invoke(extDatasource.getDataSource(), new Object[] {});
                    datasources.remove(name);
                    DB.datasource = null;
                    Logger.trace("Datasource destroyed");
                }
            }
        } catch (Throwable t) {
            Logger.error("Couldn't destroy the datasource", t);
        }
    }

    /**
     * Destroy the datasource
     */
    public static void destroy() {
        destroy(DEFAULT);
    }

    /**
     * Destroy all datasources
     */
    public static void destroyAll() {
        Set<String> keySet = new HashSet<>(datasources.keySet());
        for (String name : keySet) {
            destroy(name);
        }
    }
}
