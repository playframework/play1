package play.deps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultExcludeRule;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
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
import org.yaml.snakeyaml.Yaml;

public class YamlParser extends AbstractModuleDescriptorParser {

    static class Oops extends Exception {

        public Oops(String message) {
            super(message);
        }
    }

    public boolean accept(Resource rsrc) {
        return rsrc.exists() && rsrc.getName().endsWith(".yml");
    }

    public ModuleDescriptor parseDescriptor(ParserSettings ps, URL url, Resource rsrc, boolean bln) throws ParseException, IOException {
        try {
            Yaml yaml = new Yaml();
            Object o = null;

            // Try to parse the yaml
            try {
                o = yaml.load(rsrc.openStream());
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
            descriptor.setLastModified(rsrc.getLastModified());

            boolean transitiveDependencies = get(data, "transitiveDependencies", boolean.class, true);
            
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

                        Matcher m = Pattern.compile("([^\\s]+)\\s*[-][>]\\s*([^\\s]+)\\s+([^\\s]+).*").matcher(depName);
                        if (!m.matches()) {
                            m = Pattern.compile("(([^\\s]+))\\s+([^\\s]+).*").matcher(depName);
                            if (!m.matches()) {
                                throw new Oops("Unknown dependency format -> " + depName);
                            }
                        }

                        ModuleRevisionId depId = ModuleRevisionId.newInstance(m.group(1), m.group(2), m.group(3));

                        boolean transitive = options.containsKey("transitive") && options.get("transitive") instanceof Boolean ? (Boolean) options.get("transitive") : transitiveDependencies;
                        boolean force = options.containsKey("force") && options.get("force") instanceof Boolean ? (Boolean) options.get("force") : false;
                        boolean changing = options.containsKey("changing") && options.get("changing") instanceof Boolean ? (Boolean) options.get("changing") : false;

                        DefaultDependencyDescriptor depDescriptor = new DefaultDependencyDescriptor(descriptor, depId, force, changing, transitive);
                        depDescriptor.addDependencyConfiguration("default", "*");

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

    public void toIvyFile(InputStream in, Resource rsrc, File file, ModuleDescriptor md) throws ParseException, IOException {
        ((DefaultModuleDescriptor)md).toIvyFile(file);
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
}
