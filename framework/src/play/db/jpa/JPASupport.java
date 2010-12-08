package play.db.jpa;

import javax.persistence.Query;

/**
 * Use play.db.jpa.GenericModel insteads
 */
@Deprecated
public class JPASupport extends GenericModel {

    /**
     * Use play.db.jpa.GenericModel.JPAQuery insteads
     */
    @Deprecated
    public static class JPAQuery extends GenericModel.JPAQuery {
        
        public JPAQuery(String sq, Query query) {
            super(sq, query);
        }

        public JPAQuery(Query query) {
            super(query);
        }
    }

}
