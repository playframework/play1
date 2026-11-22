package play.db.helper;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface JdbcResultFactory<T> {

    void init(ResultSet result) throws SQLException;

    T create(ResultSet result) throws SQLException;

}
