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
        if (Play.mode.isDev() && request.method.equals("POST") && request.url.equals("/@evolutions/apply")) {
            Connection connection = DB.datasource.getConnection();
            try {
                for (Evolution evolution : getEvolutionScript()) {
                    // Insert into logs
                    if (evolution.applyUp) {
                        PreparedStatement ps = connection.prepareStatement("insert into play_evolutions values(?, ?, ?, ?, ?, ?)");
                        ps.setInt(1, evolution.revision);
                        ps.setString(2, evolution.hash);
                        ps.setDate(3, new Date(System.currentTimeMillis()));
                        ps.setString(4, evolution.sql_up);
                        ps.setString(5, evolution.sql_down);
                        ps.setString(6, "applying_up");
                        ps.execute();
                    } else {
                        connection.createStatement().execute("update play_evolutions set state = 'applying_down' where id = " + evolution.revision);
                    }
                    // Execute script
                    for (String sql : (evolution.applyUp ? evolution.sql_up : evolution.sql_down).split(";")) {
                        if (sql.trim().isEmpty()) {
                            continue;
                        }
                        connection.createStatement().execute(sql);
                    }
                    // Insert into logs
                    if (evolution.applyUp) {
                        connection.createStatement().execute("update play_evolutions set state = 'applied' where id = " + evolution.revision);
                    } else {
                        connection.createStatement().execute("delete from play_evolutions where id = " + evolution.revision);
                    }
                }
                new Redirect("/").apply(request, response);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                throw new UnexpectedException(e);
            }
        }
        return super.rawInvocation(request, response);
    }

    @Override
    public void beforeInvocation() {
        if (DB.datasource != null) {
            List<Evolution> evolutionScript = getEvolutionScript();
            if (!evolutionScript.isEmpty()) {
                // Construct the script
                StringBuilder sql = new StringBuilder();
                for (Evolution evolution : evolutionScript) {
                    sql.append("# --- Rev:").append(evolution.revision).append(",").append(evolution.applyUp ? "UPS" : "DOWNS").append(" - ").append(evolution.hash.substring(0, 7)).append("\n");
                    sql.append("\n");
                    sql.append(evolution.applyUp ? evolution.sql_up : evolution.sql_down);
                    sql.append("\n\n");
                }
                throw new InvalidDatabaseRevision(sql.toString());
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
        try {
            ResultSet rs = DB.getConnection().getMetaData().getTables(null, null, "play_evolutions", null);
            if (rs.next()) {
                ResultSet databaseEvolutions = DB.executeQuery("select id, hash, apply_script, revert_script from play_evolutions");
                while (databaseEvolutions.next()) {
                    Evolution evolution = new Evolution(databaseEvolutions.getInt(1), databaseEvolutions.getString(3), databaseEvolutions.getString(4), false);
                    evolutions.add(evolution);
                }
            } else {
                DB.execute("create table play_evolutions (id int not null primary key, hash varchar(255) not null, applied_at timestamp not null, apply_script longtext, revert_script longtext, state varchar(255))");
            }
        } catch (SQLException e) {
            Logger.error(e, "SQL error while checking play evolutions");
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
}
