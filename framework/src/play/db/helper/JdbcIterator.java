package play.db.helper;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * Iterate over a JDBC ResultSet
 * @author yma
 */
public class JdbcIterator<T> implements Iterator<T>, Iterable<T>, Closeable {

    public static <U> JdbcIterator<U> execute(SqlQuery query, Class<U> resultClass) {
        return execute(query, JdbcResultFactories.build(resultClass));
    }

    public static <U> JdbcIterator<U> execute(SqlQuery query, JdbcResultFactory<U> factory) {
        try {
            return new JdbcIterator<>(JdbcHelper.execute(query), factory);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }


    protected final JdbcResultFactory<T> factory;
    protected ResultSet result;
    private T next;

    public JdbcIterator(ResultSet result, JdbcResultFactory<T> factory) throws SQLException {
        this.factory = factory;
        this.result = result;
        next = null;

        if (this.result != null) this.factory.init(this.result);
    }

    public JdbcIterator(ResultSet result, Class<T> resultClass) throws SQLException {
        this(result, JdbcResultFactories.build(resultClass));
    }

    @Override
    public void close() {
        if (result != null) {
            try {
                next = null;
                result.close();
                result = null;
            } catch (SQLException ex) {
                result = null;
                throw new RuntimeException(ex);
            }
        }
    }

    public static void close(Iterator<?> iterator) {
        if (iterator instanceof JdbcIterator<?>) ((JdbcIterator<?>)iterator).close();
    }


    protected void load() {
        if (next == null && result != null) try {
            if (result.next()) next = factory.create(result);
            else close();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean hasNext() {
        load();
        return next != null;
    }

    @Override
    public T next() {
        load();
        T e = next;
        next = null;
        return e;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

}
