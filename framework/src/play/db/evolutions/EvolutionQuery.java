package play.db.evolutions;

import play.Logger;
import play.Play;
import play.db.DB;
import play.db.DBConfig;
import play.db.SQLSplitter;
import play.exceptions.UnexpectedException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;


public class EvolutionQuery{
    
    public static void createTable() throws SQLException {
        // If you are having problems with the default datatype text (clob for Oracle), you can
        // specify your own datatype using the 'evolution.PLAY_EVOLUTIONS.textType'-property
        String textDataType = Play.configuration.getProperty("evolution.PLAY_EVOLUTIONS.textType");
        if (textDataType == null) {
            if (isOracleDialectInUse()) {
                textDataType = "clob";
            } else {
                textDataType = "text";
            }
        }
        
	execute("create table play_evolutions (id int not null, hash varchar(255) not null, applied_at timestamp not null, apply_script " + textDataType + ", revert_script " + textDataType + ", state varchar(255), last_problem " + textDataType + ", module_key varchar(255), constraint pk_id_module_key primary key (id, module_key))");
    }
    
    public static void alterForModuleSupport(Connection connection) throws SQLException{
        // Add new column
	execute("alter table play_evolutions add module_key varchar(255);");
        
        // Set default value Assigning any existing evolutions to the parent project
        System.out.println("!!! - Assigning any existing evolutions to the parent project - !!!");
        PreparedStatement statement = connection.prepareStatement("update play_evolutions set module_key = ? where module_key is null");
        statement.setString(1, Play.configuration.getProperty("application.name"));
        statement.execute();
       
        
        if(isMySqlDialectInUse()){
            // Drop previous primary key
            execute("alter table play_evolutions drop primary key;");
        }else{
            // Drop previous primary key
            execute("alter table play_evolutions drop constraint play_evolutions_pkey;");  
        }
        
        // Add new primary key
        execute("alter table play_evolutions add constraint pk_id_module_key primary key (id,module_key);"); 
    }
    
    public static void resolve(int revision, String moduleKey) throws SQLException {
	Connection connection = getNewConnection();
        PreparedStatement ps = connection.prepareStatement("update play_evolutions set state = ?, last_problem = ?  where state = ? and id = ? and module_key = ?" );
        ps.setString(1, EvolutionState.APPLIED.getStateWord() );
        ps.setString(2, "");
        ps.setString(3, EvolutionState.APPLYING_UP.getStateWord() );
        ps.setInt(4, revision);
        ps.setString(5, moduleKey);
        ps.execute();
        
        PreparedStatement ps2 = connection.prepareStatement("delete from play_evolutions where state = ? and id = ? and module_key = ?" );
        ps2.setString(1, EvolutionState.APPLYING_DOWN.getStateWord() );
        ps2.setInt(2, revision);
        ps2.setString(3, moduleKey);
        ps2.execute();
    }
    
    public static void apply(Connection connection,boolean runScript, Evolution evolution, String moduleKey) throws SQLException {
        if (evolution.applyUp) {
            PreparedStatement ps = connection.prepareStatement("insert into play_evolutions values(?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, evolution.revision);
            ps.setString(2, evolution.hash);
            ps.setDate(3, new Date(System.currentTimeMillis()));
            ps.setString(4, evolution.sql_up);
            ps.setString(5, evolution.sql_down);
            ps.setString(6, "applying_up");
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
                final String s = sql.toString().trim();
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
    
    
    public static ResultSet getEvolutionsToApply(Connection connection, String moduleKey) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("select id, hash, apply_script, revert_script, state, last_problem from play_evolutions where module_key = ? and state like 'applying_%'"); 
        statement.setString(1, moduleKey);
        return statement.executeQuery();
    }
    
    public static ResultSet getEvolutions(Connection connection, String moduleKey) throws SQLException {
	PreparedStatement statement = connection.prepareStatement("select id, hash, apply_script, revert_script from play_evolutions where module_key = ?"); 
	statement.setString(1, moduleKey);
        return statement.executeQuery();
    }
    
    // JDBC Utils
    private static void execute(String sql) throws SQLException {
        Connection connection = null;
        try {
            connection = getNewConnection();
            connection.createStatement().execute(sql);
        } catch (SQLException e) {
            throw e;
        } finally {
            closeConnection(connection);
        }
    }
    
    public static DataSource getDatasource() {
        DBConfig dbConfig = DB.getDBConfig(DBConfig.defaultDbConfigName, true);
        if (dbConfig==null) {
            return null;
        }
        return dbConfig.getDatasource();
    }

    public static Connection getNewConnection() throws SQLException {
        return getNewConnection(true); // Yes we want auto-commit
    }

    public static Connection getNewConnection(boolean autoCommit) throws SQLException {
        Connection connection = getDatasource().getConnection();
        connection.setAutoCommit(autoCommit);
        return connection;
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

    private synchronized static boolean isOracleDialectInUse() {
        boolean isOracle = false;

        String jpaDialect = Play.configuration.getProperty("jpa.dialect");
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
    
    private static boolean isMySqlDialectInUse() {
        boolean isMySQl = false;
	String jpaDialect = Play.configuration.getProperty("jpa.dialect"); 
	 if (jpaDialect != null) {
	    try {
		Class<?> dialectClass = Play.classloader.loadClass(jpaDialect);

		// MySQLDialect is the base class for MySQL dialects
		isMySQl = play.db.jpa.MySQLDialect.class
			.isAssignableFrom(dialectClass);
	    } catch (ClassNotFoundException e) {
		// swallow
		Logger.warn("jpa.dialect class %s not found", jpaDialect);
	    }
	}
	return isMySQl;
    }

}