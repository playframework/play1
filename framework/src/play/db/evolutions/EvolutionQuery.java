package play.db.evolutions;

import play.Play;
import play.db.DB;
import play.db.SQLSplitter;
import play.exceptions.UnexpectedException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;


public class EvolutionQuery{
    
    public static void createTable() throws SQLException {
	   execute("create table play_evolutions (id int not null, hash varchar(255) not null, applied_at timestamp not null, apply_script text, revert_script text, state varchar(255), last_problem text, module_key varchar(255), constraint pk_id_module_key primary key (id, module_key))");
    }
    
    public static void alterForModuleSupport(Connection connection) throws SQLException{
        // Add new column
	execute("alter table play_evolutions add module_key varchar(255);");
        
        // Set default value Assigning any existing evolutions to the parent project
        System.out.println("!!! - Assigning any existing evolutions to the parent project - !!!");
        PreparedStatement statement = connection.prepareStatement("update play_evolutions set module_key = ? where module_key is null");
        statement.setString(1, Play.configuration.getProperty("application.name"));
        statement.execute();
       
        // Drop previous primary key
        execute("alter table play_evolutions drop constraint play_evolutions_pkey;");  
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
            execute("update play_evolutions set state = 'applying_down' where id = " + evolution.revision + " and module_key = '" + moduleKey + "'");
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

    public static Connection getNewConnection() throws SQLException {
        Connection connection = DB.datasource.getConnection();
        connection.setAutoCommit(true); // Yes we want auto-commit
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


}