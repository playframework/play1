package play.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloader;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.libs.Codec;
import play.libs.IO;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Redirect;
import play.vfs.VirtualFile;

import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

/**
 * Handles migration of data.
 *
 * Does only support the default DBConfig
 */
public class Evolutions extends PlayPlugin {

    private static Map<String, VirtualFile> modulesWithEvolutions = new LinkedHashMap<String, VirtualFile>();
    /**
     * Indicates if evolutions is disabled in application.conf ("evolutions.enabled" property)
     */
    private boolean disabled = false;

    protected static ComboPooledDataSource getDatasource() {
        DBConfig dbConfig = DB.getDBConfig(DBConfig.defaultDbConfigName, true);
        if (dbConfig==null) {
            return null;
        }
        return (ComboPooledDataSource)dbConfig.getDatasource();
    }

    public static void main(String[] args) {


        /** Start the DB plugin **/
        Play.id = System.getProperty("play.id");
        Play.applicationPath = new File(System.getProperty("application.path"));
        Play.guessFrameworkPath();
        Play.readConfiguration();
        Play.javaPath = new ArrayList<VirtualFile>();
        Play.classes = new ApplicationClasses();
        Play.classloader = new ApplicationClassloader();

        Play.templatesPath = new ArrayList<VirtualFile>();
        Play.modulesRoutes = new HashMap<String, VirtualFile>();
        Play.loadModules();


        if (System.getProperty("modules") != null) {
            populateModulesWithSpecificModules();
        } else {
            populateModulesWithEvolutions();
        }

        if (modulesWithEvolutions.isEmpty()) {
            System.out.println("~ Nothing has evolutions, go away and think again.");
            return;
        }

        Logger.init();
        Logger.setUp("ERROR");
        new DBPlugin().onApplicationStart();

        /** Connected **/
        System.out.println("~ Connected to " + getDatasource().getJdbcUrl());

        for(Entry<String, VirtualFile> moduleRoot : modulesWithEvolutions.entrySet()) {       

            /** Sumary **/
            Evolution database = listDatabaseEvolutions(moduleRoot.getKey()).peek();
            Evolution application = listApplicationEvolutions(moduleRoot.getKey(), moduleRoot.getValue()).peek();

            if ("resolve".equals(System.getProperty("mode"))) {
                try {
                    checkEvolutionsState();
                    System.out.println("~");
                    System.out.println("~ Nothing to resolve for " + moduleRoot.getKey() + "...");
                    System.out.println("~");
                    return;
                } catch (InconsistentDatabase e) {
                    resolve(e.revision);
                    System.out.println("~");
                    System.out.println("~ Revision " + e.revision + " for " + moduleRoot.getKey() + " has been resolved;");
                    System.out.println("~");
                } catch (InvalidDatabaseRevision e) {
                    // see later
                }
            }

            /** Check inconsistency **/
            try {
                checkEvolutionsState();
            } catch (InconsistentDatabase e) {
                System.out.println("~");
                System.out.println("~ Your database is in an inconsistent state!");
                System.out.println("~");
                System.out.println("~ While applying this script part:");
                System.out.println("");
                System.out.println(e.evolutionScript);
                System.out.println("");
                System.out.println("~ The following error occured:");
                System.out.println("");
                System.out.println(e.error);
                System.out.println("");
                System.out.println("~ Please correct it manually, and mark it resolved by running `play evolutions:resolve`");
                System.out.println("~");
                return;
            } catch (InvalidDatabaseRevision e) {
                // see later
            }

            System.out.print("~ '" + moduleRoot.getKey()+ "' Application revision is " + application.revision + " [" + application.hash.substring(0, 7) + "]");
            System.out.println(" and '" + moduleRoot.getKey()+ "' Database revision is " + database.revision + " [" + database.hash.substring(0, 7) + "]");
            System.out.println("~");

            /** Evolution script **/
            List<Evolution> evolutions = getEvolutionScript(moduleRoot.getKey(), moduleRoot.getValue());
            if (evolutions.isEmpty()) {
                System.out.println("~ Your database is up to date for " + moduleRoot.getKey());
                System.out.println("~");
            } else {

                if ("apply".equals(System.getProperty("mode"))) {

                    System.out.println("~ Applying evolutions for " + moduleRoot.getKey() + ":");
                    System.out.println("");
                    System.out.println("# ------------------------------------------------------------------------------");
                    System.out.println("");
                    System.out.println(toHumanReadableScript(evolutions));
                    System.out.println("");
                    System.out.println("# ------------------------------------------------------------------------------");
                    System.out.println("");
                    if (applyScript(true, moduleRoot.getKey(), moduleRoot.getValue())) {
                        System.out.println("~");
                        System.out.println("~ Evolutions script successfully applied for " + moduleRoot.getKey() + "!");
                        System.out.println("~");
                    } else {
                        System.out.println("~");
                        System.out.println("~ Can't apply evolutions for " + moduleRoot.getKey() + "...");
                        System.out.println("~");
                    }


                } else if ("markApplied".equals(System.getProperty("mode"))) {

                    if (applyScript(false, moduleRoot.getKey(), moduleRoot.getValue())) {
                        System.out.println("~ Evolutions script marked as applied for " + moduleRoot.getKey() + "!");
                        System.out.println("~");
                    } else {
                        System.out.println("~ Can't apply evolutions for " + moduleRoot.getKey() + "...");
                        System.out.println("~");
                    }

                } else {

                    System.out.println("~ Your database needs evolutions for " + moduleRoot.getKey() + "!");
                    System.out.println("");
                    System.out.println("# ------------------------------------------------------------------------------");
                    System.out.println("");
                    System.out.println(toHumanReadableScript(evolutions));
                    System.out.println("");
                    System.out.println("# ------------------------------------------------------------------------------");
                    System.out.println("");
                    System.out.println("~ Run `play evolutions:apply` to automatically apply this script to the database");
                    System.out.println("~ or apply it yourself and mark it done using `play evolutions:markApplied`");
                    System.out.println("~");
                }
            }
        }
    }

    private static void populateModulesWithSpecificModules() {
        String[] specificModules = System.getProperty("modules").split(",");

        System.out.println("~ You've requested running evolutions only for these modules: ");
        for (String specificModule : specificModules) {
            System.out.println("~~ '" + specificModule + "'");
        }
        System.out.println("~");

        boolean weShouldAddTheMainProject = false;

        for (String specificModule : specificModules) {
            if (Play.modules.containsKey(specificModule)) {
                VirtualFile moduleRoot = Play.modules.get(specificModule);
                
                if(moduleRoot.child("db/evolutions").exists()) {
                    modulesWithEvolutions.put(specificModule, moduleRoot.child("db/evolutions"));
                } else {
                    System.out.println("~ '" + specificModule + "' module doesn't have any evolutions scripts in it.  Are you sure your brain is working?");
                }
            } else if (Play.configuration.getProperty("application.name").equals(specificModule))  {
                weShouldAddTheMainProject = true;
            } else {
                System.out.println("~ Couldn't find a module with the name '" + specificModule + "' .  Are you sure you spelt it correctly?");
            }
        }

        if (weShouldAddTheMainProject) {
            addMainProjectToModuleList();
        }
    }

    private static void populateModulesWithEvolutions() {
        /** Check that evolutions are enabled **/

        for(Entry<String, VirtualFile> moduleRoot : Play.modules.entrySet()) {            
            if(moduleRoot.getValue().child("db/evolutions").exists()) {
                modulesWithEvolutions.put(moduleRoot.getKey(), moduleRoot.getValue().child("db/evolutions"));
            }
        }

        addMainProjectToModuleList();
    }

    private static void addMainProjectToModuleList() {
        if (evolutionsDirectory.exists()) {
            modulesWithEvolutions.put(Play.configuration.getProperty("application.name"), VirtualFile.open(evolutionsDirectory));
        }
    }

    static File evolutionsDirectory = Play.getFile("db/evolutions");

    @Override
    public boolean rawInvocation(Request request, Response response) throws Exception {

        // Mark an evolution as resolved
        if (Play.mode.isDev() && request.method.equals("POST") && request.url.matches("^/@evolutions/force/[0-9]+$")) {
            int revision = Integer.parseInt(request.url.substring(request.url.lastIndexOf("/") + 1));
            resolve(revision);
            new Redirect("/").apply(request, response);
            return true;
        }

        // Apply the current evolution script
        if (Play.mode.isDev() && request.method.equals("POST") && request.url.equals("/@evolutions/apply")) {
            
             for(Entry<String, VirtualFile> moduleRoot : modulesWithEvolutions.entrySet()) {            
                 applyScript(true, moduleRoot.getKey(), moduleRoot.getValue());
             }
            new Redirect("/").apply(request, response);
            return true;
        }
        return super.rawInvocation(request, response);
    }

    @Override
    public void beforeInvocation() {
        if(disabled || Play.mode.isProd()) {
            return;
        }
        try {
            checkEvolutionsState();
        } catch (InvalidDatabaseRevision e) {
        	Logger.info("Automatically applying evolutions in in-memory database");
            for(Entry<String, VirtualFile> moduleRoot : modulesWithEvolutions.entrySet()) {            
                if ("mem".equals(Play.configuration.getProperty("db")) && listDatabaseEvolutions(moduleRoot.getKey()).peek().revision == 0) {
                	Logger.info("Applying evolutions for '" + moduleRoot.getKey() + "'");
                    applyScript(true, moduleRoot.getKey(), moduleRoot.getValue());
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public void onApplicationStart() {

        disabled = "false".equals(Play.configuration.getProperty("evolutions.enabled", "true"));

        if (!disabled) {
            populateModulesWithEvolutions();

            if (Play.mode.isProd()) {
                try {
                    checkEvolutionsState();
                } catch (InvalidDatabaseRevision e) {
                    Logger.warn("");
                    Logger.warn("Your database is not up to date.");
                    Logger.warn("Use `play evolutions` command to manage database evolutions.");
                    throw e;
                }
            }
        }        
    }

    public static synchronized void resolve(int revision) {
        try {
            execute("update play_evolutions set state = 'applied' where state = 'applying_up' and id = " + revision);
            execute("delete from play_evolutions where state = 'applying_down' and id = " + revision);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public static synchronized boolean applyScript(boolean runScript, String moduleKey, VirtualFile evolutionsDirectory) {
        try {
            Connection connection = getNewConnection();
            int applying = -1;
            try {
                for (Evolution evolution : getEvolutionScript(moduleKey, evolutionsDirectory)) {
                    applying = evolution.revision;

                    // Insert into logs
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
                        execute("update play_evolutions set state = 'applying_down' where id = " + evolution.revision);
                    }
                    // Execute script
                    if (runScript) {
                       for (CharSequence sql : new SQLSplitter((evolution.applyUp ? evolution.sql_up : evolution.sql_down))) {
                            final String s = sql.toString().trim();
                            if (StringUtils.isEmpty(s)) {
                                continue;
                            }
                            execute(s);
                        }
                    }
                    // Insert into logs
                    if (evolution.applyUp) {
                        execute("update play_evolutions set state = 'applied' where id = " + evolution.revision);
                    } else {
                        execute("delete from play_evolutions where id = " + evolution.revision);
                    }
                }
                return true;
            } catch (Exception e) {
                String message = e.getMessage();
                if (e instanceof SQLException) {
                    SQLException ex = (SQLException) e;
                    message += " [ERROR:" + ex.getErrorCode() + ", SQLSTATE:" + ex.getSQLState() + "]";
                }
                PreparedStatement ps = connection.prepareStatement("update play_evolutions set last_problem = ? where id = ?");
                ps.setString(1, message);
                ps.setInt(2, applying);
                ps.execute();
                closeConnection(connection);
                Logger.error(e, "Can't apply evolution");
                return false;
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public static String toHumanReadableScript(List<Evolution> evolutionScript) {
        // Construct the script
        StringBuilder sql = new StringBuilder();
        boolean containsDown = false;
        for (Evolution evolution : evolutionScript) {
            if (!evolution.applyUp) {
                containsDown = true;
            }
            sql.append("# --- Rev:").append(evolution.revision).append(",").append(evolution.applyUp ? "Ups" : "Downs").append(" - ").append(evolution.hash.substring(0, 7)).append("\n");
            sql.append("\n");
            sql.append(evolution.applyUp ? evolution.sql_up : evolution.sql_down);
            sql.append("\n\n");
        }

        if (containsDown) {
            sql.insert(0, "# !!! WARNING! This script contains DOWNS evolutions that are likely destructives\n\n");
        }

        return sql.toString().trim();
    }

    public synchronized static void checkEvolutionsState() {

        for(Entry<String, VirtualFile> moduleRoot : modulesWithEvolutions.entrySet()) {            

            if (getDatasource() != null) {
                List<Evolution> evolutionScript = getEvolutionScript(moduleRoot.getKey(), moduleRoot.getValue());
                Connection connection = null;
                try {
                    connection = getNewConnection();
                    PreparedStatement statement = connection.prepareStatement("select id, hash, apply_script, revert_script, state, last_problem from play_evolutions where module_key = ? and state like 'applying_%'"); 
                    statement.setString(1, moduleRoot.getKey());
                    ResultSet rs = statement.executeQuery();
                    if (rs.next()) {
                        int revision = rs.getInt("id");
                        String state = rs.getString("state");
                        String hash = rs.getString("hash").substring(0, 7);
                        String script = "";
                        if (state.equals("applying_up")) {
                            script = rs.getString("apply_script");
                        } else {
                            script = rs.getString("revert_script");
                        }
                        script = "# --- Rev:" + revision + "," + (state.equals("applying_up") ? "Ups" : "Downs") + " - " + hash + "\n\n" + script;
                        String error = rs.getString("last_problem");
                        throw new InconsistentDatabase(script, error, revision);
                    }
                } catch (SQLException e) {
                    throw new UnexpectedException(e);
                } finally {
                    closeConnection(connection);
                }

                if (!evolutionScript.isEmpty()) {
                    throw new InvalidDatabaseRevision(toHumanReadableScript(evolutionScript));
                }
            }
        }
    }

    public synchronized static List<Evolution> getEvolutionScript(String moduleKey, VirtualFile evolutionsDirectory) {
        Stack<Evolution> app = listApplicationEvolutions(moduleKey, evolutionsDirectory);
        Stack<Evolution> db = listDatabaseEvolutions(moduleKey);
        List<Evolution> downs = new ArrayList<Evolution>();
        List<Evolution> ups = new ArrayList<Evolution>();

        // Apply non conflicting evolutions (ups and downs)
        while (db.peek().revision != app.peek().revision) {
            if (db.peek().revision > app.peek().revision) {
                downs.add(db.pop());
            } else {
                ups.add(app.pop());
            }
        }

        // Revert conflicting to fork node
        while (db.peek().revision == app.peek().revision && !(db.peek().hash.equals(app.peek().hash))) {
            downs.add(db.pop());
            ups.add(app.pop());
        }

        // Ups need to be applied earlier first
        Collections.reverse(ups);

        List<Evolution> script = new ArrayList<Evolution>();
        script.addAll(downs);
        script.addAll(ups);

        return script;
    }

    public synchronized static Stack<Evolution> listApplicationEvolutions(String moduleKey, VirtualFile evolutionsDirectory) {
        Stack<Evolution> evolutions = new Stack<Evolution>();
        evolutions.add(new Evolution(0, "", "", true));
        if (evolutionsDirectory.exists()) {
            for (File evolution : evolutionsDirectory.getRealFile().listFiles()) {
                if (evolution.getName().matches("^[0-9]+[.]sql$")) {
                    if (Logger.isTraceEnabled()) {
                        Logger.trace("Loading evolution %s", evolution);
                    }

                    int version = Integer.parseInt(evolution.getName().substring(0, evolution.getName().indexOf(".")));
                    String sql = IO.readContentAsString(evolution);
                    StringBuffer sql_up = new StringBuffer();
                    StringBuffer sql_down = new StringBuffer();
                    StringBuffer current = new StringBuffer();
                    for (String line : sql.split("\r?\n")) {
                        if (line.trim().matches("^#.*[!]Ups")) {
                            current = sql_up;
                        } else if (line.trim().matches("^#.*[!]Downs")) {
                            current = sql_down;
                        } else if (line.trim().startsWith("#")) {
                            // skip
                        } else if (!StringUtils.isEmpty(line.trim())) {
                            current.append(line).append("\n");
                        }
                    }
                    evolutions.add(new Evolution(version, sql_up.toString(), sql_down.toString(), true));
                }
            }
            Collections.sort(evolutions);
        }
        return evolutions;
    }

    public synchronized static Stack<Evolution> listDatabaseEvolutions(String moduleKey) {
        Stack<Evolution> evolutions = new Stack<Evolution>();
        evolutions.add(new Evolution(0, "", "", false));
        Connection connection = null;
        try {
            connection = getNewConnection();
            ResultSet rs = connection.getMetaData().getTables(null, null, "play_evolutions", null);
            if (rs.next()) {
                
                checkAndUpdateEvolutionsForMultiModuleSupport(connection);                    

                PreparedStatement statement = connection.prepareStatement("select id, hash, apply_script, revert_script from play_evolutions where module_key = ?");
                statement.setString(1, moduleKey);
                ResultSet databaseEvolutions = statement.executeQuery();
                
                while (databaseEvolutions.next()) {
                    Evolution evolution = new Evolution(databaseEvolutions.getInt(1), databaseEvolutions.getString(3), databaseEvolutions.getString(4), false);
                    evolutions.add(evolution);
                }
            
            } else {
                execute("create table play_evolutions (id int not null, hash varchar(255) not null, applied_at timestamp not null, apply_script text, revert_script text, state varchar(255), last_problem text, module_key varchar(255), constraint pk_id_module_key primary key (id, module_key))");
            }
        } catch (SQLException e) {
            Logger.error(e, "SQL error while checking play evolutions");
        } finally {
            closeConnection(connection);
        }
        Collections.sort(evolutions);
        return evolutions;
    }

    private static void checkAndUpdateEvolutionsForMultiModuleSupport(Connection connection) throws SQLException {
        ResultSet rs = connection.getMetaData().getColumns(null, null, "play_evolutions", "module_key");

        if(!rs.next()) {
            
            System.out.println("!!! - Updating the play_evolutions table to cope with multiple modules - !!!");
            execute("alter table play_evolutions add module_key varchar(255);");
            execute("alter table play_evolutions drop primary key;");
            execute("alter table play_evolutions add constraint pk_id_module_key primary key (id,module_key);");

            System.out.println("!!! - Assigning any existing evolutions to the parent project - !!!");
            PreparedStatement statement = connection.prepareStatement("update play_evolutions set module_key = ? where module_key is null");
            statement.setString(1, Play.configuration.getProperty("application.name"));
            statement.execute();
        }
    }

    public static class Evolution implements Comparable<Evolution> {

        int revision;
        String sql_up;
        String sql_down;
        String hash;
        boolean applyUp;

        public Evolution(int revision, String sql_up, String sql_down, boolean applyUp) {
            this.revision = revision;
            this.sql_down = sql_down;
            this.sql_up = sql_up;
            this.hash = Codec.hexSHA1(sql_up + sql_down);
            this.applyUp = applyUp;
        }

        public int compareTo(Evolution o) {
            return this.revision - o.revision;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof Evolution) && ((Evolution) obj).revision == this.revision;
        }

        @Override
        public int hashCode() {
            return revision;
        }
    }

    // JDBC Utils
    static void execute(String sql) throws SQLException {
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

    static Connection getNewConnection() throws SQLException {
        Connection connection = getDatasource().getConnection();
        connection.setAutoCommit(true); // Yes we want auto-commit
        return connection;
    }

    static void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    // Exceptions
    public static class InvalidDatabaseRevision extends PlayException {

        String evolutionScript;

        public InvalidDatabaseRevision(String evolutionScript) {
            this.evolutionScript = evolutionScript;
        }

        @Override
        public String getErrorTitle() {
            return "Your database needs evolution!";
        }

        @Override
        public String getErrorDescription() {
            return "An SQL script will be run on your database.";
        }

        @Override
        public String getMoreHTML() {
            return "<h3>This SQL script must be run:</h3><pre style=\"background:#fff; border:1px solid #ccc; padding: 5px\">" + evolutionScript + "</pre><form action='/@evolutions/apply' method='POST'><input type='submit' value='Apply evolutions'></form>";
        }
    }

    public static class InconsistentDatabase extends PlayException {

        String evolutionScript;
        String error;
        int revision;

        public InconsistentDatabase(String evolutionScript, String error, int revision) {
            this.evolutionScript = evolutionScript;
            this.error = error;
            this.revision = revision;
        }

        @Override
        public String getErrorTitle() {
            return "Your database is in an inconsistent state!";
        }

        @Override
        public String getErrorDescription() {
            return "An evolution has not been applied properly. Please check the problem and resolve it manually before marking it as resolved.";
        }

        @Override
        public String getMoreHTML() {
            return "<h3>This SQL script has been run, and there was a problem:</h3><pre style=\"background:#fff; border:1px solid #ccc; padding: 5px\">" + evolutionScript + "</pre><h4>This error has been thrown:</h4><pre style=\"background:#fff; border:1px solid #ccc; color: #c00; padding: 5px\">" + error + "</pre><form action='/@evolutions/force/" + revision + "' method='POST'><input type='submit' value='Mark it resolved'></form>";
        }
    }
}
