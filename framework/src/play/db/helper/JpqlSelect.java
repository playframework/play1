package play.db.helper;

public class JpqlSelect extends SqlQuery {

    private final Concat select;
    private final Concat from;
    private final Concat where;
    private final Concat groupBy;
    private final Concat orderBy;

    public JpqlSelect() {
        select = new Concat("SELECT ", ", ").defaultValue(null);
        from = new Concat("FROM ", ", ").defaultValue(null);
        where = new Concat("WHERE ", null).defaultValue(null);
        groupBy = new Concat("GROUP BY ", ", ").defaultValue(null);
        orderBy = new Concat("ORDER BY ", ", ").defaultValue(null);
    }

    @Override public JpqlSelect param(Object obj) { super.param(obj); return this; }
    @Override public JpqlSelect params(Object ... objs) { super.params(objs); return this; }

    public JpqlSelect select(String ... expr) { select.add(expr); return this; }
    public JpqlSelect from(String ... expr) { from.add(expr); return this; }
    public JpqlSelect where(String ... expr) { return andWhere(expr); }
    public JpqlSelect andWhere(String ... expr) { where.separator(" AND ").add(expr); return this; }
    public JpqlSelect orWhere(String ... expr) { where.separator(" OR ").add(expr); return this; }
    public JpqlSelect groupBy(String ... expr) { groupBy.add(expr); return this; }
    public JpqlSelect orderBy(String ... expr) { orderBy.add(expr); return this; }

    @Override
    public String toString() {
        if (select.isEmpty() && from.isEmpty()) where.prefix("");
        return new Concat(""," ").defaultValue(null)
                .append(select)
                .append(from)
                .append(where)
                .append(groupBy)
                .append(orderBy)
                .toString();
    }

}
