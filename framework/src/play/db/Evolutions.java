package play.db;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloader;
import play.db.evolutions.Evolution;
import play.db.evolutions.EvolutionQuery;
import play.db.evolutions.exceptions.InconsistentDatabase;
import play.db.evolutions.exceptions.InvalidDatabaseRevision;
import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Redirect;
import play.vfs.VirtualFile;

import java.io.File;
import java.sql.Connection;
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



    public static void main(String[] args) throws SQLException {


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
        System.out.println("~ Connected to " + EvolutionQuery.getDatasource().getConnection().getMetaData().getURL());

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
                    resolve(moduleRoot.getKey(), e.getRevision());
                    System.out.println("~");
                    System.out.println("~ Revision " + e.getRevision() + " for " + moduleRoot.getKey() + " has been resolved;");
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
                System.out.println(e.getEvolutionScript());
                System.out.println("");
                System.out.println("~ The following error occured:");
                System.out.println("");
                System.out.println(e.getError());
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
                    System.exit(-1);
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
                
                if(!isModuleEvolutionDisabled(specificModule) && moduleRoot.child("db/evolutions").exists()) {
                    modulesWithEvolutions.put(specificModule, moduleRoot.child("db/evolutions"));
                } else {
                    System.out.println("~ '" + specificModule + "' module doesn't have any evolutions scripts in it or evolutions are disabled.");
	            System.out.println("~");
                    System.exit(-1);
                }
            } else if (Play.configuration.getProperty("application.name").equals(specificModule))  {
                weShouldAddTheMainProject = true;
            } else {
                System.out.println("~ Couldn't find a module with the name '" + specificModule + "'. ");
            }
        }

        if (weShouldAddTheMainProject) {
            addMainProjectToModuleList();
        }
    }

    private static void populateModulesWithEvolutions() {
        /** Check that evolutions are enabled **/
        if(!isModuleEvolutionDisabled()){
            for(Entry<String, VirtualFile> moduleRoot : Play.modules.entrySet()) {
                if(moduleRoot.getValue().child("db/evolutions").exists()) {
                    if(!isModuleEvolutionDisabled(moduleRoot.getKey())){
                        modulesWithEvolutions.put(moduleRoot.getKey(), moduleRoot.getValue().child("db/evolutions"));
                    } else {
                        System.out.println("~ '" + moduleRoot.getKey() + "' module evolutions are disabled.");
                    }
                }
            }
        }else{
            System.out.println("~ Module evolutions are disabled.");
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
        if (Play.mode.isDev() && request.method.equals("POST") && request.url.matches("^/@evolutions/force/[a-zA-Z0-9]+/[0-9]+$")) {            
            int index = request.url.lastIndexOf("/@evolutions/force/") + "/@evolutions/force/".length();
            String moduleKey = request.url.substring(index, request.url.lastIndexOf("/"));
            int revision = Integer.parseInt(request.url.substring(request.url.lastIndexOf("/") + 1));
            resolve(moduleKey, revision);
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
        if(isDisabled() || Play.mode.isProd()) {
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
        if (!isDisabled()) {
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

    /**
     * Checks if evolutions is disabled in application.conf (property "evolutions.enabled")
     */
    private boolean isDisabled() {
        return "false".equals(Play.configuration.getProperty("evolutions.enabled", "true"));
    }
    
    private static boolean isModuleEvolutionDisabled(){
        return "false".equals(Play.configuration.getProperty("modules.evolutions.enabled", "true")); 
    }
    
    private static boolean isModuleEvolutionDisabled(String name){
        return "false".equals(Play.configuration.getProperty(name + ".evolutions.enabled", "true")); 
    }
    
    public static boolean autoCommit() {
        return ! "false".equals(Play.configuration.getProperty("evolutions.autocommit", "true"));
    }
    
    public static synchronized void resolve(int revision) {
        try {
            EvolutionQuery.resolve(revision, Play.configuration.getProperty("application.name"));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }
    
    public static synchronized void resolve(String moduleKey, int revision) {
        try {
            EvolutionQuery.resolve(revision, moduleKey);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public static synchronized boolean applyScript(boolean runScript, String moduleKey, VirtualFile evolutionsDirectory) {
        try {
            Connection connection =  EvolutionQuery.getNewConnection(Evolutions.autoCommit());
            int applying = -1;
            try {
                for (Evolution evolution : getEvolutionScript(moduleKey, evolutionsDirectory)) {
                    applying = evolution.revision;                  
                    EvolutionQuery.apply(connection, runScript, evolution, moduleKey);               
                }
                return true;
            } catch (Exception e) {
                String message = e.getMessage();
                if (e instanceof SQLException) {
                    SQLException ex = (SQLException) e;
                    message += " [ERROR:" + ex.getErrorCode() + ", SQLSTATE:" + ex.getSQLState() + "]";
                }
                
                EvolutionQuery.setProblem(connection, applying, moduleKey, message);
                EvolutionQuery.closeConnection(connection);
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

            if (EvolutionQuery.getDatasource() != null) {
                List<Evolution> evolutionScript = getEvolutionScript(moduleRoot.getKey(), moduleRoot.getValue());
                Connection connection = null;
                try {
                    connection = EvolutionQuery.getNewConnection();   
                    ResultSet rs = EvolutionQuery.getEvolutionsToApply(connection, moduleRoot.getKey());
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
                        throw new InconsistentDatabase(script, error, revision, moduleRoot.getKey());
                    }
                } catch (SQLException e) {
                    throw new UnexpectedException(e);
                } finally {
                    EvolutionQuery.closeConnection(connection);
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
        evolutions.add(new Evolution("", 0, "", "", true));
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
                    evolutions.add(new Evolution(moduleKey, version, sql_up.toString(), sql_down.toString(), true));
                }
            }
            Collections.sort(evolutions);
        }
        return evolutions;
    }

    public synchronized static Stack<Evolution> listDatabaseEvolutions(String moduleKey) {
        Stack<Evolution> evolutions = new Stack<Evolution>();
        evolutions.add(new Evolution("", 0, "", "", false));
        Connection connection = null;
        try {
            connection = EvolutionQuery.getNewConnection();
            String tableName = "play_evolutions";
            boolean tableExists = true;
            ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null);

            if (!rs.next()) {
		
                // Table in lowercase does not exist
                // oracle gives table names in upper case
                tableName = tableName.toUpperCase();
                Logger.trace("Checking " + tableName);
                rs.close();
                rs = connection.getMetaData().getTables(null, null, tableName, null);
                // Does it exist?
                if (!rs.next() ) {
                    // did not find it in uppercase either
                    tableExists = false;
                }
            }

            // Do we have a
            if (tableExists) {
                
                checkAndUpdateEvolutionsForMultiModuleSupport(connection);                    

                ResultSet databaseEvolutions = EvolutionQuery.getEvolutions(connection, moduleKey);
                                
                while (databaseEvolutions.next()) {
                    Evolution evolution = new Evolution(moduleKey, databaseEvolutions.getInt(1), databaseEvolutions.getString(3), databaseEvolutions.getString(4), false);
                    evolutions.add(evolution);
                }
            
            } else {
                EvolutionQuery.createTable();
            }
        } catch (SQLException e) {
            Logger.error(e, "SQL error while checking play evolutions");
        } finally {
            EvolutionQuery.closeConnection(connection);
        }
        Collections.sort(evolutions);
        return evolutions;
    }

    private static void checkAndUpdateEvolutionsForMultiModuleSupport(Connection connection) throws SQLException {
        ResultSet rs = connection.getMetaData().getColumns(null, null, "play_evolutions", "module_key");
        if(!rs.next()) {       
             System.out.println("!!! - Updating the play_evolutions table to cope with multiple modules - !!!");      
             EvolutionQuery.alterForModuleSupport(connection); 
        }
    }

}
