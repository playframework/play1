package play.db.helper;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface JdbcResultFactory<T> {

    public void init(ResultSet result) throws SQLException;

    public T create(ResultSet result) throws SQLException;

}
