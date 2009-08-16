package play.modules.gae;

import play.db.jpa.JPQLDialect;

public class DataNucleusDialect extends JPQLDialect {
    
    @SuppressWarnings("unused")
    public String createFindByQuery(String entityName, String entityClass, String query, Object... params) {
        if (query == null) {
            return "select from " + entityName;
        }
        if (query.trim().toLowerCase().startsWith("select ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return "select " + query;
        }
        if (query.trim().indexOf(" ") == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(" ") == -1 && params == null) {
            query += " = null";
        }
        return "select from " + entityName + " where " + query;
    }
    
}
