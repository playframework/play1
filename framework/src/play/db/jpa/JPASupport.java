package play.db.jpa;

import javax.persistence.Query;

/**
 * Use play.db.jpa.GenericModel instead
 */
@Deprecated
public class JPASupport extends GenericModel {

    /**
     * Use play.db.jpa.GenericModel.JPAQuery instead
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
