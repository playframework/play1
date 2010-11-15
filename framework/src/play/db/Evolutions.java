package play.db;

import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.libs.Codec;
import play.libs.IO;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Redirect;

public class Evolutions extends PlayPlugin {

    static File evolutionsDirectory = Play.getFile("db/evolutions");

    @Override
    public boolean rawInvocation(Request request, Response response) throws Exception {

        // Mark an evolution as resolved
        if (Play.mode.isDev() && request.method.equals("POST") && request.url.matches("^/@evolutions/force/[0-9]+$")) {
            int revision = Integer.parseInt(request.url.substring(request.url.lastIndexOf("/") + 1));
            execute("update play_evolutions set state = 'applied' where state = 'applying_up' and id = " +revision);
            execute("delete from play_evolutions where state = 'applying_down' and id = " +revision);
            new Redirect("/").apply(request, response);
            return true;
        }

        // Apply the current evolution script
        if (Play.mode.isDev() && request.method.equals("POST") && request.url.equals("/@evolutions/apply")) {
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
                    for (String sql : (evolution.applyUp ? evolution.sql_up : evolution.sql_down).split(";")) {
                        if (sql.trim().isEmpty()) {
                            continue;
                        }
                        execute(sql);
                    }
                    // Insert into logs
                    if (evolution.applyUp) {
                        execute("update play_evolutions set state = 'applied' where id = " + evolution.revision);
                    } else {
                        execute("delete from play_evolutions where id = " + evolution.revision);
                    }
                }

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
            }
            new Redirect("/").apply(request, response);
            return true;
        }
        return super.rawInvocation(request, response);
    }

    @Override
    public void beforeInvocation() {
        checkEvolutionsState();
    }

    @Override
    public void onApplicationStart() {
        if (Play.mode.isProd()) {
            try {
                checkEvolutionsState();
            } catch (InvalidDatabaseRevision e) {
                Logger.warn("*** Your database needs evolution! You must run this script on your database: \n\n" + toHumanReadableScript(getEvolutionScript()) + "\n\n");
                throw e;
            }
        }
    }

    public String toHumanReadableScript(List<Evolution> evolutionScript) {
        // Construct the script
        StringBuilder sql = new StringBuilder();
        boolean containsDown = false;
        for (Evolution evolution : evolutionScript) {
            if(!evolution.applyUp) {
                containsDown = true;
            }
            sql.append("# --- Rev:").append(evolution.revision).append(",").append(evolution.applyUp ? "Ups" : "Downs").append(" - ").append(evolution.hash.substring(0, 7)).append("\n");
            sql.append("\n");
            sql.append(evolution.applyUp ? evolution.sql_up : evolution.sql_down);
            sql.append("\n\n");
        }

        if(containsDown) {
            sql.insert(0, "# !!! WARNING! This script contains DOWNS evolutions that are likely destructives\n\n");
        }

        return sql.toString().trim();
    }

    public void checkEvolutionsState() {
        if (DB.datasource != null && evolutionsDirectory.exists()) {
            List<Evolution> evolutionScript = getEvolutionScript();
            Connection connection = null;
            try {
                connection = getNewConnection();
                ResultSet rs = connection.createStatement().executeQuery("select id, hash, apply_script, revert_script, state, last_problem from play_evolutions where state like 'applying_%'");
                if(rs.next()) {
                    int revision = rs.getInt("id");
                    String state= rs.getString("state");
                    String hash = rs.getString("hash").substring(0, 7);
                    String script = "";
                    if(state.equals("applying_up")) {
                        script = rs.getString("apply_script");
                    } else {
                        script = rs.getString("revert_script");
                    }
                    script = "# --- Rev:"+revision + "," + (state.equals("applying_up") ? "Ups" : "Downs") + " - " + hash + "\n\n" +script;
                    String error = rs.getString("last_problem");
                    throw new InconsistentDatabase(script, error, revision);
                }
            } catch(SQLException e) {
                throw new UnexpectedException(e);
            } finally {
                closeConnection(connection);
            }

            if (!evolutionScript.isEmpty()) {
                throw new InvalidDatabaseRevision(toHumanReadableScript(evolutionScript));
            }
        }
    }

    public static List<Evolution> getEvolutionScript() {
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

    public static Stack<Evolution> listApplicationEvolutions() {
        Stack<Evolution> evolutions = new Stack<Evolution>();
        evolutions.add(new Evolution(0, "", "", true));
        if (evolutionsDirectory.exists()) {
            for (File evolution : evolutionsDirectory.listFiles()) {
                if (evolution.getName().matches("^[0-9]+[.]sql$")) {
                    Logger.trace("Loading evolution %s", evolution);
                    int version = Integer.parseInt(evolution.getName().substring(0, evolution.getName().indexOf(".")));
                    String sql = IO.readContentAsString(evolution);
                    StringBuffer sql_up = new StringBuffer();
                    StringBuffer sql_down = new StringBuffer();
                    StringBuffer current = new StringBuffer();
                    for (String line : sql.split("\n")) {
                        if (line.matches("^#.*[!]Ups")) {
                            current = sql_up;
                        } else if (line.matches("^#.*[!]Downs")) {
                            current = sql_down;
                        } else if (line.startsWith("#")) {
                            // skip
                        } else if (!line.trim().isEmpty()) {
                            current.append(line.trim() + "\n");
                        }
                    }
                    evolutions.add(new Evolution(version, sql_up.toString(), sql_down.toString(), true));
                }
            }
            Collections.sort(evolutions);
        }
        return evolutions;
    }

    public static Stack<Evolution> listDatabaseEvolutions() {
        Stack<Evolution> evolutions = new Stack<Evolution>();
        evolutions.add(new Evolution(0, "", "", false));
        Connection connection = null;
        try {
            connection = getNewConnection();
            ResultSet rs = connection.getMetaData().getTables(null, null, "play_evolutions", null);
            if (rs.next()) {
                ResultSet databaseEvolutions = connection.createStatement().executeQuery("select id, hash, apply_script, revert_script from play_evolutions");
                while (databaseEvolutions.next()) {
                    Evolution evolution = new Evolution(databaseEvolutions.getInt(1), databaseEvolutions.getString(3), databaseEvolutions.getString(4), false);
                    evolutions.add(evolution);
                }
            } else {
                execute("create table play_evolutions (id int not null primary key, hash varchar(255) not null, applied_at timestamp not null, apply_script text, revert_script text, state varchar(255), last_problem text)");
            }
        } catch (SQLException e) {
            Logger.error(e, "SQL error while checking play evolutions");
        } finally {
            closeConnection(connection);
        }
        Collections.sort(evolutions);
        return evolutions;
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
        } catch(SQLException e) {
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
            if(connection != null) connection.close();
        } catch(Exception e) {
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
            return "An SQL script will be run on your database. Please check the generated script before applying it.";
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
            return "<h3>This SQL script has been run, and there was a problem:</h3><pre style=\"background:#fff; border:1px solid #ccc; padding: 5px\">" + evolutionScript + "</pre><h4>This error has been thrown:</h4><pre style=\"background:#fff; border:1px solid #ccc; color: #c00; padding: 5px\">" + error + "</pre><form action='/@evolutions/force/"+revision+"' method='POST'><input type='submit' value='Mark it resolved'></form>";
        }
    }
}
