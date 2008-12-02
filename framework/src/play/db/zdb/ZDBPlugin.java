package play.db.zdb;

import java.io.File;
import play.PlayPlugin;
import zdb.core.Store;
import play.Play;

/**
 * ZDB Plugin
 */
public class ZDBPlugin extends PlayPlugin {

    @Override
    public void onApplicationStart() {
        if (Play.configuration.getProperty("zdb", "disabled").equals("enabled")) {
            Store.init(new File(Play.applicationPath, "zdb"));
        }
    }
}
