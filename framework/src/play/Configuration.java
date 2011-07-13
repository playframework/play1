package play;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.libs.IO;
import play.utils.OrderSafeProperties;
import play.vfs.VirtualFile;

public class Configuration extends Properties {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_BASE_DIR = Configuration.class.getResource("/").getPath();
	private static final String DEFAULT_CONF_FILE = "application.conf";
	private static final int MAX_INCLUDE_FILES = 16;
	private static final Pattern PROFILE_KEY_PATTERN = Pattern.compile("^%([a-zA-Z0-9_\\-]+)\\.(.*)$");
	
	public String id = null;
	private HashSet<String> seenFileNames = new HashSet<String>();
	
	/**
	 * stores now filtering key prefix
	 */
	private String prefix = "";
	
	private VirtualFile baseDir = null;
	
	public Configuration hidePrefix()
	{
	    Configuration result = new Configuration();
	    int len = prefix.length();
	    for (final Object key : keySet()) {
	        result.put(key.toString().substring(len), getProperty(key.toString()));
	    }
	    return result;
	}
	private String pushPrefix(final String prefix)
	{
	    if (prefix != null && !prefix.isEmpty()) {
	        this.prefix += prefix + ".";
	    }
	    return this.prefix;
	}
	public Configuration group(final String prefix)
	{
	    Configuration result = new Configuration();
	    final String _prefix = result.pushPrefix(prefix);
        for (final Object key : keySet()) {
            if (key.toString().startsWith(_prefix)) {
                result.put(key, getProperty(key.toString()));
            }
        }
        return result;
	}
	public Configuration group(final String prefix, final String subLabel)
	{
	    return group(prefix + "[" + subLabel + "]");
	}
	public Configuration filterValue(final String pattern)
    {
	    Configuration result = new Configuration();
	    result.prefix = this.prefix;
        for (final Object key : keySet()) {
            final String v = getProperty(key.toString());
            if (v.matches(pattern)) {
                result.put(key, getProperty(key.toString()));
            }
        }
        return result;
    }
    public Configuration filterValue(final Pattern pattern)
    {
        return filterValue(pattern.pattern());
    }
    public List<String> subLabels()
    {
        return null;
    }
	
	/**
	 * Read other Properties instance
	 * 
	 * @param prop
	 */
	public void read(final Properties prop)
	{
		this.putAll(prop);
	}
	
	/**
	 * Read a configuration file
	 * 
	 * @param filename
	 */
	public void read(final String filename)
	{
		if (checkIfAlreadyLoaded(filename)) {
			Logger.debug("config file %s has been already loaded", filename);
			return;
		}

		seenFileNames.add(filename);
		this.read(IO.readUtf8Properties(this.baseDir.child(filename).inputstream()));
	}
	
	/**
	 * Defautl Constructor
	 */
	public Configuration()
	{
		this(VirtualFile.open(DEFAULT_BASE_DIR), DEFAULT_CONF_FILE);
	}
	
	/**
	 * Constructor
	 * 
	 * @param basedir
	 */
	public Configuration(final VirtualFile basedir)
	{
		this(basedir, DEFAULT_CONF_FILE);
	}
	
	/**
	 * Constructor
	 * 
	 * @param configuration filename to load
	 */
	public Configuration(final String filename) {
		this(VirtualFile.open(DEFAULT_BASE_DIR), filename);
	}
	
	/**
	 * Construct from conf file.
	 * @param conf
	 */
	public Configuration(VirtualFile basedir, final String filename)
	{
		super();
		this.baseDir = basedir;
		
		// Builtin configurations
//		this.putIfNotExists("application.path", Play.applicationPath.getAbsolutePath());
//		this.putIfNotExists("play.path", Play.frameworkPath.getAbsolutePath());
		
		try {
			this.read(filename);
			int preIncludes = 0, postIncludes = 0;
			int preProfile = 0, postProfile = 0;
			int loop = 0, maxLoop = 100;
			do {
				if (maxLoop < loop++) {
					throw new RuntimeException("Too many configuration loops occurred.");
				}
				
				// count before
				preProfile = findProfileKeys(Play.id).size();
				preIncludes = findIncludeKeys().size();
				
				// resolve
				detectProfile(Play.id);
				detectInclude();

				// count again
				postProfile = findProfileKeys(Play.id).size();
				postIncludes = findIncludeKeys().size();
				
				// trace
				Logger.trace("Configuration file [%s] processing loop:%d", filename, loop);
				
			} while (preIncludes != postIncludes || preProfile != postProfile);
			
			// DEBUG
			Logger.debug("Configuration -> filename:%s, id:%s, includes:%d files.",
					filename,
					Play.id,
					seenFileNames.size() - 1 // without initial config
					);
			
		} catch (NullPointerException e) {
        	Logger.error("Invalid conf file (null)");
        	System.exit(-1);
        
		} catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
            	Logger.fatal(e.getCause().getMessage());
                Logger.fatal("Cannot read %s", filename);
                
            } else {
            	Logger.fatal(e.getMessage());
            }
            System.exit(-1);
        
		}
	}
	/**
	 * Check if the configuration file already loaded?
	 * 
	 * @param filename
	 * @return true:the configuration file has been already loaded,
	 *         false:not loaded
	 */
	private boolean checkIfAlreadyLoaded(final String filename)
	{
		if (seenFileNames.contains(filename)) {
            //throw new RuntimeException("Detected recursive @include usage. Have seen the file " + filename + " before");
			return true;
        }
        return false;
	}
	public void putIfNotExists(final Object key, final Object value)
	{
		if (!this.contains(key)) {
			this.put(key, value);
		}
	}
	private Set<Object> findIncludeKeys()
	{
		return this.group("@include").keySet();
//		HashSet<String> keys = new HashSet<String>();
//		for (Object key : keySet()) {
//			if (key.toString().startsWith("@include.")) {
//				keys.add(key.toString());
//			}
//		}
//		return keys;
	}
	private void detectInclude()
	{
		Set<Object> keys = findIncludeKeys();
        for (final Object key : keys) {
        	final String filename = this.getProperty(key.toString());
        	if (!seenFileNames.contains(filename)) {
        		// check include count (with default config file)
        		if (MAX_INCLUDE_FILES < seenFileNames.size()) {
        			throw new RuntimeException("Too many @include found in your configuration.");
        		}
        		try {
                    this.read(filename);
                    
                } catch (Exception ex) {
                	ex.printStackTrace();
                    Logger.warn("Missing include: %s", key);
                }
        	}
        }
	}
	private void detectKeyword()
	{
		
	}
	private HashSet<String> findProfileKeys(final String id)
	{
		
		HashSet<String> keys = new HashSet<String>();
		if (id == null) {
			return keys;
		}
		Matcher matcher = null;
		for (Object key : keySet()) {
			matcher = PROFILE_KEY_PATTERN.matcher(key.toString());
            try {
	            if (matcher.matches() && matcher.group(1).equals(id)) {
	            	keys.add(key.toString());
	            }
            } catch (IndexOutOfBoundsException e) {
                Logger.info("configuration key %s cannot load, continue.", key.toString());
                continue;
            }
		}
		return keys;
	}
	private void detectProfile(final String id)
	{
		HashSet<String> keys = findProfileKeys(id);
//        Properties newConfiguration = new OrderSafeProperties();
//        Pattern pattern = Pattern.compile("^%([a-zA-Z0-9_\\-]+)\\.(.*)$");
        for (final Object key : keys) {
            Matcher matcher = PROFILE_KEY_PATTERN.matcher(key.toString());
            this.put(matcher.group(2), this.get(key).toString().trim());
            
        }
//        for (Object key : this.keySet()) {
//            Matcher matcher = pattern.matcher(key + "");
//            if (matcher.matches()) {
//                String instance = matcher.group(1);
//                if (instance.equals(id)) {
//                    newConfiguration.put(matcher.group(2), this.get(key).toString().trim());
//                }
//            }
//        }
        //this = newConfiguration;
	}

	private Set<Object> findRemainKeyword()
	{
//		Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
		return this.filterValue("\\$\\{([^}]+)}").keySet();
	}
	private void resolveKeyword()
	{
		// Resolve ${..}
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)}");
        for (final Object key : keySet()) {
            String value = getProperty(key.toString());
            Matcher matcher = pattern.matcher(value);
            StringBuffer newValue = new StringBuffer(100);
            while (matcher.find()) {
                String jp = matcher.group(1);
                String r;
                if (jp.equals("application.path")) {
                    r = Play.applicationPath.getAbsolutePath();
                
                } else if (jp.equals("play.path")) {
                    r = Play.frameworkPath.getAbsolutePath();
                    
                } else {
                	if (this.contains(jp)) {
                		if (key.toString().trim().equals(jp)) {
                			Logger.fatal("Keyword must not be itself-loop, exit. (%s=%s)", key, value);
                			System.exit(-1);
                		}
                		r = getProperty(jp);
                		
                	} else {
                		r = System.getProperty(jp);
                		
                	}
                    if (r == null) {
                        Logger.warn("Cannot replace %s in configuration (%s=%s)", jp, key, value);
                        continue;
                    }
                }
                matcher.appendReplacement(newValue, r.replaceAll("\\\\", "\\\\\\\\"));
            }
            matcher.appendTail(newValue);
            setProperty(key.toString(), newValue.toString());
        }
	}
}

