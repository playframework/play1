package play.db.helper;

public class SqlSelect extends SqlQuery {

    private final Concat select;
    private final Concat from;
    private final Concat join;
    private final Concat where;
    private final Concat groupBy;
    private final Concat orderBy;
    private final Concat limit;

    public SqlSelect() {
        select = new Concat("SELECT ", ", ").defaultValue(null);
        from = new Concat("FROM ", ", ").defaultValue(null);
        join = new Concat(null, null).defaultValue(null);
        where = new Concat("WHERE ", null).defaultValue(null);
        groupBy = new Concat("GROUP BY ", ", ").defaultValue(null);
        orderBy = new Concat("ORDER BY ", ", ").defaultValue(null);
        limit = new Concat("LIMIT ", null);
    }

    @Override public SqlSelect param(Object obj) { super.param(obj); return this; }
    @Override public SqlSelect params(Object ... objs) { super.params(objs); return this; }

    public SqlSelect select(String ... expr) { select.add(expr); return this; }
    public SqlSelect from(String ... expr) { from.add(expr); return this; }
    public SqlSelect innerJoin(String ... expr) { join.prefix("INNER JOIN ").separator(" INNER JOIN ").add(expr); return this; }
    public SqlSelect leftJoin(String ... expr) { join.prefix("LEFT JOIN ").separator(" LEFT JOIN ").add(expr); return this; }
    public SqlSelect where(String ... expr) { return andWhere(expr); }
    public SqlSelect andWhere(String ... expr) { where.separator(" AND ").add(expr); return this; }
    public SqlSelect orWhere(String ... expr) { where.separator(" OR ").add(expr); return this; }
    public SqlSelect groupBy(String ... expr) { groupBy.add(expr); return this; }
    public SqlSelect orderBy(String ... expr) { orderBy.add(expr); return this; }
    public SqlSelect limit(long lines) { limit.append(lines); return this; }
    public SqlSelect limit(long offset, long lines) { limit.append(offset +", "+ lines); return this; }

    @Override
    public String toString() {
        if (select.isEmpty() || from.isEmpty()) throw new IllegalArgumentException();
        return new Concat(""," ").defaultValue(null)
                .append(select)
                .append(from)
                .append(join)
                .append(where)
                .append(groupBy)
                .append(orderBy)
                .append(limit)
                .toString();
    }

}
