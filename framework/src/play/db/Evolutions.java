package play.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

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

/**
 * Handles migration of data.
 *
 * Does only support the default DBConfig
 */
public class Evolutions extends PlayPlugin {
	

  	public static void main(String[] args) throws SQLException {

        /** Check that evolutions are enabled **/
        if (!evolutionsDirectory.exists()) {
            System.out.println("~ Evolutions are not enabled. Create a db/evolutions directory to create your first 1.sql evolution script.");
            System.out.println("~");
            return;
        }

        /** Start the DB plugin **/
        Play.id = System.getProperty("play.id");
        Play.applicationPath = new File(System.getProperty("application.path"));
        Play.guessFrameworkPath();
        Play.readConfiguration();
        Play.javaPath = new ArrayList<VirtualFile>();
        Play.classes = new ApplicationClasses();
        Play.classloader = new ApplicationClassloader();
        Logger.init();
        Logger.setUp("ERROR");
        new DBPlugin().onApplicationStart();

        /** Connected **/
        System.out.println("~ Connected to " + DB.datasource.getConnection().getMetaData().getURL());

        /** Sumary **/
        Evolution database = listDatabaseEvolutions().peek();
        Evolution application = listApplicationEvolutions().peek();

        if ("resolve".equals(System.getProperty("mode"))) {
            try {
                checkEvolutionsState();
                System.out.println("~");
                System.out.println("~ Nothing to resolve...");
                System.out.println("~");
                return;
            } catch (InconsistentDatabase e) {
                resolve(e.revision);
                System.out.println("~");
                System.out.println("~ Revision " + e.revision + " has been resolved;");
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
            System.out.println("~ Your database is an inconsistent state!");
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

        System.out.print("~ Application revision is " + application.revision + " [" + application.hash.substring(0, 7) + "]");
        System.out.println(" and Database revision is " + database.revision + " [" + database.hash.substring(0, 7) + "]");
        System.out.println("~");

        /** Evolution script **/
        List<Evolution> evolutions = getEvolutionScript();
        if (evolutions.isEmpty()) {
            System.out.println("~ Your database is up to date");
            System.out.println("~");
        } else {

            if ("apply".equals(System.getProperty("mode"))) {

                System.out.println("~ Applying evolutions:");
                System.out.println("");
                System.out.println("# ------------------------------------------------------------------------------");
                System.out.println("");
                System.out.println(toHumanReadableScript(evolutions));
                System.out.println("");
                System.out.println("# ------------------------------------------------------------------------------");
                System.out.println("");
                if (applyScript(true)) {
                    System.out.println("~");
                    System.out.println("~ Evolutions script successfully applied!");
                    System.out.println("~");
                } else {
                    System.out.println("~");
                    System.out.println("~ Can't apply evolutions...");
                    System.out.println("~");
                    System.exit(-1);
                }


            } else if ("markApplied".equals(System.getProperty("mode"))) {

                if (applyScript(false)) {
                    System.out.println("~ Evolutions script marked as applied!");
                    System.out.println("~");
                } else {
                    System.out.println("~ Can't apply evolutions...");
                    System.out.println("~");
                    System.exit(-1);
                }

            } else {

                System.out.println("~ Your database needs evolutions!");
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
            applyScript(true);
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
            if ("mem".equals(Play.configuration.getProperty("db")) && listDatabaseEvolutions().peek().revision == 0) {
                Logger.info("Automatically applying evolutions in in-memory database");
                applyScript(true);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void onApplicationStart() {
        if (!isDisabled() && Play.mode.isProd()) {
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

    /**
     * Checks if evolutions is disabled in application.conf (property "evolutions.enabled")
     */
    private boolean isDisabled() {
        return "false".equals(Play.configuration.getProperty("evolutions.enabled", "true"));
    }
    
    public static synchronized void resolve(int revision) {
        try {
            execute("update play_evolutions set state = 'applied' where state = 'applying_up' and id = " + revision);
            execute("delete from play_evolutions where state = 'applying_down' and id = " + revision);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public static synchronized boolean applyScript(boolean runScript) {
        try {
            Connection connection = getNewConnection();
            int applying = -1;
            try {
                for (Evolution evolution : getEvolutionScript()) {
                    applying = evolution.revision;

                    // Insert into logs
                    if (evolution.applyUp) {
                        PreparedStatement ps = connection.prepareStatement("insert into play_evolutions values(?, ?, ?, ?, ?, ?, ?)");
                        ps.setInt(1, evolution.revision);
                        ps.setString(2, evolution.hash);
                        ps.setDate(3, new Date(System.currentTimeMillis()));
                        ps.setString(4, evolution.sql_up);
                        ps.setString(5, evolution.sql_down);
                        ps.setString(6, "applying_up");
                        ps.setString(7, "");
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
                            connection.createStatement().execute(s);
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
        if (DB.datasource != null && evolutionsDirectory.exists()) {
            List<Evolution> evolutionScript = getEvolutionScript();
            Connection connection = null;
            try {
                connection = getNewConnection();
                ResultSet rs = connection.createStatement().executeQuery("select id, hash, apply_script, revert_script, state, last_problem from play_evolutions where state like 'applying_%'");
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

    public synchronized static List<Evolution> getEvolutionScript() {
        Stack<Evolution> app = listApplicationEvolutions();
        Stack<Evolution> db = listDatabaseEvolutions();
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

    public synchronized static Stack<Evolution> listApplicationEvolutions() {
        Stack<Evolution> evolutions = new Stack<Evolution>();
        evolutions.add(new Evolution(0, "", "", true));
        if (evolutionsDirectory.exists()) {
            for (File evolution : evolutionsDirectory.listFiles()) {
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

    public synchronized static Stack<Evolution> listDatabaseEvolutions() {
        Stack<Evolution> evolutions = new Stack<Evolution>();
        evolutions.add(new Evolution(0, "", "", false));
        Connection connection = null;
        try {
            connection = getNewConnection();
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
                ResultSet databaseEvolutions = connection.createStatement().executeQuery("select id, hash, apply_script, revert_script from play_evolutions");
                while (databaseEvolutions.next()) {
                    Evolution evolution = new Evolution(databaseEvolutions.getInt(1), databaseEvolutions.getString(3), databaseEvolutions.getString(4), false);
                    evolutions.add(evolution);
                }
            } else {
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

                execute("create table play_evolutions (id int not null primary key, hash varchar(255) not null, applied_at timestamp not null, apply_script " + textDataType + ", revert_script " + textDataType + ", state varchar(255), last_problem " + textDataType + ")");
            }
        } catch (SQLException e) {
            Logger.error(e, "SQL error while checking play evolutions");
        } finally {
            closeConnection(connection);
        }
        Collections.sort(evolutions);
        return evolutions;
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
        Connection connection = DB.datasource.getConnection();
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
            return "Your database is an inconsistent state!";
        }

        @Override
        public String getErrorDescription() {
            return "An evolution has not been applied properly. Please check the problem and resolve it manually before making it as resolved.";
        }

        @Override
        public String getMoreHTML() {
            return "<h3>This SQL script has been run, and there was a problem:</h3><pre style=\"background:#fff; border:1px solid #ccc; padding: 5px\">" + evolutionScript + "</pre><h4>This error has been thrown:</h4><pre style=\"background:#fff; border:1px solid #ccc; color: #c00; padding: 5px\">" + error + "</pre><form action='/@evolutions/force/" + revision + "' method='POST'><input type='submit' value='Mark it resolved'></form>";
        }
    }
}
