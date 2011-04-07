package play.db.helper;

import java.util.List;
import javax.persistence.Query;

import play.db.DBConfig;
import play.db.jpa.JPA;
import play.db.jpa.JPAConfig;

public class JpaHelper {

    private static JpaHelperInstance defaultJpaHelper = new JpaHelperInstance(DBConfig.defaultDbConfigName);

    public static JpaHelperInstance withConfig(String jpaConfigName) {
        return new JpaHelperInstance( jpaConfigName);
    }

    public static Query execute(String sql, Object ... params) {
        return defaultJpaHelper.execute(sql, params);
    }

    public static Query executeList(String sql, List<Object> params) {
        return defaultJpaHelper.executeList(sql, params);
    }

    public static Query execute(SqlQuery query) {
        return defaultJpaHelper.execute(query);
    }

    public static class JpaHelperInstance {
        private final JPAConfig jpaConfig;

        private JpaHelperInstance(String jpaConfigName) {
            jpaConfig = JPA.getJPAConfig(jpaConfigName);
        }

        public Query execute(String sql, Object ... params) {
            Query query = jpaConfig.getJPAContext().em().createQuery(sql);
            int index = 0;
            for (Object param : params) {
                query.setParameter(++index, param);
            }
            return query;
        }

        public Query executeList(String sql, List<Object> params) {
            Query query = jpaConfig.getJPAContext().em().createQuery(sql);
            int index = 0;
            for (Object param : params) {
                query.setParameter(++index, param);
            }
            return query;
        }

        public Query execute(SqlQuery query) {
            return executeList(query.toString(), query.getParams());
        }


    }

}
