package play.i18n;

import java.util.Properties;

/**
 * Used to prepare Messages (static) to work while testing
 */
public class MessagesBuilder {


    public Properties defaults = new Properties();

    public void build() {
        Messages.defaults = defaults;
    }
}
