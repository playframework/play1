package play.db;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.classloading.ApplicationClassloader;
import play.db.evolutions.Evolution;
import play.db.evolutions.EvolutionQuery;
import play.db.evolutions.EvolutionState;
import play.db.evolutions.exceptions.InconsistentDatabase;
import play.db.evolutions.exceptions.InvalidDatabaseRevision;
import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Redirect;
import play.vfs.VirtualFile;

/**
 * Handles migration of data.
 *
 * Does only support the default DBConfig
 */
public class Evolutions extends PlayPlugin {

    private static String EVOLUTIONS_TABLE_NAME = "play_evolutions";
    protected static File evolutionsDirectory = Play.getFile("db/evolutions");

    private static Map<String, VirtualFile> modulesWithEvolutions = new LinkedHashMap<>();

    public static void main(String[] args) throws SQLException {
        /** Start the DB plugin **/
        Play.guessFrameworkPath();
        Play.readConfiguration();
        Play.classes = new ApplicationClasses();
        Play.classloader = new ApplicationClassloader();

        Play.loadModules(VirtualFile.open(Play.applicationPath));

        if (System.getProperty("modules") != null) {
            populateModulesWithSpecificModules();
        } else {
            populateModulesWithEvolutions();
        }

        if (modulesWithEvolutions.isEmpty()) {
            System.out.println("~ Nothing has evolutions, go away and think again.");
            System.exit(-1);
            return;
        }

        Logger.init();
        Logger.setUp("ERROR");
        new DBPlugin().onApplicationStart();

        // Look over all the DB
        Set<String> dBNames = Configuration.getDbNames();
        boolean defaultExitCode = true;

        for (String dbName : dBNames) {
            Configuration dbConfig = new Configuration(dbName);
            /** Connected **/
            System.out.println("~ Connected to " + DB.getDataSource(dbName).getConnection().getMetaData().getURL());

            for (Entry<String, VirtualFile> moduleRoot : modulesWithEvolutions.entrySet()) {

                /** Summary **/
                Evolution database = listDatabaseEvolutions(dbName, moduleRoot.getKey()).peek();
                Evolution application = listApplicationEvolutions(dbName, moduleRoot.getKey(), moduleRoot.getValue()).peek();

                boolean needToCheck = true;
                if ("resolve".equals(System.getProperty("mode"))) {
                    needToCheck = handleResolveAction(dbName, moduleRoot);
                }
                if (needToCheck) {
                    /** Check inconsistency **/
                    try {
                        checkEvolutionsState(dbName);
                    } catch (InconsistentDatabase e) {
                        defaultExitCode = false;
                        System.out.println("~");
                        System.out.println("~ Your database " + dbName + " is in an inconsistent state!");
                        System.out.println("~");
                        System.out.println("~ While applying this script part:");
                        System.out.println("");
                        System.out.println(e.getEvolutionScript());
                        System.out.println("");
                        System.out.println("~ The following error occurred:");
                        System.out.println("");
                        System.out.println(e.getError());
                        System.out.println("");
                        System.out.println("~ Please correct it manually, and mark it resolved by running `play evolutions:resolve`");
                        System.out.println("~");
                        continue;
                    } catch (InvalidDatabaseRevision e) {
                        // see later
                    }

                    System.out.print("~ '" + moduleRoot.getKey() + "' Application revision is " + application.revision + " ["
                            + application.hash.substring(0, 7) + "]");
                    System.out.println(" and '" + moduleRoot.getKey() + "' Database revision is " + database.revision + " ["
                            + database.hash.substring(0, 7) + "]");
                    System.out.println("~");

                    /** Evolution script **/
                    List<Evolution> evolutions = getEvolutionScript(dbName, moduleRoot.getKey(), moduleRoot.getValue());
                    if (evolutions.isEmpty()) {
                        System.out.println("~ Your database " + dbName + " is up to date for " + moduleRoot.getKey());
                        System.out.println("~");
                    } else {
                        if ("apply".equals(System.getProperty("mode"))) {
                            if (!handleApplyAction(dbName, moduleRoot, evolutions)) {
                                defaultExitCode = false;
                            }
                        } else if ("markApplied".equals(System.getProperty("mode"))) {
                            if (!handleMarkAppliedAction(dbName, moduleRoot, evolutions)) {
                                defaultExitCode = false;
                            }
                        } else {
                            defaultExitCode = false;
                            handleDefaultAction(dbName, moduleRoot, evolutions);
                        }
                    }
                }
            }
        }

        if (!defaultExitCode) {
            System.exit(-1);
        }
    }

    /**
     * Method to handle the "default" action
     * 
     * @param dbName
     *            database name
     * @param moduleRoot
     *            the module root of evolutions
     * @param evolutions
     *            list of evolutions
     */
    private static void handleDefaultAction(String dbName, Entry<String, VirtualFile> moduleRoot, List<Evolution> evolutions) {
        System.out.println("~ Your database " + dbName + " needs evolutions for " + moduleRoot.getKey() + "!");
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

    /**
     * Method to handle the "resolve" action
     * 
     * @param dbName
     *            database name
     * @param moduleRoot
     *            the module root of evolutions
     * @return true if need to check, false otherwise
     */
    private static boolean handleResolveAction(String dbName, Entry<String, VirtualFile> moduleRoot) {
        try {
            checkEvolutionsState(dbName);
            System.out.println("~");
            System.out.println("~ Nothing to resolve for " + moduleRoot.getKey() + "...");
            System.out.println("~");
            return false;
        } catch (InconsistentDatabase e) {
            resolve(dbName, moduleRoot.getKey(), e.getRevision());
            System.out.println("~");
            System.out.println("~ Revision " + e.getRevision() + " for " + moduleRoot.getKey() + " has been resolved;");
            System.out.println("~");
        } catch (InvalidDatabaseRevision e) {
            // see later
        }
        return true;
    }

    /**
     * Method to handle the "apply" action
     * 
     * @param dbName
     *            database name
     * @param moduleRoot
     *            the module root of evolutions
     * @param evolutions
     *            list of evolutions
     * @return true if action was applied successfully, false otherwise
     */
    private static boolean handleApplyAction(String dbName, Entry<String, VirtualFile> moduleRoot, List<Evolution> evolutions) {
        System.out.println("~ Applying evolutions for " + moduleRoot.getKey() + ":");
        System.out.println("");
        System.out.println("# ------------------------------------------------------------------------------");
        System.out.println("");
        System.out.println(toHumanReadableScript(evolutions));
        System.out.println("");
        System.out.println("# ------------------------------------------------------------------------------");
        System.out.println("");
        if (applyScript(dbName, true, moduleRoot.getKey(), moduleRoot.getValue())) {
            System.out.println("~");
            System.out.println("~ Evolutions script successfully applied for " + moduleRoot.getKey() + "!");
            System.out.println("~");
            return true;
        } else {
            System.out.println("~");
            System.out.println("~ Can't apply evolutions for " + moduleRoot.getKey() + "...");
            System.out.println("~");
            return false;
        }
    }

    /**
     * Method to handle the "markApplied" action
     * 
     * @param dbName
     *            database name
     * @param moduleRoot
     *            the module root of evolutions
     * @param evolutions
     *            list of evolutions
     * @return true if action was applied successfully, false otherwise
     */
    private static boolean handleMarkAppliedAction(String dbName, Entry<String, VirtualFile> moduleRoot, List<Evolution> evolutions) {
        if (applyScript(dbName, false, moduleRoot.getKey(), moduleRoot.getValue())) {
            System.out.println("~ Evolutions script marked as applied for " + moduleRoot.getKey() + "!");
            System.out.println("~");
            return true;
        } else {
            System.out.println("~ Can't apply evolutions for " + moduleRoot.getKey() + "...");
            System.out.println("~");
            return false;
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

                if (!isModuleEvolutionDisabled(specificModule) && moduleRoot.child("db/evolutions").exists()) {
                    modulesWithEvolutions.put(specificModule, moduleRoot.child("db/evolutions"));
                } else {
                    System.out.println(
                            "~ '" + specificModule + "' module doesn't have any evolutions scripts in it or evolutions are disabled.");
                    System.out.println("~");
                    System.exit(-1);
                }
            } else if (Play.configuration.getProperty("application.name").equals(specificModule)) {
                weShouldAddTheMainProject = true;
            } else {
                System.out.println("~ Couldn't find a module with the name '" + specificModule + "'. ");
                System.exit(-1);
            }
        }

        if (weShouldAddTheMainProject) {
            addMainProjectToModuleList();
        }
    }

    private static void populateModulesWithEvolutions() {
        /** Check that evolutions are enabled **/
        if (!isModuleEvolutionDisabled()) {
            for (Entry<String, VirtualFile> moduleRoot : Play.modules.entrySet()) {
                if (moduleRoot.getValue().child("db/evolutions").exists()) {
                    if (!isModuleEvolutionDisabled(moduleRoot.getKey())) {
                        modulesWithEvolutions.put(moduleRoot.getKey(), moduleRoot.getValue().child("db/evolutions"));
                    } else {
                        System.out.println("~ '" + moduleRoot.getKey() + "' module evolutions are disabled.");
                    }
                }
            }
        } else {
            System.out.println("~ Module evolutions are disabled.");
        }

        addMainProjectToModuleList();
    }

    private static void addMainProjectToModuleList() {
        if (evolutionsDirectory.exists()) {
            modulesWithEvolutions.put(Play.configuration.getProperty("application.name"), VirtualFile.open(evolutionsDirectory));
        }
    }

    @Override
    public boolean rawInvocation(Request request, Response response) throws Exception {

        // Mark an evolution as resolved
        if (Play.mode.isDev() && request.method.equals("POST") && request.url.matches("^/@evolutions/force/[a-zA-Z0-9]+/[0-9]+$")) {
            int index = request.url.lastIndexOf("/@evolutions/force/") + "/@evolutions/force/".length();

            String dbName = DB.DEFAULT;
            String moduleKey = request.url.substring(index, request.url.lastIndexOf("/"));
            int revision = Integer.parseInt(request.url.substring(request.url.lastIndexOf("/") + 1));

            resolve(dbName, moduleKey, revision);
            new Redirect("/").apply(request, response);
            return true;
        }

        // Apply the current evolution script
        if (Play.mode.isDev() && request.method.equals("POST") && request.url.equals("/@evolutions/apply")) {

            for (Entry<String, VirtualFile> moduleRoot : modulesWithEvolutions.entrySet()) {
                applyScript(true, moduleRoot.getKey(), moduleRoot.getValue());
            }
            new Redirect("/").apply(request, response);
            return true;
        }
        return super.rawInvocation(request, response);
    }

    @Override
    public void beforeInvocation() {
        if (isDisabled() || Play.mode.isProd()) {
            return;
        }
        try {
            checkEvolutionsState();
        } catch (InvalidDatabaseRevision e) {
            Set<String> dbNames = Configuration.getDbNames();
            for (String dbName : dbNames) {
                Configuration dbConfig = new Configuration(dbName);

                for (Entry<String, VirtualFile> moduleRoot : modulesWithEvolutions.entrySet()) {
                    if ("mem".equals(dbConfig.getProperty("db"))
                            && listDatabaseEvolutions(e.getDbName(), moduleRoot.getKey()).peek().revision == 0) {
                        Logger.info("Automatically applying evolutions in in-memory database");
                        Logger.info("Applying evolutions for '" + moduleRoot.getKey() + "'");
                        applyScript(true, moduleRoot.getKey(), moduleRoot.getValue());
                    } else {
                        throw e;
                    }
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

    private static boolean isModuleEvolutionDisabled() {
        return "false".equals(Play.configuration.getProperty("modules.evolutions.enabled", "true"));
    }

    private static boolean isModuleEvolutionDisabled(String name) {
        return "false".equals(Play.configuration.getProperty(name + ".evolutions.enabled", "true"));
    }

    public static boolean autoCommit() {
        return !"false".equals(Play.configuration.getProperty("evolutions.autocommit", "true"));
    }

    public static synchronized void resolve(int revision) {
        try {
            EvolutionQuery.resolve(DB.DEFAULT, revision, Play.configuration.getProperty("application.name"));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public static synchronized void resolve(String dBName, int revision) {
        try {
            EvolutionQuery.resolve(dBName, revision, Play.configuration.getProperty("application.name"));
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public static synchronized void resolve(String dBName, String moduleKey, int revision) {
        try {
            EvolutionQuery.resolve(dBName, revision, moduleKey);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public static synchronized boolean applyScript(boolean runScript, String moduleKey, VirtualFile evolutionsDirectory) {
        // Look over all the DB
        Set<String> dBNames = Configuration.getDbNames();
        for (String dbName : dBNames) {
            return applyScript(dbName, runScript, moduleKey, evolutionsDirectory);
        }
        return true;
    }

    public static synchronized boolean applyScript(String dbName, boolean runScript, String moduleKey, VirtualFile evolutionsDirectory) {
        try {
            Connection connection = EvolutionQuery.getNewConnection(dbName, Evolutions.autoCommit());
            int applying = -1;
            try {
                List<Evolution> evolutions = getEvolutionScript(dbName, moduleKey, evolutionsDirectory);
                for (Evolution evolution : evolutions) {
                    applying = evolution.revision;
                    EvolutionQuery.apply(connection, runScript, evolution, moduleKey);
                }

                if (!Evolutions.autoCommit()) {
                    connection.commit();
                }

                return true;
            } catch (Exception e) {
                Logger.error(e, "Can't apply evolution");
                if (Evolutions.autoCommit()) {
                    String message = e.getMessage();
                    if (e instanceof SQLException) {
                        SQLException ex = (SQLException) e;
                        message += " [ERROR:" + ex.getErrorCode() + ", SQLSTATE:" + ex.getSQLState() + "]";
                    }

                    EvolutionQuery.setProblem(connection, applying, moduleKey, message);
                } else {
                    connection.rollback();
                }
                return false;
            } finally {
                EvolutionQuery.closeConnection(connection);
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
            sql.append("# --- Rev:").append(evolution.revision).append(",").append(evolution.applyUp ? "Ups" : "Downs").append(" - ")
                    .append(evolution.hash.substring(0, 7)).append("\n");
            sql.append("\n");
            sql.append(evolution.applyUp ? evolution.sql_up : evolution.sql_down);
            sql.append("\n\n");
        }

        if (containsDown) {
            sql.insert(0, "# !!! WARNING! This script contains DOWNS evolutions that are likely destructive\n\n");
        }

        return sql.toString().trim();
    }

    public static synchronized void checkEvolutionsState() {
        // Look over all the DB
        Set<String> dBNames = Configuration.getDbNames();
        for (String dbName : dBNames) {
            checkEvolutionsState(dbName);
        }
    }

    public static synchronized void checkEvolutionsState(String dbName) {
        for (Entry<String, VirtualFile> moduleRoot : modulesWithEvolutions.entrySet()) {

            if (DB.getDataSource(dbName) != null) {
                List<Evolution> evolutionScript = getEvolutionScript(dbName, moduleRoot.getKey(), moduleRoot.getValue());
                Connection connection = null;
                ResultSet resultSet = null;
                try {
                    connection = EvolutionQuery.getNewConnection(dbName);
                    resultSet = EvolutionQuery.getEvolutionsToApply(connection, moduleRoot.getKey());
                    if (resultSet.next()) {
                        int revision = resultSet.getInt("id");
                        String state = resultSet.getString("state");
                        String hash = resultSet.getString("hash").substring(0, 7);
                        String script = "";
                        if (EvolutionState.APPLYING_UP.getStateWord().equals(state)) {
                            script = resultSet.getString("apply_script");
                        } else {
                            script = resultSet.getString("revert_script");
                        }
                        script = "# --- Rev:" + revision + "," + (EvolutionState.APPLYING_UP.getStateWord().equals(state) ? "Ups" : "Downs")
                                + " - " + hash + "\n\n" + script;
                        String error = resultSet.getString("last_problem");
                        throw new InconsistentDatabase(dbName, script, error, revision, moduleRoot.getKey());
                    }
                } catch (SQLException e) {
                    throw new UnexpectedException(e);
                } finally {
                    EvolutionQuery.closeResultSet(resultSet);
                    EvolutionQuery.closeConnection(connection);
                }

                if (!evolutionScript.isEmpty()) {
                    throw new InvalidDatabaseRevision(dbName, toHumanReadableScript(evolutionScript));
                }
            }
        }
    }

    public static synchronized List<Evolution> getEvolutionScript(String dbName, String moduleKey, VirtualFile evolutionsDirectory) {
        Stack<Evolution> app = listApplicationEvolutions(dbName, moduleKey, evolutionsDirectory);
        Stack<Evolution> db = listDatabaseEvolutions(dbName, moduleKey);
        List<Evolution> downs = new ArrayList<>();
        List<Evolution> ups = new ArrayList<>();

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

        List<Evolution> script = new ArrayList<>();
        script.addAll(downs);
        script.addAll(ups);

        return script;
    }

    public static synchronized Stack<Evolution> listApplicationEvolutions(String dBName, String moduleKey,
            VirtualFile evolutionsDirectory) {
        Stack<Evolution> evolutions = new Stack<>();
        evolutions.add(new Evolution("", 0, "", "", true));
        if (evolutionsDirectory.exists()) {
            for (File evolution : evolutionsDirectory.getRealFile().listFiles()) {
                if (evolution.getName().matches("^" + dBName + ".[0-9]+[.]sql$")
                        || (DB.DEFAULT.equals(dBName) && evolution.getName().matches("^[0-9]+[.]sql$"))) {
                    if (Logger.isTraceEnabled()) {
                        Logger.trace("Loading evolution %s", evolution);
                    }

                    int version = 0;
                    if (evolution.getName().contains(dBName)) {
                        version = Integer.parseInt(
                                evolution.getName().substring(evolution.getName().indexOf(".") + 1, evolution.getName().lastIndexOf(".")));
                    } else {
                        version = Integer.parseInt(evolution.getName().substring(0, evolution.getName().indexOf(".")));
                    }

                    String sql = IO.readContentAsString(evolution);
                    StringBuilder sql_up = new StringBuilder();
                    StringBuilder sql_down = new StringBuilder();
                    StringBuilder current = new StringBuilder();
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

    private static boolean isEvolutionsTableExist(Connection connection) {
        String tableName = EVOLUTIONS_TABLE_NAME;
        ResultSet resultSet = null;
        try {
            resultSet = connection.getMetaData().getTables(null, null, tableName, null);
            if (!resultSet.next()) {
                // Table in lowercase does not exist
                // oracle gives table names in upper case
                tableName = tableName.toUpperCase();
                Logger.trace("Checking " + tableName);
                resultSet.close();
                resultSet = connection.getMetaData().getTables(null, null, tableName, null);
                // Does it exist?
                if (!resultSet.next()) {
                    // did not find it in uppercase either
                    return false;
                }
            }
        } catch (SQLException e) {
            Logger.error(e, "SQL error while checking if play evolutions exist");
        } finally {
            EvolutionQuery.closeResultSet(resultSet);
        }
        return true;
    }

    public static synchronized Stack<Evolution> listDatabaseEvolutions(String dbName, String moduleKey) {
        Stack<Evolution> evolutions = new Stack<>();
        evolutions.add(new Evolution("", 0, "", "", false));
        Connection connection = null;
        try {
            connection = EvolutionQuery.getNewConnection(dbName);
            // Do we have a
            if (isEvolutionsTableExist(connection)) {
                checkAndUpdateEvolutionsForMultiModuleSupport(dbName, connection);

                ResultSet databaseEvolutions = EvolutionQuery.getEvolutions(connection, moduleKey);

                while (databaseEvolutions.next()) {
                    Evolution evolution = new Evolution(moduleKey, databaseEvolutions.getInt(1), databaseEvolutions.getString(3),
                            databaseEvolutions.getString(4), false);
                    evolutions.add(evolution);
                }

            } else {
                EvolutionQuery.createTable(dbName);
            }
        } catch (SQLException e) {
            Logger.error(e, "SQL error while checking play evolutions");
        } finally {
            EvolutionQuery.closeConnection(connection);
        }
        Collections.sort(evolutions);
        return evolutions;
    }

    private static void checkAndUpdateEvolutionsForMultiModuleSupport(String dbName, Connection connection) throws SQLException {
        ResultSet rs = connection.getMetaData().getColumns(null, null, "play_evolutions", "module_key");
        if (!rs.next()) {
            System.out.println("!!! - Updating the play_evolutions table to cope with multiple modules - !!!");
            EvolutionQuery.alterForModuleSupport(dbName, connection);
        }
    }

}
