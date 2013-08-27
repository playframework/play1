package play.db;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;
import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;

import org.hibernate.impl.SessionImpl;
import com.sun.rowset.CachedRowSetImpl;

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
     * @return true if the next result is a ResultSet object; false if it is an update count or there are no more results 
     */
    public static boolean execute(String SQL) {
        Statement statement = null;
        try {
            statement = getConnection().createStatement();
            if(statement != null){
                return statement.execute(SQL);
            }
        } catch (SQLException ex) {
            throw new DatabaseException(ex.getMessage(), ex);
        }finally {
            safeCloseStatement(statement);
        }
        return false; 
    }

    /**
     * Execute an SQL query
     * @param SQL
     * @return The rowSet of the query
     */
    public static RowSet executeQuery(String SQL) {
        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = getConnection().createStatement();
            if(statement != null){
                rs = statement.executeQuery(SQL);
            }
            
            // Need to use a CachedRowSet that caches its rows in memory, which makes it possible to operate without always being connected to its data source
            CachedRowSet rowset = new CachedRowSetImpl();
            rowset.populate(rs);
            return rowset;     
        } catch (SQLException ex) {
            throw new DatabaseException(ex.getMessage(), ex);
        }finally {
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
