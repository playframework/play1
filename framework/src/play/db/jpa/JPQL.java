package play.db.jpa;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import play.Play;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;
import play.db.Configuration;
import play.db.jpa.GenericModel.JPAQuery;
import play.mvc.Scope.Params;

public class JPQL {

    public EntityManager em(String dbName) {
        return JPA.em(dbName);
    }

     public EntityManager em() {
        return JPA.em(JPA.DEFAULT);
    }

     public long count(String entity) {
        return count(JPA.DEFAULT, entity);
    }

    public long count(String dbName, String entity) {
        return Long.parseLong(em(dbName).createQuery("select count(*) from " + entity + " e").getSingleResult().toString());
    }

    public long count(String entity, String query, Object[] params) {
        return count(JPA.DEFAULT, entity, query, params);
    }

    public long count(String dbName, String entity, String query, Object[] params) {
        return Long.parseLong(
                bindParameters(em(dbName).createQuery(
                createCountQuery(dbName, entity, entity, query, params)), params).getSingleResult().toString());
    }

    public <T extends JPABase> List<T> findAll(String entity) {
        return findAll(JPA.DEFAULT, entity);
    }

     public <T extends JPABase> List<T> findAll(String dbName, String entity) {
        return em(dbName).createQuery("select e from " + entity + " e").getResultList();
    }

    public JPABase findById(String entity, Object id) throws Exception {
        return findById(JPA.DEFAULT, entity, id);
    }

    public JPABase findById(String dbName, String entity, Object id) throws Exception {
        return (JPABase) em(dbName).find(Play.classloader.loadClass(entity), id);
    }

    public <T extends JPABase> List<T> findBy(String entity, String query, Object[] params) {
       return findBy(JPA.DEFAULT, entity, query, params);
    }

    public <T extends JPABase> List<T> findBy(String dbName, String entity, String query, Object[] params) {
        Query q = em(dbName).createQuery(
                createFindByQuery(dbName, entity, entity, query, params));
        return bindParameters(q, params).getResultList();
    }

    public JPAQuery find(String entity, String query, Object[] params) {
      return find(JPA.DEFAULT, entity, query, params);
    }


    public JPAQuery find(String dbName, String entity, String query, Object[] params) {
        Query q = em(dbName).createQuery(
                createFindByQuery(dbName, entity, entity, query, params));
        return new JPAQuery(
                createFindByQuery(dbName, entity, entity, query, params), bindParameters(q, params));
    }

    public JPAQuery find(String entity) {
        return find(JPA.DEFAULT, entity);
    }

    public JPAQuery find(String dbName, String entity) {
        Query q = em(dbName).createQuery(
                createFindByQuery(dbName, entity, entity, null));
        return new JPAQuery(
                createFindByQuery(dbName, entity, entity, null), bindParameters(q));
    }

    public JPAQuery all(String entity) {
        return all(JPA.DEFAULT, entity);
    }

    public JPAQuery all(String dbName, String entity) {
        Query q = em(dbName).createQuery(
                createFindByQuery(dbName, entity, entity, null));
        return new JPAQuery(
                createFindByQuery(dbName, entity, entity, null), bindParameters(q));
    }

    public int delete(String dbName, String entity, String query, Object[] params) {
        Query q = em(dbName).createQuery(
                createDeleteQuery(entity, entity, query, params));
        return bindParameters(q, params).executeUpdate();
    }

    public int delete(String entity, String query, Object[] params) {
       return delete(JPA.DEFAULT, entity, query, params);
    }


    public int deleteAll(String dbName, String entity) {
        Query q = em(dbName).createQuery(
                createDeleteQuery(entity, entity, null));
        return bindParameters(q).executeUpdate();
    }

    public int deleteAll(String entity) {
       return deleteAll(JPA.DEFAULT, entity);
    }

    public JPABase findOneBy(String dbName, String entity, String query, Object[] params) {
        Query q = em(dbName).createQuery(
                createFindByQuery(dbName, entity, entity, query, params));
        List results = bindParameters(q, params).getResultList();
        if (results.size() == 0) {
            return null;
        }
        return (JPABase) results.get(0);
    }

    public JPABase findOneBy(String entity, String query, Object[] params) {
       return findOneBy(JPA.DEFAULT, entity, query, params);
    }

    public JPABase create(String entity, String name, Params params) throws Exception {
        return create(JPA.DEFAULT, entity, name, params);
    }

    public JPABase create(String dbName, String entity, String name, Params params) throws Exception {
        Object o = Play.classloader.loadClass(entity).newInstance();

        RootParamNode rootParamNode = ParamNode.convert(params.all());

        return ((GenericModel) o).edit(rootParamNode, name);
    }

    public String createFindByQuery(String dbName, String entityName, String entityClass, String query, Object... params) {
        if (query == null || query.trim().length() == 0) {
            return "from " + entityName;
        }
        if (query.matches("^by[A-Z].*$")) {
            return "from " + entityName + " where " + findByToJPQL(dbName, query);
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
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params == null) {
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
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params == null) {
            query += " = null";
        }
        return "delete from " + entityName + " where " + query;
    }

    public String createCountQuery(String dbName, String entityName, String entityClass, String query, Object... params) {
        if (query.trim().toLowerCase().startsWith("select ")) {
            return query;
        }
        if (query.matches("^by[A-Z].*$")) {
            return "select count(*) from " + entityName + " where " + findByToJPQL(dbName, query);
        }
        if (query.trim().toLowerCase().startsWith("from ")) {
            return "select count(*) " + query;
        }
        if (query.trim().toLowerCase().startsWith("order by ")) {
            return "select count(*) from " + entityName;
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params != null && params.length == 1) {
            query += " = ?1";
        }
        if (query.trim().indexOf(' ') == -1 && query.trim().indexOf('=') == -1 && params == null) {
            query += " = null";
        }
        if (query.trim().length() == 0) {
            return "select count(*) from " + entityName;
        }
        return "select count(*) from " + entityName + " e where " + query;
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
        return findByToJPQL(JPA.DEFAULT, findBy);
    }

    public String findByToJPQL(String dbName, String findBy) {
        findBy = findBy.substring(2);
        StringBuilder jpql = new StringBuilder();
        String subRequest;
        if (findBy.contains("OrderBy"))
            subRequest = findBy.split("OrderBy")[0];
        else subRequest = findBy;
        String[] parts = subRequest.split("And");
        int index = 1;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.endsWith("NotEqual")) {
                String prop = extractProp(part, "NotEqual");
                jpql.append(prop).append(" <> ?").append(index++);
            } else if (part.endsWith("Equal")) {
                String prop = extractProp(part, "Equal");
                jpql.append(prop).append(" = ?").append(index++);
            } else if (part.endsWith("IsNotNull")) {
                String prop = extractProp(part, "IsNotNull");
                jpql.append(prop).append(" is not null");
            } else if (part.endsWith("IsNull")) {
                String prop = extractProp(part, "IsNull");
                jpql.append(prop).append(" is null");
            } else if (part.endsWith("LessThan")) {
                String prop = extractProp(part, "LessThan");
                jpql.append(prop).append(" < ?").append(index++);
            } else if (part.endsWith("LessThanEquals")) {
                String prop = extractProp(part, "LessThanEquals");
                jpql.append(prop).append(" <= ?").append(index++);
            } else if (part.endsWith("GreaterThan")) {
                String prop = extractProp(part, "GreaterThan");
                jpql.append(prop).append(" > ?").append(index++);
            } else if (part.endsWith("GreaterThanEquals")) {
                String prop = extractProp(part, "GreaterThanEquals");
                jpql.append(prop).append(" >= ?").append(index++);
            } else if (part.endsWith("Between")) {
                String prop = extractProp(part, "Between");
                jpql.append(prop).append(" < ?").append(index++).append(" AND ").append(prop).append(" > ?").append(index++);
            } else if (part.endsWith("Like")) {
                String prop = extractProp(part, "Like");
                // HSQL -> LCASE, all other dbs lower
                if (this.isHSQL(dbName)) {
                    jpql.append("LCASE(").append(prop).append(") like ?").append(index++);
                } else {
                    jpql.append("LOWER(").append(prop).append(") like ?").append(index++);
                }
            } else if (part.endsWith("Ilike")) {
                String prop = extractProp(part, "Ilike");
                 if (this.isHSQL(dbName)) {
                    jpql.append("LCASE(").append(prop).append(") like LCASE(?").append(index++).append(")");
                 } else {
                    jpql.append("LOWER(").append(prop).append(") like LOWER(?").append(index++).append(")");
                 }
            } else if (part.endsWith("Elike")) {
                String prop = extractProp(part, "Elike");
                jpql.append(prop).append(" like ?").append(index++);
            } else {
                String prop = extractProp(part, "");
                jpql.append(prop).append(" = ?").append(index++);
            }
            if (i < parts.length - 1) {
                jpql.append(" AND ");
            }
        }
        return jpql.toString();
    }

    private boolean isHSQL(String dbName) {
        Configuration dbConfig = new Configuration(dbName);
        String db = dbConfig.getProperty("db");
        return ("mem".equals(db) || "fs".equals(db) || "org.hsqldb.jdbcDriver".equals(dbConfig.getProperty("db.driver")));
    }

    protected static String extractProp(String part, String end) {
        String prop = part.substring(0, part.length() - end.length());
        prop = (prop.charAt(0) + "").toLowerCase() + prop.substring(1);
        return prop;
    }
    public static JPQL instance = null;
}
