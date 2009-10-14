package play.db.helper;

public class JpqlSelect extends SqlSelect {

    public JpqlSelect() {
        super();
    }

    public JpqlSelect(JpqlSelect src) {
        super(src);
    }

    @Override
    public String toString() {
        if (!join.isEmpty()) throw new IllegalArgumentException();
        if (!limit.isEmpty()) throw new IllegalArgumentException();

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
