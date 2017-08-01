package play.db;

import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.sql.SQLException;

public interface DataSourceFactory {
  DataSource createDataSource(Configuration dbConfig) throws PropertyVetoException, SQLException;

  String getStatus() throws SQLException;

  String getDriverClass(DataSource ds);
  String getJdbcUrl(DataSource ds);
  String getUser(DataSource ds);
}
