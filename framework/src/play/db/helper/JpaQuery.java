package play.db.helper;

public class JpaQuery extends SqlQuery {

    private final Concat where;
    private final Concat orderBy;

    public JpaQuery() {
        where = new Concat("", null).defaultValue(null);
        orderBy = new Concat("ORDER BY ", ", ").defaultValue(null);
    }

    @Override public JpaQuery param(Object obj) { super.param(obj); return this; }
    @Override public JpaQuery params(Object ... objs) { super.params(objs); return this; }

    public JpaQuery where(String ... expr) { return andWhere(expr); }
    public JpaQuery andWhere(String ... expr) { where.separator(" AND ").add(expr); return this; }
    public JpaQuery orWhere(String ... expr) { where.separator(" OR ").add(expr); return this; }
    public JpaQuery orderBy(String ... expr) { orderBy.add(expr); return this; }

    @Override
    public String toString() {
        if (where.isEmpty()) throw new IllegalArgumentException();
        return new Concat(""," ").defaultValue(null)
                .append(where)
                .append(orderBy)
                .toString();
    }

}
