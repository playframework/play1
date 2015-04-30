package play.db.helper;

import java.util.List;
import javax.persistence.Query;
import play.db.jpa.JPA;

public class JpaHelper {

    private JpaHelper() {
    }
    
    public static Query execute(String sql, Object ... params) {
        return execute(JPA.DEFAULT, sql, params);
    }

    public static Query execute(String dbName, String sql, Object ... params) {
        Query query = JPA.em(dbName).createQuery(sql);
        int index = 0;
        for (Object param : params) {
            query.setParameter(++index, param);
        }
        return query;
    }

    public static Query executeList(String sql, List<Object> params) {
        return executeList(JPA.DEFAULT, sql, params);
    }
    
    public static Query executeList(String dbName, String sql, List<Object> params) {
        Query query = JPA.em(dbName).createQuery(sql);
        int index = 0;
        for (Object param : params) {
            query.setParameter(++index, param);
        }
        return query;
    }

    public static Query execute(SqlQuery query) {
        return execute(JPA.DEFAULT, query);
    }
    
    public static Query execute(String dbName, SqlQuery query) {
        return executeList(dbName, query.toString(), query.getParams());
    }

}
