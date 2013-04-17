package play.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    private static List<String> includeMessageFilenames = new ArrayList<String>();

    @Override
    public void onApplicationStart() {
	includeMessageFilenames.clear();
        Messages.defaults = new Properties();
        try {
            FileInputStream is = new FileInputStream(new File(Play.frameworkPath, "resources/messages"));
            Messages.defaults.putAll(IO.readUtf8Properties(is));
        } catch(Exception e) {
            Logger.warn("Defaults messsages file missing");
        }
        for(VirtualFile module : Play.modules.values()) {
            VirtualFile messages = module.child("conf/messages");
            if(messages != null && messages.exists() && !messages.isDirectory()) {
                Messages.defaults.putAll(read(messages)); 
            }
        }
        VirtualFile appDM = Play.getVirtualFile("conf/messages");
        if(appDM != null && appDM.exists() && !appDM.isDirectory()) {
            Messages.defaults.putAll(read(appDM));
        }
        for (String locale : Play.langs) {
            Properties properties = new Properties();
            for(VirtualFile module : Play.modules.values()) {
                VirtualFile messages = module.child("conf/messages." + locale);
                if(messages != null && messages.exists()  && !messages.isDirectory()) {
                    properties.putAll(read(messages)); 
                }
            }
            VirtualFile appM = Play.getVirtualFile("conf/messages." + locale);
            if(appM != null && appM.exists()  && !appM.isDirectory()) {
                properties.putAll(read(appM));
            } else {
                Logger.warn("Messages file missing for locale %s", locale);
            }     
            Messages.locales.put(locale, properties);
        }
        lastLoading = System.currentTimeMillis();
    }

    static Properties read(VirtualFile vf) {
	Properties propsFromFile=null;
        if (vf != null && !vf.isDirectory()) {
            propsFromFile = IO.readUtf8Properties(vf.inputstream());
            
            // Include
            Map<Object, Object> toInclude = new HashMap<Object, Object>(16);
            for (Object key : propsFromFile.keySet()) {
                if (key.toString().startsWith("@include.")) {
                    try {
                        String filenameToInclude = propsFromFile.getProperty(key.toString());
                        VirtualFile fileToInclude = Play.getVirtualFile("conf/" + filenameToInclude);

                        if(fileToInclude != null && fileToInclude.exists() && !fileToInclude.isDirectory()) {
                            toInclude.putAll( read(fileToInclude) );
                            if(!includeMessageFilenames.contains(filenameToInclude)){
                        	includeMessageFilenames.add(filenameToInclude);
                            }
                        }
                    } catch (Exception ex) {
                        Logger.warn("Missing include: %s", key);
                    }
                }
            }
            propsFromFile.putAll(toInclude);
        }
        return propsFromFile;
    }

    @Override
    public void detectChange() {
	VirtualFile vf = Play.getVirtualFile("conf/messages");
        if (vf != null && vf.exists() && !vf.isDirectory() && vf.lastModified() > lastLoading) {
            onApplicationStart();
            return;
        }
        for(VirtualFile module : Play.modules.values()) {
            vf = module.child("conf/messages");
            if(vf != null && vf.exists() && !vf.isDirectory() && vf.lastModified() > lastLoading) {
                onApplicationStart();
                return;
            }
        }
        for (String locale : Play.langs) {
            vf = Play.getVirtualFile("conf/messages." + locale);
            if (vf != null && vf.exists() && !vf.isDirectory() && vf.lastModified() > lastLoading) {
                onApplicationStart();
                return;
            }
            for(VirtualFile module : Play.modules.values()) {
                vf = module.child("conf/messages."+locale);
                if( vf != null && vf.exists() && !vf.isDirectory() && vf.lastModified() > lastLoading) {
                    onApplicationStart();
                    return;
                }
            }
        }
        
        for (String includeFilename : includeMessageFilenames) {
            vf = Play.getVirtualFile("conf/" + includeFilename);
            if (vf != null && vf.exists() && !vf.isDirectory() && vf.lastModified() > lastLoading) {
                onApplicationStart();
                return;
            }
        }
    }
}
