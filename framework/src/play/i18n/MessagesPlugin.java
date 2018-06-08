package play.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.vfs.VirtualFile;

/**
 * Messages plugin
 */
public class MessagesPlugin extends PlayPlugin {

    static Long lastLoading = 0L;

    private static List<String> includeMessageFilenames = new ArrayList<>();

    @Override
    public void onApplicationStart() {
        includeMessageFilenames.clear();
        Messages.defaults = new Properties();
        try {
            File message = new File(Play.frameworkPath, "resources/messages");
            Messages.defaults.putAll(read(message));
        } catch (Exception e) {
            Logger.warn("Default messages file missing: %s", e);
        }
        for (VirtualFile module : Play.modules.values()) {
            VirtualFile messages = module.child("conf/messages");
            if (messages != null && messages.exists()
                    && !messages.isDirectory()) {
                Messages.defaults.putAll(read(messages));
            }
        }
        VirtualFile appDM = Play.getVirtualFile("conf/messages");
        if (appDM != null && appDM.exists() && !appDM.isDirectory()) {
            Messages.defaults.putAll(read(appDM));
        }
        for (String locale : Play.langs) {
            Properties properties = new Properties();
            for (VirtualFile module : Play.modules.values()) {
                VirtualFile messages = module.child("conf/messages." + locale);
                if (messages != null && messages.exists()
                        && !messages.isDirectory()) {
                    properties.putAll(read(messages));
                }
            }
            VirtualFile appM = Play.getVirtualFile("conf/messages." + locale);
            if (appM != null && appM.exists() && !appM.isDirectory()) {
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
            return read(vf.getRealFile());
        }
        return null;
    }

    static Properties read(File file) {
        Properties propsFromFile = null;
        if (file != null && !file.isDirectory()) {
            InputStream inStream = null;
            try {
                inStream = new FileInputStream(file);
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
            propsFromFile = IO.readUtf8Properties(inStream);

            // Include
            Map<Object, Object> toInclude = new HashMap<>(16);
            for (Object key : propsFromFile.keySet()) {
                if (key.toString().startsWith("@include.")) {
                    try {
                        String filenameToInclude = propsFromFile.getProperty(key.toString());
                        File fileToInclude = getIncludeFile(file, filenameToInclude);
                        if (fileToInclude != null && fileToInclude.exists() && !fileToInclude.isDirectory()) {
                            // Check if the file was not previously read
                            if (!includeMessageFilenames.contains(fileToInclude.getAbsolutePath())) {
                                toInclude.putAll(read(fileToInclude));
                                includeMessageFilenames.add(fileToInclude.getAbsolutePath());
                            }
                        } else {
                            Logger.warn("Missing include: %s from file %s", filenameToInclude, file.getPath());
                        }
                    } catch (Exception ex) {
                        Logger.warn("Missing include: %s, caused by: %s", key, ex);
                    }
                }
            }
            propsFromFile.putAll(toInclude);
        }
        return propsFromFile;
    }

    private static File getIncludeFile(File file, String filenameToInclude) {
        if (file != null && filenameToInclude != null && !filenameToInclude.isEmpty()) {
            // Test absolute path
            File fileToInclude = new File(filenameToInclude);
            if (fileToInclude.isAbsolute()) {
                return fileToInclude;
            } else {
                return new File(file.getParent(), filenameToInclude); 
            }
        }
        return null;
    }

    @Override
    public void detectChange() {
        VirtualFile vf = Play.getVirtualFile("conf/messages");
        if (vf != null && vf.exists() && !vf.isDirectory()
                && vf.lastModified() > lastLoading) {
            onApplicationStart();
            return;
        }
        for (VirtualFile module : Play.modules.values()) {
            vf = module.child("conf/messages");
            if (vf != null && vf.exists() && !vf.isDirectory()
                    && vf.lastModified() > lastLoading) {
                onApplicationStart();
                return;
            }
        }
        for (String locale : Play.langs) {
            vf = Play.getVirtualFile("conf/messages." + locale);
            if (vf != null && vf.exists() && !vf.isDirectory()
                    && vf.lastModified() > lastLoading) {
                onApplicationStart();
                return;
            }
            for (VirtualFile module : Play.modules.values()) {
                vf = module.child("conf/messages." + locale);
                if (vf != null && vf.exists() && !vf.isDirectory()
                        && vf.lastModified() > lastLoading) {
                    onApplicationStart();
                    return;
                }
            }
        }

        for (String includeFilename : includeMessageFilenames) {
            File fileToInclude = new File(includeFilename);
            if (fileToInclude != null && fileToInclude.exists()
                    && !fileToInclude.isDirectory()
                    && fileToInclude.lastModified() > lastLoading) {
                onApplicationStart();
                return;
            }
        }
    }
}