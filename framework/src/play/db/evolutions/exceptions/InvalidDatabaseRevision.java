package play.db.evolutions.exceptions;

import play.exceptions.PlayException;

//Exceptions
public class InvalidDatabaseRevision extends PlayException {

    /**
     * The name of the Database concern by the exception
     */
    private String dbName;
    private String evolutionScript;

    public InvalidDatabaseRevision(String dbName, String evolutionScript) {
        this.dbName = dbName;
        this.evolutionScript = evolutionScript;
    }

    public String getDbName() {
        return this.dbName;
    }
    
    public String getEvolutionScript() {
        return evolutionScript;
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
