package play.db.evolutions;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.Play;
import play.db.Configuration;
import play.db.DB;
import play.db.SQLSplitter;
import play.db.jpa.JPAPlugin;
import play.exceptions.UnexpectedException;


public class EvolutionQuery{
    
    public static void createTable(String dbName) throws SQLException {
        // If you are having problems with the default datatype text (clob for Oracle), you can
        // specify your own datatype using the 'evolution.PLAY_EVOLUTIONS.textType'-property
        String textDataType = Play.configuration.getProperty("evolution.PLAY_EVOLUTIONS.textType");
        if (textDataType == null) {
            if (isOracleDialectInUse(dbName)) {
                textDataType = "clob";
            } else {
                textDataType = "text";
            }
        }
        
        execute(dbName, "create table play_evolutions (id int not null, hash varchar(255) not null, applied_at timestamp not null, apply_script " + textDataType + ", revert_script " + textDataType + ", state varchar(255), last_problem " + textDataType + ", module_key varchar(255), constraint pk_id_module_key primary key (id, module_key))");
    }
    
    public static void alterForModuleSupport(String dbName, Connection connection) throws SQLException{
        // Add new column
        PreparedStatement ps1 = connection.prepareStatement("alter table play_evolutions add module_key varchar(255);");
        ps1.execute();
        closeStatement(ps1);
        
        // Set default value Assigning any existing evolutions to the parent project
        System.out.println("!!! - Assigning any existing evolutions to the parent project - !!!");
        PreparedStatement statement = connection.prepareStatement("update play_evolutions set module_key = ? where module_key is null");
        statement.setString(1, Play.configuration.getProperty("application.name"));
        statement.execute();
        closeStatement(statement);
       
        
        if(isMySqlDialectInUse(dbName)){
            // Drop previous primary key
            PreparedStatement ps2 = connection.prepareStatement( "alter table play_evolutions drop primary key;");
            ps2.execute();
            closeStatement(ps2);
        }else{
            // Drop previous primary key
            PreparedStatement ps3 = connection.prepareStatement("alter table play_evolutions drop constraint play_evolutions_pkey;");  
            ps3.execute();
            closeStatement(ps3);
        }
        
        // Add new primary key
        PreparedStatement ps4 = connection.prepareStatement("alter table play_evolutions add constraint pk_id_module_key primary key (id,module_key);");
        ps4.execute();
        closeStatement(ps4);
    }
    
    public static void resolve(String dbName, int revision, String moduleKey) throws SQLException {
        Connection connection = getNewConnection(dbName);
        PreparedStatement ps = connection.prepareStatement("update play_evolutions set state = ?, last_problem = ?  where state = ? and id = ? and module_key = ?" );
        ps.setString(1, EvolutionState.APPLIED.getStateWord() );
        ps.setString(2, "");
        ps.setString(3, EvolutionState.APPLYING_UP.getStateWord() );
        ps.setInt(4, revision);
        ps.setString(5, moduleKey);
        ps.execute();
        closeStatement(ps);
        
        PreparedStatement ps2 = connection.prepareStatement("delete from play_evolutions where state = ? and id = ? and module_key = ?" );
        ps2.setString(1, EvolutionState.APPLYING_DOWN.getStateWord() );
        ps2.setInt(2, revision);
        ps2.setString(3, moduleKey);
        ps2.execute();
        closeStatement(ps2);
    }
    
    public static void apply(Connection connection, boolean runScript, Evolution evolution, String moduleKey) throws SQLException {
        if (evolution.applyUp) {
            PreparedStatement ps = connection.prepareStatement("insert into play_evolutions values(?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, evolution.revision);
            ps.setString(2, evolution.hash);
            ps.setDate(3, new Date(System.currentTimeMillis()));
            ps.setString(4, evolution.sql_up);
            ps.setString(5, evolution.sql_down);
            ps.setString(6, EvolutionState.APPLYING_UP.getStateWord());
            ps.setString(7, "");
            ps.setString(8, moduleKey);
            ps.execute();
        } else {
            PreparedStatement ps = connection.prepareStatement("update play_evolutions set state = ? where id = ? and module_key = ?" );
            ps.setString(1, EvolutionState.APPLYING_DOWN.getStateWord() );
            ps.setInt(2, evolution.revision);
            ps.setString(3, moduleKey);
            ps.execute();
        }
       
        // Execute script
        if (runScript) {
           for (CharSequence sql : new SQLSplitter((evolution.applyUp ? evolution.sql_up : evolution.sql_down))) {
                String s = sql.toString().trim();
                if (StringUtils.isEmpty(s)) {
                    continue;
                }              
                connection.createStatement().execute(s);
            }
        }
        // Insert into logs
        if (evolution.applyUp) {
            PreparedStatement ps = connection.prepareStatement("update play_evolutions set state = ?, last_problem = ? where id = ? and module_key = ?");
            ps.setString(1, EvolutionState.APPLIED.getStateWord() );
            ps.setString(2, "");
            ps.setInt(3, evolution.revision);
            ps.setString(4, moduleKey);
            ps.execute();
        } else {
            PreparedStatement ps = connection.prepareStatement("delete from play_evolutions where id = ? and module_key = ?");
            ps.setInt(1, evolution.revision);
            ps.setString(2, moduleKey);
            ps.execute();
        }
    }
    
    public static void setProblem(Connection connection, int applying,
                                  String moduleKey, String message) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("update play_evolutions set last_problem = ? where id = ? and module_key = ?");
        ps.setString(1, message);
        ps.setInt(2, applying);
        ps.setString(3, moduleKey);
        ps.execute();
    }
    
    
    public static RowSet getEvolutionsToApply(Connection connection, String moduleKey) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet  = null;
        try {
            statement = connection
                    .prepareStatement("select id, hash, apply_script, revert_script, state, last_problem from play_evolutions where module_key = ? and state like 'applying_%'");
            statement.setString(1, moduleKey);
            resultSet = statement.executeQuery();
            // Need to use a CachedRowSet that caches its rows in memory, which
            // makes it possible to operate without always being connected to
            // its data source
            CachedRowSet rowset = RowSetProvider.newFactory().createCachedRowSet();
            rowset.populate(resultSet);
            return rowset;
        } catch (SQLException e) {
            throw e;
        } finally {
            closeResultSet(resultSet);
            closeStatement(statement);
        }
    }
    
    public static RowSet getEvolutions(Connection connection, String moduleKey) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet  = null;
        try {
            statement = connection
                    .prepareStatement("select id, hash, apply_script, revert_script from play_evolutions where module_key = ?");
            statement.setString(1, moduleKey);
            resultSet = statement.executeQuery();
            // Need to use a CachedRowSet that caches its rows in memory, which
            // makes it possible to operate without always being connected to
            // its data source
            CachedRowSet rowset = RowSetProvider.newFactory().createCachedRowSet();
            rowset.populate(resultSet);
            return rowset;
        } catch (SQLException e) {
            throw e;
        } finally {
            closeResultSet(resultSet);
            closeStatement(statement);
        }
    }
    
    // JDBC Utils
    private static void execute(String dbName, String sql) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getNewConnection(dbName);
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            throw e;
        } finally {
            closeStatement(statement);
            closeConnection(connection);
        }
    }
    
    public static Connection getNewConnection() throws SQLException {
        return getNewConnection(DB.DEFAULT, true); // Yes we want auto-commit
    }
    
    public static Connection getNewConnection(String dbName) throws SQLException {
        return getNewConnection(dbName, true); // Yes we want auto-commit
    }

    public static Connection getNewConnection(String dbName, boolean autoCommit) throws SQLException {
        Connection connection = DB.getDataSource(dbName).getConnection();
        connection.setAutoCommit(autoCommit); 
        return connection;
    }
    
    public static void closeResultSet(ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
    
    public static void closeStatement(Statement statement) {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public static void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    private static synchronized boolean isOracleDialectInUse(String dbName) {
        boolean isOracle = false;
        Configuration dbConfig = new Configuration(dbName);
        String jpaDialect = JPAPlugin.getDefaultDialect(dbConfig.getProperty("db.driver")); 
        if (jpaDialect != null) {
            try {
                Class<?> dialectClass = Play.classloader.loadClass(jpaDialect);

                // Oracle 8i dialect is the base class for oracle dialects (at least for now)
                isOracle = org.hibernate.dialect.Oracle8iDialect.class.isAssignableFrom(dialectClass);
            } catch (ClassNotFoundException e) {
                // swallow
                Logger.warn("jpa.dialect class %s not found", jpaDialect);
            }
        }
        return isOracle;
    }
    
    private static boolean isMySqlDialectInUse(String dbName) {
        boolean isMySQl = false;
        Configuration dbConfig = new Configuration(dbName);
        String jpaDialect = JPAPlugin.getDefaultDialect(dbConfig.getProperty("db.driver"));
        if (jpaDialect != null) {
            try {
                Class<?> dialectClass = Play.classloader.loadClass(jpaDialect);

                // MySQLDialect is the base class for MySQL dialects
                isMySQl = org.hibernate.dialect.MySQLDialect.class.isAssignableFrom(dialectClass);
            } catch (ClassNotFoundException e) {
                // swallow
                Logger.warn("jpa.dialect class %s not found", jpaDialect);
            }
        }
        return isMySQl;
    }

}