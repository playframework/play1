package play.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.apache.tools.ant.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Ant task which loads settings needed by the ant from the ant configuration file.
 *
 * These include:
 * - Resolving the settings for the given play id and setting them to the ant project properties
 * - Creating classpath element for the module libraries
 *
 */
public class PlayConfigurationLoadTask {

    private Project project;
    /** Play id */
    private String playId = "";
    /** Prefix to use for the properties loaded from the configuration file */
    private String prefix = "application.conf.";
    /** Id for the classpath element */
    private String modulesClasspath = "modules.classpath";
    /** Source file to read */
    private File applicationDir;

    public void setProject(Project project) {
        this.project = project;
    }

    public void setPlayId(String playId) {
        this.playId = playId;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setApplicationDir(File applicationDir) {
        this.applicationDir = applicationDir;
    }

    public void execute() {
        if (applicationDir == null) {
            throw new BuildException("No applicationDir set!");
        }
        File srcFile = new File(applicationDir, "conf/application.conf");
        if (!srcFile.exists()) {
            throw new BuildException("No application configuration found! " + srcFile.getAbsolutePath());
        }
        Map<String,String> map = loadAndResolve(srcFile, playId);
        for (Map.Entry<String,String> entry: map.entrySet()) {
            String key = entry.getKey();
            String value = project.replaceProperties(entry.getValue());
            project.setProperty(prefix + key, value);
            project.log("Loaded property '" + prefix + key + "'='" + value + "'", Project.MSG_VERBOSE);
        }

        File applicationDir = srcFile.getParentFile().getParentFile();
        Path path = new Path(project);

        FilenameSelector endsToJar = new FilenameSelector();
        endsToJar.setName("*.jar");

        for (Map.Entry<String,String> entry : map.entrySet()) {
            if (entry.getKey().startsWith("module.")) {
                String s = entry.getValue();
                s = project.replaceProperties(s);
                File moduleDir;
                if (!FileUtils.isAbsolutePath(s)) {
                    moduleDir = new File(new File(applicationDir, "conf"), s);
                } else {
                    moduleDir = new File(s);
                }
                if (!moduleDir.exists()) {
                    project.log("Failed add non existing module to classpath! " + moduleDir.getAbsolutePath(), Project.MSG_WARN);
                    continue;
                }
                File moduleLib = new File(moduleDir, "lib");
                if (moduleLib.exists()) {
                    FileSet fileSet = new FileSet();
                    fileSet.setDir(moduleLib);
                    fileSet.addFilename(endsToJar);
                    path.addFileset(fileSet);
                    project.log("Added fileSet to path: " + fileSet, Project.MSG_VERBOSE);
                } else {
                    project.log("Ignoring non existing lib dir: " + moduleLib.getAbsolutePath(), Project.MSG_VERBOSE);
                }
            }
        }
        project.addReference(modulesClasspath, path);
        project.log("Generated classpath '" + modulesClasspath + "':" + project.getReference(modulesClasspath), Project.MSG_VERBOSE);
    }

    public static Map<String, String> loadAndResolve(File srcFile, String playId) {
        try {
            Map<String,String> defaults = new HashMap<String,String>();
            Map<String,String> idSpecific = new HashMap<String,String>();
            BufferedReader reader = new BufferedReader(new FileReader(srcFile));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("%")) {
                    if (playId.length() > 0 && line.startsWith(playId + ".")) {
                        line = line.substring((playId + ".").length());
                        String[] sa = splitLine(line);
                        if (sa != null) {
                            idSpecific.put(sa[0], sa[1]);
                        }
                    } else {
                        continue;
                    }
                } else {
                    String[] sa = splitLine(line);
                    if (sa != null) {
                        defaults.put(sa[0], sa[1]);
                    }
                }
            }
            defaults.putAll(idSpecific);
            return defaults;
        } catch (IOException e) {
            throw new BuildException("Failed to load configuration file: " + srcFile.getAbsolutePath(), e);
        }
    }

    private static String[] splitLine(String line) {
        int i = line.indexOf("=");
        if (i > 0) {
            String key = line.substring(0, i);
            String value = "";
            if (i < line.length()) {
                value = line.substring(i+1);
            }
            return new String[]{key.trim(), value.trim()};
        }
        return null;
    }
}
