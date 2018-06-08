package play.deps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.descriptor.ExcludeRule;
import org.apache.ivy.core.module.descriptor.MDArtifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ArtifactId;
import org.apache.ivy.core.module.id.ModuleId;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.plugins.matcher.ExactOrRegexpPatternMatcher;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.parser.AbstractModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ModuleDescriptorParser;
import org.apache.ivy.plugins.parser.ParserSettings;
import org.apache.ivy.plugins.repository.Resource;
import org.apache.ivy.plugins.repository.url.URLResource;
import org.yaml.snakeyaml.Yaml;

import play.Logger;
import play.Play;
import play.libs.IO;

public class YamlParser extends AbstractModuleDescriptorParser {

    static class Oops extends Exception {
        public Oops(String message) {
            super(message);
        }
    }

    @Override
    public boolean accept(Resource rsrc) {
        return rsrc.exists() && rsrc.getName().endsWith(".yml");
    }



    @Override
    public ModuleDescriptor parseDescriptor(ParserSettings ps, URL url, Resource rsrc, boolean bln) throws ParseException, IOException {
        try {
            InputStream srcStream =  rsrc.openStream();
            long lastModified = (rsrc != null?rsrc.getLastModified():0L);
            
            Yaml yaml = new Yaml();
            Object o = null;

            // Try to parse the yaml
            try {
                o = yaml.load(srcStream);
            } catch (Exception e) {
                throw new Oops(e.toString().replace("\n", "\n~ \t"));
            }

            // We expect a Map here
            if (!(o instanceof Map)) {
                throw new Oops("Unexpected format -> " + o);
            }

            Map data = (Map) o;
            ModuleRevisionId id = null;

            // Search for 'self' tag
            if (data.containsKey("self")) {
                if (data.get("self") instanceof String) {
                    Matcher m = Pattern.compile("([^\\s]+)\\s*[-][>]\\s*([^\\s]+)\\s+([^\\s]+).*").matcher(data.get("self").toString());
                    if (m.matches()) {
                        String org = m.group(1);
                        String name = m.group(2);
                        String rev = m.group(3).replace("$version", System.getProperty("play.version"));
                        id = ModuleRevisionId.newInstance(org, name, rev);
                    } else {
                        throw new Oops("Unknown self format -> " + data.get("self"));
                    }
                } else {
                    throw new Oops("Unknown self format -> " + data.get("self"));
                }
            } else {
                String org = "play-application";
                String name = new File(System.getProperty("application.path")).getName();
                String rev = "1.0";
                id = ModuleRevisionId.newInstance(org, name, rev);
            }

            DefaultModuleDescriptor descriptor = new DefaultModuleDescriptor(id, "release", null, true) {
                @Override
                public ModuleDescriptorParser getParser() {
                    return new YamlParser();
                }
            };
            descriptor.addConfiguration(new Configuration("default"));
            descriptor.addArtifact("default", new MDArtifact(descriptor, id.getName(), "jar", "zip"));
            descriptor.setLastModified(lastModified);

            boolean transitiveDependencies = get(data, "transitiveDependencies", boolean.class, true);
            
            List<String> confs = new ArrayList<>();
            if (data.containsKey("configurations")) {
                if (data.get("configurations") instanceof List) {
                    boolean allExcludes = true;
                    List configurations = (List) data.get("configurations");
                    for (Object conf : configurations) {
                        String confName;
                        Map options;
                        
                        if (conf instanceof String) {
                            confName = ((String) conf).trim();
                            options = new HashMap();
                        } else if (conf instanceof Map) {
                            confName = ((Map) conf).keySet().iterator().next().toString().trim();
                            options = (Map) ((Map) conf).values().iterator().next();
                        } else {
                            throw new Oops("Unknown configuration format -> " + conf);
                        }
                        boolean exclude = options.containsKey("exclude") && options.get("exclude") instanceof Boolean ? (Boolean) options.get("exclude") : false;
                        allExcludes &=  exclude;
                        confs.add((exclude ? "!" : "") + confName);
                    }
                    
                    if (allExcludes) {
                        confs.add(0, "*");
                    }
                } else {
                    throw new Oops("Unknown \"configurations\" format -> " + data.get("self"));
                }
            } else {
                confs.add("*");
            }
            
            if (data.containsKey("require")) {
                if (data.get("require") instanceof List) {

                    List dependencies = (List) data.get("require");
                    for (Object dep : dependencies) {

                        String depName;
                        Map options;

                        if (dep instanceof String) {
                            depName = ((String) dep).trim();
                            options = new HashMap();
                        } else if (dep instanceof Map) {
                            depName = ((Map) dep).keySet().iterator().next().toString().trim();
                            options = (Map) ((Map) dep).values().iterator().next();
                        } else {
                            throw new Oops("Unknown dependency format -> " + dep);
                        }

                        // Hack
                        depName = depName.replace("$version", System.getProperty("play.version"));
                        if(depName.matches("play\\s+->\\s+play") || depName.equals("play")) {
                            depName = "play -> play " + System.getProperty("play.version");
                        }
                        if(depName.matches("play\\s+->\\s+crud") || depName.equals("crud")) {
                            depName = "play -> crud " + System.getProperty("play.version");
                        }
                        if(depName.matches("play\\s+->\\s+secure") || depName.equals("secure")) {
                            depName = "play -> secure " + System.getProperty("play.version");
                        }
                        if(depName.matches("play\\s+->\\s+docviewer") || depName.equals("docviewer")) {
                            depName = "play -> docviewer " + System.getProperty("play.version");
                        }

                        // Pattern compile to match [organisation name] - > [artifact] [revision] [classifier]
                        Matcher m = Pattern.compile("([^\\s]+)\\s*[-][>]\\s*([^\\s]+)\\s+([^\\s]+)(\\s+[^\\s]+)?.*").matcher(depName);
                        if (!m.matches()) {
                         // Pattern compile to match [artifact] [revision] [classifier]
                            m = Pattern.compile("(([^\\s]+))\\s+([^\\s]+)(\\s+[^\\s]+)?.*").matcher(depName);
                            if (!m.matches()) {
                                throw new Oops("Unknown dependency format -> " + depName);
                            } else if( m.groupCount() >= 3 && m.group(3).trim().equals("->")){
                                throw new Oops("Missing revision in dependency format (use \"latest.integration\" to  matches all versions) -> " + depName);
                            }
                        }
                        HashMap extraAttributesMap = null;
                        if (m.groupCount() == 4 && m.group(4) != null && !m.group(4).trim().isEmpty()) {
                            // dependency has a classifier
                            extraAttributesMap = new HashMap();
                            extraAttributesMap.put("classifier", m.group(4).trim());
                        }

                        ModuleRevisionId depId = ModuleRevisionId.newInstance(m.group(1), m.group(2), m.group(3), extraAttributesMap);

                        boolean transitive = options.containsKey("transitive") && options.get("transitive") instanceof Boolean ? (Boolean) options.get("transitive") : transitiveDependencies;
                        boolean force = options.containsKey("force") && options.get("force") instanceof Boolean ? (Boolean) options.get("force") : false;
                        boolean changing = options.containsKey("changing") && options.get("changing") instanceof Boolean ? (Boolean) options.get("changing") : false;

                        DefaultDependencyDescriptor depDescriptor = new DefaultDependencyDescriptor(descriptor, depId, force, changing, transitive);
                        for (String conf : confs) {
                            depDescriptor.addDependencyConfiguration("default", conf);
                        }

                        // Exclude transitive dependencies
                        if (options.containsKey("exclude") && options.get("exclude") instanceof List) {
                            List exclude = (List) options.get("exclude");
                            for (Object ex : exclude) {
                                String exName = ex.toString().trim();
                                m = Pattern.compile("([^\\s]+)\\s*[-][>]\\s*([^\\s]+).*").matcher(exName);
                                if (!m.matches()) {
                                    m = Pattern.compile("([^\\s]+)").matcher(exName);
                                    if (!m.matches()) {
                                        throw new Oops("Unknown exclude format -> " + exName);
                                    }
                                }
                                String org = m.group(1);
                                String module = "*";
                                if (m.groupCount() > 1) {
                                    module = m.group(2);
                                }

                                ArtifactId aid = new ArtifactId(new ModuleId(org, module), "*", "*", "*");
                                PatternMatcher matcher = new ExactOrRegexpPatternMatcher();
                                ExcludeRule excludeRule = new DefaultExcludeRule(aid, matcher, new HashMap());
                                depDescriptor.addExcludeRule("default", excludeRule);
                            }
                        }

                        // Ids
                        boolean useIt = true;
                        String currentId = System.getProperty("play.id");
                        if (currentId == null || currentId.trim().equals("")) {
                            currentId = "unset";
                        }
                        if (options.containsKey("id")) {
                            if (options.get("id") instanceof String) {
                                useIt = options.get("id").toString().equals(currentId);
                            } else if (options.get("id") instanceof List) {
                                useIt = ((List) options.get("id")).contains(currentId);
                            }
                        }

                        if (useIt) {
                            // Add it!
                            descriptor.addDependency(depDescriptor);
                        }

                    }

                } else {
                    throw new Oops("require list not found -> " + o);
                }
            }

            return descriptor;

        } catch (Oops e) {
            System.out.println("~ Oops, malformed dependencies.yml descriptor:");
            System.out.println("~");
            System.out.println("~ \t" + e.getMessage());
            System.out.println("~");
            throw new ParseException("Malformed dependencies.yml descriptor", 0);
        }
    }

    @Override
    public void toIvyFile(InputStream in, Resource rsrc, File file, ModuleDescriptor md) throws ParseException, IOException {
        md.toIvyFile(file);
    }

    @SuppressWarnings("unchecked")
    <T> T get(Map data, String key, Class<T> type) {
        if (data.containsKey(key)) {
            Object o = data.get(key);
            if (type.isAssignableFrom(o.getClass())) {
                if(o instanceof String) {
                    o = o.toString().replace("${play.path}", System.getProperty("play.path"));
                    o = o.toString().replace("${application.path}", System.getProperty("application.path"));
                }
                return (T) o;
            }
        }
        return null;
    }

    <T> T get(Map data, String key, Class<T> type, T defaultValue) {
        T o = get(data, key, type);
        if (o == null) {
            return defaultValue;
        }
        return o;
    }

    public static Set<String> getOrderedModuleList(File file) throws ParseException, IOException {
        Set<String> modules = new LinkedHashSet<>();
        System.setProperty("application.path", Play.applicationPath.getAbsolutePath());
        return getOrderedModuleList(modules, file);
    }
        
   private static Set<String> getOrderedModuleList(Set<String> modules, File file) throws ParseException, IOException {
        if (file == null || !file.exists()) {
            throw new FileNotFoundException("There was a problem to find the file");
        }

        YamlParser parser = new YamlParser();

        ModuleDescriptor md = parser.parseDescriptor(null, null, new URLResource(file.toURI().toURL()), true);

        DependencyDescriptor[] rules = md.getDependencies();
        File localModules = Play.getFile("modules");
        for (DependencyDescriptor dep : rules) {
            ModuleRevisionId rev = dep.getDependencyRevisionId();       
            String moduleName = filterModuleName(rev);
            
            // Check if the module was already load to avoid circular parsing
            if (moduleName != null && !modules.contains(moduleName)) {
                // Add the given module
                modules.add(moduleName);
                
                // Need to load module dependencies of this given module 
                File module = new File(localModules, moduleName);
                if(module != null && module.isDirectory()) {  
                    File ivyModule = new File(module, "conf/dependencies.yml");
                    if(ivyModule != null && ivyModule.exists()) {
                        getOrderedModuleList(modules, ivyModule);
                    }    
                } else {
                    File modulePath = new File(IO.readContentAsString(module).trim());
                    if (modulePath.exists() && modulePath.isDirectory()) {
                        File ivyModule = new File(modulePath, "conf/dependencies.yml");
                        if(ivyModule != null && ivyModule.exists()) {
                            getOrderedModuleList(modules, ivyModule);
                        } 
                    }
                }
            } else if(moduleName == null && rev.getRevision().equals("->")){
                Logger.error("Revision is required, module [%s -> %s] will not be loaded.", rev.getName(), rev.getExtraAttribute("classifier"));
            }
        }
        return modules;
    }
    
      
    private static String filterModuleName(ModuleRevisionId rev) {
        if (rev != null && !"play".equals(rev.getName())) {
            File moduleDir = new File(Play.applicationPath, "modules");
            if(moduleDir != null && moduleDir.isDirectory()){
                // create new filename filter to check if it is a module (lib will
                // be skipped)
                File[] filterFiles = moduleDir.listFiles(new ModuleFilter(rev));
                if (filterFiles != null && filterFiles.length > 0) {
                    return filterFiles[0].getName();
               }
            }
        }

        return null;

    }
    private static class ModuleFilter implements FilenameFilter {

        private ModuleRevisionId moduleRevision;

        public ModuleFilter(ModuleRevisionId moduleRevision) {
            this.moduleRevision = moduleRevision;
        }

        @Override
        public boolean accept(File dir, String name) {
            // Accept module with the same name or with a version number
            if (name != null && moduleRevision != null &&
                    (name.equals(moduleRevision.getName())
                    || name.equals(moduleRevision.getName() + "-" + moduleRevision.getRevision())
                    || name.startsWith(moduleRevision.getName() + "-"))) {
                return true;
            }
            return false;
        }
    }

}
