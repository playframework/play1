package play.db.jpa;

import javax.persistence.Query;

public class JPQLDialect {

    @SuppressWarnings("unused")
    public String createFindByQuery(String entityName, String entityClass, String query, Object... params) {
        if (query == null) {
            return "from " + entityName;
        }
        if (query.matches("^by[A-Z].*$")) {
            return "from " + entityName + " where " + findByToJPQL(query);
        }
        if (query.trim().toLowerCase().startsWith("select ")) {
            return query;
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return query;
        }
        if (query.trim().indexOf(" ") == -1 && query.trim().indexOf("=") == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(" ") == -1 && query.trim().indexOf("=") == -1 && params == null) {
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
        if (query.trim().indexOf(" ") == -1 && query.trim().indexOf("=") == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(" ") == -1 && query.trim().indexOf("=") == -1 && params == null) {
            query += " = null";
        }
        return "delete from " + entityName + " where " + query;
    }

    @SuppressWarnings("unused")
    public String createCountQuery(String entityName, String entityClass, String query, Object... params) {
        if (query.trim().toLowerCase().startsWith("select ")) {
            return query;
        }
        if (query.matches("^by[A-Z].*$")) {
            return "select count(*) from " + entityName + " where " + findByToJPQL(query);
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return "select count(*) " + query;
        }
        if (query.trim().indexOf(" ") == -1 && query.trim().indexOf("=") == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(" ") == -1 && query.trim().indexOf("=") == -1 && params == null) {
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

    public String findByToJPQL(String findBy) {
        findBy = findBy.substring(2);
        StringBuffer jpql = new StringBuffer();
        String[] parts = findBy.split("And");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.endsWith("Equal")) {
                String prop = extractProp(part, "Equal");
                jpql.append(prop + " = ?");
            } else if (part.endsWith("IsNotNull")) {
                String prop = extractProp(part, "IsNotNull");
                jpql.append(prop + " is not null");
            } else if (part.endsWith("IsNull")) {
                String prop = extractProp(part, "IsNull");
                jpql.append(prop + " is null");
            } else if (part.endsWith("Between")) {
                String prop = extractProp(part, "Between");
                jpql.append(prop + " < ? AND " + prop + " > ?");
            } else if (part.endsWith("Like")) {
                String prop = extractProp(part, "Like");
                jpql.append("LOWER(" + prop + ") like ?");
            } else {
                String prop = extractProp(part, "");
                jpql.append(prop + " = ?");
            }
            if (i < parts.length - 1) {
                jpql.append(" AND ");
            }
        }
        return jpql.toString();
    }

    protected static String extractProp(String part, String end) {
        String prop = part.substring(0, part.length() - end.length());
        prop = (prop.charAt(0) + "").toLowerCase() + prop.substring(1);
        return prop;
    }
    public static JPQLDialect instance = null;
}
