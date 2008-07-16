package play.i18n;

import java.io.IOException;
import java.util.Properties;
import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.libs.IO;
import play.vfs.VirtualFile;

public class MessagesPlugin extends PlayPlugin {

    static Long lastLoading = 0L;

    @Override
    public void onApplicationStart() {
        Messages.defaults = read(Play.getVirtualFile("conf/messages"));
        if (Messages.defaults == null) {
            Messages.defaults = new Properties();
        }
        for (String locale : Play.langs) {
            Properties properties = read(Play.getVirtualFile("conf/messages." + locale));
            if (properties == null) {
                Logger.warn("conf/messages.%s is missing", locale);
                Messages.locales.put(locale, new Properties());
            } else {
                Messages.locales.put(locale, properties);
            }
        }
        lastLoading = System.currentTimeMillis();
    }

    @Override
    public void beforeInvocation() {
        if (Play.langs.isEmpty()) {
            Lang.set("");
        } else {
            Lang.set(Play.langs.get(0));
        }
    }

    static Properties read(VirtualFile vf) {
        try {
            if (vf.exists()) {
                return IO.readUtf8Properties(vf.inputstream());
            }
            return null;
        } catch (IOException e) {
            Logger.error(e, "Error while loading messages %s", vf.getName());
            return null;
        }
    }

    @Override
    public void detectChange() {
        if (Play.getVirtualFile("conf/messages").exists() && Play.getVirtualFile("conf/messages").lastModified() > lastLoading) {
            onApplicationStart();
            return;
        }
        for (String locale : Play.langs) {
            if (Play.getVirtualFile("conf/messages." + locale).exists() && Play.getVirtualFile("conf/messages." + locale).lastModified() > lastLoading) {
                onApplicationStart();
                return;
            }
        }
    }
}
