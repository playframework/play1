package play.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.libs.IO;
import play.vfs.VirtualFile;

/**
 * Messages plugin
 */
public class MessagesPlugin extends PlayPlugin {

    static Long lastLoading = 0L;

    @Override
    public void onApplicationStart() {
        Messages.defaults = new Properties();
        try {
            FileInputStream is = new FileInputStream(new File(Play.frameworkPath, "resources/messages"));
            Messages.defaults.putAll(IO.readUtf8Properties(is));
        } catch(Exception e) {
            Logger.warn("Defaults messsages file missing");
        }
        for(VirtualFile module : Play.modules.values()) {
            VirtualFile messages = module.child("conf/messages");
            if(messages != null && messages.exists()) {
                Messages.defaults.putAll(read(messages)); 
            }
        }
        VirtualFile appDM = Play.getVirtualFile("conf/messages");
        if(appDM != null && appDM.exists()) {
            Messages.defaults.putAll(read(appDM));
        }
        for (String locale : Play.langs) {
            Properties properties = new Properties();
            for(VirtualFile module : Play.modules.values()) {
                VirtualFile messages = module.child("conf/messages." + locale);
                if(messages != null && messages.exists()) {
                    properties.putAll(read(messages)); 
                }
            }
            VirtualFile appM = Play.getVirtualFile("conf/messages." + locale);
            if(appM != null && appM.exists()) {
                properties.putAll(read(appM));
            } else {
                Logger.warn("Messages file missing for locale %s", locale);
            }     
            Messages.locales.put(locale, properties);
        }
        lastLoading = System.currentTimeMillis();
    }

    static Properties read(VirtualFile vf) {
        if (vf != null) {
            return IO.readUtf8Properties(vf.inputstream());
        }
        return null;
    }

    @Override
    public void detectChange() {
        if (Play.getVirtualFile("conf/messages")!=null && Play.getVirtualFile("conf/messages").lastModified() > lastLoading) {
            onApplicationStart();
            return;
        }
        for(VirtualFile module : Play.modules.values()) {
            if(module.child("conf/messages") != null && module.child("conf/messages").exists() && module.child("conf/messages").lastModified() > lastLoading) {
                onApplicationStart();
                return;
            }
        }
        for (String locale : Play.langs) {
            if (Play.getVirtualFile("conf/messages." + locale)!=null && Play.getVirtualFile("conf/messages." + locale).lastModified() > lastLoading) {
                onApplicationStart();
                return;
            }
            for(VirtualFile module : Play.modules.values()) {
                if(module.child("conf/messages."+locale) != null && module.child("conf/messages."+locale).exists() && module.child("conf/messages."+locale).lastModified() > lastLoading) {
                    onApplicationStart();
                    return;
                }
            }
        }

    }
}
