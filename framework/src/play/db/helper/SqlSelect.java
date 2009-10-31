package play.db.helper;

import java.util.List;

public class SqlSelect extends SqlQuery {

    protected final Concat select;
    protected final Concat from;
    protected final Concat join;
    protected final Concat where;
    protected final Concat groupBy;
    protected final Concat orderBy;
    protected final Concat limit;

    public SqlSelect() {
        select = new Concat("SELECT ", ", ").defaultValue(null);
        from = new Concat("FROM ", ", ").defaultValue(null);
        join = new Concat(null, null).defaultValue(null);
        where = new Concat("WHERE ", null).defaultValue(null);
        groupBy = new Concat("GROUP BY ", ", ").defaultValue(null);
        orderBy = new Concat("ORDER BY ", ", ").defaultValue(null);
        limit = new Concat("LIMIT ", null);
    }

    public SqlSelect(SqlSelect src) {
        select = new Concat(src.select);
        from = new Concat(src.from);
        join = new Concat(src.join);
        where = new Concat(src.where);
        groupBy = new Concat(src.groupBy);
        orderBy = new Concat(src.orderBy);
        limit = new Concat(src.limit);

        params.addAll(src.getParams());
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

    public Where where() { return new Where(this); }
    public SqlSelect where(Where ... expr) { return andWhere(expr); }
    public SqlSelect andWhere(Where ... expr) {
        for (Where subquery : expr) andWhere(subquery.toString());
        return this;
    }
    public SqlSelect orWhere(Where ... expr) {
        for (Where subquery : expr) orWhere(subquery.toString());
        return this;
    }

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

    public static class Where {
        private final SqlSelect parent;
        private final Concat where;

        private Where(SqlSelect parent) {
            this.parent = parent;
            where = new Concat("(", null, ")").defaultValue(null);
        }

        public Where param(Object obj) { parent.param(obj); return this; }
        public Where params(Object ... objs) { parent.params(objs); return this; }
        public List<Object> getParams() { return parent.getParams(); }

        public int paramCurrentIndex() { return parent.paramCurrentIndex(); }
        public String pmark() { return parent.pmark(); }
        public String pmark(int offset) { return parent.pmark(offset); }

        public Where where(String ... expr) { return andWhere(expr); }
        public Where andWhere(String ... expr) { where.separator(" AND ").add(expr); return this; }
        public Where orWhere(String ... expr) { where.separator(" OR ").add(expr); return this; }

        @Override
        public String toString() {
            return where.toString();
        }
    }

}
