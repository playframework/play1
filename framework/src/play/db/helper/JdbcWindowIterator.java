package play.db.helper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcWindowIterator<T> extends JdbcIterator<T> {

    private int pageSize;

    public JdbcWindowIterator(ResultSet result, int pageOffset, int pageSize, JdbcResultFactory<T> factory) throws SQLException {
        super(result, factory);
        this.pageSize = pageSize;
        seek(pageOffset);
    }

    public JdbcWindowIterator(ResultSet result, int pageOffset, int pageSize, Class<T> adapterClass) throws SQLException {
        super(result, adapterClass);
        this.pageSize = pageSize;
        seek(pageOffset);
    }

    private void seek(int pageOffset) throws SQLException {
        if (result != null) {
            if (pageOffset < 0) {
                pageSize += pageOffset;
                pageOffset = 0;
            }
            if (pageSize > 0) {
                if (pageOffset == 0) result.beforeFirst();
                else result.absolute(pageOffset);
            } else close();
        }
    }

    @Override
    protected void load() {
        if (pageSize-- > 0) super.load();
        else close();
    }

}
