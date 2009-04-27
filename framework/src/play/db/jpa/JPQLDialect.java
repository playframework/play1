package play.db.jpa;

import javax.persistence.Query;

public class JPQLDialect {
    
    @SuppressWarnings("unused")
    public String createFindByQuery(String entityName, String entityClass, String query, Object... params) {
        if (query == null) {
            return "from " + entityName;
        }
        if (query.trim().toLowerCase().startsWith("select ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return query;
        }
        if (query.trim().indexOf(" ") == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(" ") == -1 && params == null) {
            query += " = null";
        }
        return "from " + entityName + " where " + query;
    }

    @SuppressWarnings("unused")
    public String createDeleteQuery(String entityName, String entityClass, String query, Object... params) {
        if (query == null) {
            return "delete from " + entityName;
        }
        if (query.trim().toLowerCase().startsWith("delete ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return "delete " + query;
        }
        if (query.trim().indexOf(" ") == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(" ") == -1 && params == null) {
            query += " = null";
        }
        return "delete from " + entityName + " where " + query;
    }

    @SuppressWarnings("unused")
    public String createCountQuery(String entityName, String entityClass, String query, Object... params) {
        if (query.trim().toLowerCase().startsWith("select ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return "select count(*) " + query;
        }
        if (query.trim().indexOf(" ") == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(" ") == -1 && params == null) {
            query += " = null";
        }
        return "select count(e) from " + entityName + " e where " + query;
    }

    @SuppressWarnings("unused")
    public Query bindParameters(Query q, Object... params) {
        if (params == null) {
            return q;
        }
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i + 1, params[i]);
        }
        return q;
    }
    
    public static JPQLDialect instance = null;
}
