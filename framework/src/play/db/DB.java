package play.db;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.hibernate.impl.SessionImpl;
import play.db.jpa.JPA;
import play.exceptions.DatabaseException;
import play.Logger;

/**
 * Database connection utilities.
 */
public class DB {

    /**
     * The loaded datasource.
     */
    public static DataSource datasource = null;

    /**
     * The method used to destroy the datasource
     */
    public static String destroyMethod = "";

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
                throw new DatabaseException("It's possible than the connection was not properly closed !", e);
            }
        }
    }
    static ThreadLocal<Connection> localConnection = new ThreadLocal<Connection>();

    /**
     * Open a connection for the current thread.
     * @return A valid SQL connection
     */
    @SuppressWarnings("deprecation")
    public static Connection getConnection() {
        try {
            if (JPA.isEnabled()) {
                return ((SessionImpl)((org.hibernate.ejb.EntityManagerImpl) JPA.em()).getSession()).connection();
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

    /**
     * Destroy the datasource
     */
    public static void destroy() {
        try {
            if (DB.datasource != null && DB.destroyMethod != null && !DB.destroyMethod.equals("")) {
                Method close = DB.datasource.getClass().getMethod(DB.destroyMethod, new Class[] {});
                if (close != null) {
                    close.invoke(DB.datasource, new Object[] {});
                    DB.datasource = null;
                    Logger.trace("Datasource destroyed");
                }
            }
        } catch (Throwable t) {
             Logger.error("Couldn't destroy the datasource", t);
        }
    }
}
