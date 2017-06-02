package play.db.c3p0;

import com.mchange.v2.c3p0.ConnectionCustomizer;
import play.Logger;
import play.Play;
import play.exceptions.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class PlayConnectionCustomizer implements ConnectionCustomizer {

  public static Map<String, Integer> isolationLevels;

  static {
    isolationLevels = new HashMap<>();
    isolationLevels.put("NONE", Connection.TRANSACTION_NONE);
    isolationLevels.put("READ_UNCOMMITTED", Connection.TRANSACTION_READ_UNCOMMITTED);
    isolationLevels.put("READ_COMMITTED", Connection.TRANSACTION_READ_COMMITTED);
    isolationLevels.put("REPEATABLE_READ", Connection.TRANSACTION_REPEATABLE_READ);
    isolationLevels.put("SERIALIZABLE", Connection.TRANSACTION_SERIALIZABLE);
  }

  @Override
  public void onAcquire(Connection c, String parentDataSourceIdentityToken) {
    Integer isolation = getIsolationLevel();
    if (isolation != null) {
      try {
        Logger.trace("Setting connection isolation level to %s", isolation);
        c.setTransactionIsolation(isolation);
      } catch (SQLException e) {
        throw new DatabaseException("Failed to set isolation level to " + isolation, e);
      }
    }
  }

  @Override
  public void onDestroy(Connection c, String parentDataSourceIdentityToken) {}

  @Override
  public void onCheckOut(Connection c, String parentDataSourceIdentityToken) {}

  @Override
  public void onCheckIn(Connection c, String parentDataSourceIdentityToken) {}

  /**
   * Get the isolation level from either the isolationLevels map, or by
   * parsing into an int.
   */
  private Integer getIsolationLevel() {
    String isolation = Play.configuration.getProperty("db.isolation");
    if (isolation == null) {
      return null;
    }
    Integer level = isolationLevels.get(isolation);
    if (level != null) {
      return level;
    }

    try {
      return Integer.valueOf(isolation);
    } catch (NumberFormatException e) {
      throw new DatabaseException("Invalid isolation level configuration" + isolation, e);
    }
  }
}
