package play.db.jpa;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import play.Play;
import play.db.jpa.GenericModel.JPAQuery;
import play.mvc.Scope.Params;

public class JPQL {

    public EntityManager em() {
        return JPA.em();
    }

    public long count(String entity) {
        return Long.parseLong(em().createQuery("select count(*) from " + entity).getSingleResult().toString());
    }

    public long count(String entity, String query, Object[] params) {
        return Long.parseLong(
                bindParameters(em().createQuery(
                createCountQuery(entity, entity, query, params)), params).getSingleResult().toString());
    }

    public List findAll(String entity) {
        return em().createQuery("select e from " + entity + " e").getResultList();
    }

    public JPABase findById(String entity, Object id) throws Exception {
        return (JPABase) em().find(Play.classloader.loadClass(entity), id);
    }

    public List findBy(String entity, String query, Object[] params) {
        Query q = em().createQuery(
                createFindByQuery(entity, entity, query, params));
        return bindParameters(q, params).getResultList();
    }

    public JPAQuery find(String entity, String query, Object[] params) {
        Query q = em().createQuery(
                createFindByQuery(entity, entity, query, params));
        return new JPAQuery(
                createFindByQuery(entity, entity, query, params), bindParameters(q, params));
    }

    public JPAQuery find(String entity) {
        Query q = em().createQuery(
                createFindByQuery(entity, entity, null));
        return new JPAQuery(
                createFindByQuery(entity, entity, null), bindParameters(q));
    }

    public JPAQuery all(String entity) {
        Query q = em().createQuery(
                createFindByQuery(entity, entity, null));
        return new JPAQuery(
                createFindByQuery(entity, entity, null), bindParameters(q));
    }

    public int delete(String entity, String query, Object[] params) {
        Query q = em().createQuery(
                createDeleteQuery(entity, entity, query, params));
        return bindParameters(q, params).executeUpdate();
    }

    public int deleteAll(String entity) {
        Query q = em().createQuery(
                createDeleteQuery(entity, entity, null));
        return bindParameters(q).executeUpdate();
    }

    public JPABase findOneBy(String entity, String query, Object[] params) {
        Query q = em().createQuery(
                createFindByQuery(entity, entity, query, params));
        List results = bindParameters(q, params).getResultList();
        if (results.size() == 0) {
            return null;
        }
        return (JPABase) results.get(0);
    }

    public JPABase create(String entity, String name, Params params) throws Exception {
        Object o = Play.classloader.loadClass(entity).newInstance();
        return ((GenericModel) o).edit(name, params.all());
    }

    public String createFindByQuery(String entityName, String entityClass, String query, Object... params) {
        if (query == null || query.trim().length() == 0) {
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
        if (query.trim().toLowerCase().startsWith("order by ")) {
            return "from " + entityName + " " + query;
        }
        if (query.trim().indexOf(" ") == -1 && query.trim().indexOf("=") == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(" ") == -1 && query.trim().indexOf("=") == -1 && params == null) {
            query += " = null";
        }
        return "from " + entityName + " where " + query;
    }

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
        if (query.trim().toLowerCase().startsWith("order by ")) {
            return "select count(*) from " + entityName;
        }
        if (query.trim().indexOf(" ") == -1 && query.trim().indexOf("=") == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(" ") == -1 && query.trim().indexOf("=") == -1 && params == null) {
            query += " = null";
        }
        if (query.trim().length() == 0) {
            return "select count(*) from " + entityName;
        }
        return "select count(*) from " + entityName + " where " + query;
    }

    @SuppressWarnings("unchecked")
    public Query bindParameters(Query q, Object... params) {
        if (params == null) {
            return q;
        }
        if (params.length == 1 && params[0] instanceof Map) {
            return bindParameters(q, (Map<String, Object>) params[0]);
        }
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i + 1, params[i]);
        }
        return q;
    }

    public Query bindParameters(Query q, Map<String,Object> params) {
        if (params == null) {
            return q;
        }
        for (String key : params.keySet()) {
            q.setParameter(key, params.get(key));
        }
        return q;
    }

    public String findByToJPQL(String findBy) {
        findBy = findBy.substring(2);
        StringBuffer jpql = new StringBuffer();
        String[] parts = findBy.split("And");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.endsWith("NotEqual")) {
                String prop = extractProp(part, "NotEqual");
                jpql.append(prop + " <> ?");
            } else if (part.endsWith("Equal")) {
                String prop = extractProp(part, "Equal");
                jpql.append(prop + " = ?");
            } else if (part.endsWith("IsNotNull")) {
                String prop = extractProp(part, "IsNotNull");
                jpql.append(prop + " is not null");
            } else if (part.endsWith("IsNull")) {
                String prop = extractProp(part, "IsNull");
                jpql.append(prop + " is null");
            } else if (part.endsWith("LessThan")) {
                String prop = extractProp(part, "LessThan");
                jpql.append(prop + " < ?");
            } else if (part.endsWith("LessThanEquals")) {
                String prop = extractProp(part, "LessThanEquals");
                jpql.append(prop + " <= ?");
            } else if (part.endsWith("GreaterThan")) {
                String prop = extractProp(part, "GreaterThan");
                jpql.append(prop + " > ?");
            } else if (part.endsWith("GreaterThanEquals")) {
                String prop = extractProp(part, "GreaterThanEquals");
                jpql.append(prop + " >= ?");
            } else if (part.endsWith("Between")) {
                String prop = extractProp(part, "Between");
                jpql.append(prop + " < ? AND " + prop + " > ?");
            } else if (part.endsWith("Like")) {
                String prop = extractProp(part, "Like");
                jpql.append("LOWER(" + prop + ") like ?");
            } else if (part.endsWith("Ilike")) {
                String prop = extractProp(part, "Ilike");
                jpql.append("LOWER(" + prop + ") like LOWER(?)");
            } else if (part.endsWith("Elike")) {
                String prop = extractProp(part, "Ilike");
                jpql.append(prop + " like LOWER(?)");
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
    public static JPQL instance = null;
}
