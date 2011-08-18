package play.deps;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.matcher.PatternMatcher;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.FileSystemResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.plugins.resolver.URLResolver;
import org.yaml.snakeyaml.Yaml;

public class SettingsParser {

    static class Oops extends Exception {

        public Oops(String message) {
            super(message);
        }
    }
    
    HumanReadyLogger logger;

    public SettingsParser(HumanReadyLogger logger) {
        this.logger = logger;
    }

    public void parse(IvySettings settings, File desc) {
        if(!desc.exists()) {
            System.out.println("~ !! " + desc.getAbsolutePath() + " does not exist");
            return;
        }

        try {
            Yaml yaml = new Yaml();
            Object o = null;

            // Try to parse the yaml
            try {
                o = yaml.load(new FileInputStream(desc));
            } catch (Exception e) {
                throw new Oops(e.toString().replace("\n", "\n~ \t"));
            }

            // We expect a Map here
            if (!(o instanceof Map)) {
                throw new Oops("Unexpected format -> " + o);
            }

            Map data = (Map) o;

            parseIncludes(settings, data);

            if (data.containsKey("repositories")) {
                if (data.get("repositories") instanceof List) {

                    List repositories = (List) data.get("repositories");
                    List<Map<String, String>> modules = new ArrayList<Map<String, String>>();
                    for (Object dep: repositories) {
                        if (dep instanceof Map) {
                            settings.addResolver(parseRepository((Map) dep, modules));
                        } else {
                            throw new Oops("Unknown repository format -> " + dep);
                        }
                    }

                    for (Map attributes: modules) {
                        settings.addModuleConfiguration(attributes, settings.getMatcher(PatternMatcher.EXACT_OR_REGEXP), (String) attributes.remove("resolver"), null, null, null);
                    }

                } else {
                    throw new Oops("repositories list not found -> " + o);
                }
            }


        } catch (Oops e) {
            System.out.println("~ Oops, malformed dependencies.yml descriptor:");
            System.out.println("~");
            System.out.println("~ \t" + e.getMessage());
            System.out.println("~");
            throw new RuntimeException("Malformed dependencies.yml descriptor");
        }
    }

    /**
     * Look for an "include" property containing a list of yaml descriptors and load their repositories.
     */
    private void parseIncludes(IvySettings settings, Map data) throws Oops {
        if (data.containsKey("include") && data.get("include") != null) {
            if (data.get("include") instanceof List) {
                List<?> includes = (List)data.get("include");
                if (includes != null) {
                    for (Object inc : includes) {
                        File include = new File(substitute(inc.toString()));
                        new SettingsParser(logger).parse(settings, include); // Load found descriptors
                    }
                }
            } else {
                throw new Oops("\"include\" property must be a list");
            }
        }
    }

    DependencyResolver parseRepository(Map repoDescriptor, List<Map<String, String>> modules) throws Oops {

        Object key = repoDescriptor.keySet().iterator().next();
        String repName = key.toString().trim();
        Map options = (Map) repoDescriptor.get(key);

        if (options == null) {
            throw new Oops("Parsing error in " + repName + ": check the format and the indentation.");
        }
        String type = get(options, "type", String.class);
        if (type == null) {
            throw new Oops("Repository type need to be specified -> " + repName + ": " + options);
        }

        DependencyResolver resolver = null;

        if (type.equalsIgnoreCase("iBiblio")) {
            IBiblioResolver iBiblioResolver = new IBiblioResolver();
            iBiblioResolver.setName(repName);
            if(options.containsKey("root")) {
                iBiblioResolver.setRoot(get(options, "root", String.class));
            }
            iBiblioResolver.setM2compatible(get(options, "m2compatible", boolean.class, true));
            iBiblioResolver.getRepository().addTransferListener(logger);
            resolver = iBiblioResolver;
        }

        if (type.equalsIgnoreCase("local")) {
            FileSystemResolver fileSystemResolver = new FileSystemResolver();
            fileSystemResolver.setName(repName);
            fileSystemResolver.setLocal(true);
            if (get(options, "descriptor", String.class) != null) {
                fileSystemResolver.addIvyPattern(get(options, "descriptor", String.class));
            }
            if (get(options, "artifact", String.class) != null) {
                fileSystemResolver.addArtifactPattern(get(options, "artifact", String.class));
            }
            fileSystemResolver.setCheckmodified(true);
            resolver = fileSystemResolver;
        }

        if (type.equalsIgnoreCase("http")) {
            URLResolver urlResolver = new URLResolver();
            urlResolver.setName(repName);
            if (get(options, "descriptor", String.class) != null) {
                urlResolver.addIvyPattern(get(options, "descriptor", String.class));
            }
            if (get(options, "artifact", String.class) != null) {
                urlResolver.addArtifactPattern(get(options, "artifact", String.class));
            }
            urlResolver.getRepository().addTransferListener(logger);
            resolver = urlResolver;
        }

        if (type.equalsIgnoreCase("chain")) {
            ChainResolver chainResolver = new ChainResolver();
            chainResolver.setName(repName);
            chainResolver.setReturnFirst(true);
            for (Object o : get(options, "using", List.class, new ArrayList())) {
                DependencyResolver res = parseRepository((Map) o, modules);
                if(res instanceof FileSystemResolver) {
                    chainResolver.setCheckmodified(true);
                }
                chainResolver.add(res);
            }
            resolver = chainResolver;
        }

        if (resolver == null) {
            throw new Oops("Unknown repository type -> " + type);
        }

        List contains = get(options, "contains", List.class);
        if (contains != null) {
            for (Object o : contains) {
                String v = o.toString().trim();
                String module = null;
                String organisation = null;
                String revision = null;
                Matcher m = Pattern.compile("([^\\s]+)\\s*[-][>]\\s*([^\\s]+)\\s+([^\\s]+)").matcher(v);
                if (m.matches()) {
                    organisation = m.group(1);
                    module = m.group(2);
                    revision = m.group(3).replace("$version", System.getProperty("play.version"));
                } else {
                    m = Pattern.compile("(([^\\s]+))\\s+([^\\s]+)").matcher(v);
                    if (m.matches()) {
                        organisation = m.group(1);
                        module = m.group(2);
                        revision = m.group(3).replace("$version", System.getProperty("play.version"));
                    } else {
                        m = Pattern.compile("([^\\s]+)\\s*[-][>]\\s*([^\\s]+)").matcher(v);
                        if (m.matches()) {
                            organisation = m.group(1);
                            module = m.group(2);
                        } else {
                            m = Pattern.compile("([^\\s]+)").matcher(v);
                            if (m.matches()) {
                                organisation = m.group(1);
                            } else {
                                throw new Oops("Unknown depedency format -> " + o);
                            }
                        }
                    }
                }
                Map<String, String> attributes = new HashMap<String, String>();
                attributes.put("organisation", organisation);
                if (module != null) {
                    attributes.put("module", module);
                }
                if (revision != null) {
                    attributes.put("revision", revision);
                }
                attributes.put("resolver", repName);
                modules.add(attributes);
            }
        }

        return resolver;
    }

    @SuppressWarnings("unchecked")
    <T> T get(Map data, String key, Class<T> type) throws Oops {
        if (data.containsKey(key) && data.get(key) != null) {
            Object o = data.get(key);
            if (type.isAssignableFrom(o.getClass())) {
                if (o instanceof String) {
                    o = substitute(o.toString());
                }
                return (T) o;
            }
        }
        return null;
    }

    /**
     * Substitute environment variables found in a <code>String</code> with their value.
     * This function search for <code>${variable}</code> patterns and replace them with
     * their value taken from current environment.
     *
     * @param s <code>String</code> to substitute
     * @return The substituted <code>String</code>
     * @throws Oops If an environment variable is not found
     */
    private String substitute(String s) throws Oops {
        Matcher m = Pattern.compile("\\$\\{([^\\}]*)\\}").matcher((String)s); //search of ${something} group(1) => something
        while (m.find()) {
            String propertyValue = System.getProperty(m.group(1));
            if(propertyValue != null){
                s = s.replace("${" + m.group(1) + "}",propertyValue);
            } else {
                throw new Oops("Unknown property " + m.group(1) + " in " + s);
            }
        }
        return s;
    }

    <T> T get(Map data, String key, Class<T> type, T defaultValue) throws Oops {
        T o = get(data, key, type);
        if (o == null) {
            return defaultValue;
        }
        return o;
    }
}
