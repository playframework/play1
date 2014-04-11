package play.db.evolutions.exceptions;

import play.exceptions.PlayException;

//Exceptions
public class InconsistentDatabase extends PlayException {
    /**
     * The name of the Database concern by the exception
     */
    private String dbName;
    private String evolutionScript;
    private String error;
    private int revision;
    private String moduleKey;

    public InconsistentDatabase(String dbName, String evolutionScript, String error, int revision, String moduleKey) {
        this.dbName = dbName;
        this.evolutionScript = evolutionScript;
        this.error = error;
        this.revision = revision;
        this.moduleKey = moduleKey;
    }
    
    public String getDbName() {
        return this.dbName;
    }
    
    public String getEvolutionScript() {
        return this.evolutionScript;
    }

    public String getError() {
        return this.error;
    }

    public int getRevision() {
        return this.revision;
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
        return "<h3>This SQL script has been run, and there was a problem:</h3><pre style=\"background:#fff; border:1px solid #ccc; padding: 5px\">" + evolutionScript + "</pre><h4>This error has been thrown:</h4><pre style=\"background:#fff; border:1px solid #ccc; color: #c00; padding: 5px\">" + error + "</pre><form action='/@evolutions/force/" + moduleKey + "/" + revision + "' method='POST'><input type='submit' value='Mark it resolved'></form>";
    }
}
