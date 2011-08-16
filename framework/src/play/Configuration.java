package play;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import play.exceptions.ConfigurationException;
import play.libs.IO;
import play.vfs.VirtualFile;

public class Configuration extends Properties {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_BASE_DIR = Configuration.class.getResource("/").getPath();
//	private static final String DEFAULT_CONF_FILE = "application.conf";
	private static final int MAX_INCLUDE_FILES = 16;
	private static final Pattern PROFILE_KEY_PATTERN = Pattern.compile("^%([a-zA-Z0-9_\\-]+)\\.(.*)$");
	
	public String id = null;
	private HashSet<String> seenFileNames = new HashSet<String>();
	
	/**
	 * stores now filtering key prefix
	 */
	public String prefix = "";
	private List<String> subLabels = new ArrayList<String>();
	private VirtualFile baseDir = null;
	
	/**
	 * Defautl Constructor
	 */
	public Configuration()
	{
		super();
	}
	/**
	 * Constructor
	 * 
	 * @param basedir
	 */
	public Configuration(final VirtualFile file) throws ConfigurationException
	{
		super();
		this.baseDir = file.child("..");
		Logger.debug("loading %s...", file.getName());
		
		// Builtin configurations
//		this.putIfNotExists("application.path", Play.applicationPath.getAbsolutePath());
//		this.putIfNotExists("play.path", Play.frameworkPath.getAbsolutePath());
		
		final String profile = Play.id;
		try {
			this.read(file);
			Logger.debug("loaded %s...", file.getName());
			int preIncludes = 0, postIncludes = 0;
			int preProfile = 0, postProfile = 0;
			int loop = 0, maxLoop = 100;
			do {
				if (maxLoop < loop++) {
					throw new RuntimeException("Too many configuration loops occurred.");
				}
				
				// count before
				preProfile = findProfileKeys(profile).size();
				preIncludes = findIncludeKeys().size();
				
				// resolve
				detectProfile(profile);
				detectInclude();

				// count again
				postProfile = findProfileKeys(profile).size();
				postIncludes = findIncludeKeys().size();
				
				// trace
				Logger.trace("Configuration file [%s] processing loop:%d", file.getName(), loop);
				
			} while (preIncludes != postIncludes || preProfile != postProfile);
			
			// Keyword
			resolveKeyword();
			
			// DEBUG
			Logger.debug("Configuration -> filename:%s, profile:%s, includes:%d files.",
					file.getName(),
					profile,
					seenFileNames.size() - 1 // without initial config
					);
			
		} catch (NullPointerException e) {
        	Logger.error("Invalid conf file (null)");
        	System.exit(-1);
        	
		} catch (ConfigurationException e) {
			Logger.error(e.getMessage());
			throw e;
			
		} catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
            	Logger.fatal(e.getCause().getMessage());
                Logger.fatal("Cannot read %s", file.getName());
                
            } else {
            	Logger.fatal(e.toString());
            }
            e.printStackTrace();
            System.exit(-1);
        
		}
	}
	
	/**
	 * Constructor
	 * 
	 * @param configuration filename to load
	 */
	public Configuration(final String filename) throws ConfigurationException {
		this(VirtualFile.open(DEFAULT_BASE_DIR).child(filename));
	}
	
	/**
	 * Construct from conf file.
	 * @param conf
	 */
	public Configuration(VirtualFile basedir, final String filename) throws ConfigurationException	{
		this(basedir.child(filename));
	}
	
	/**
	 * Creates a new Configuration from this without upper prefixes
	 * 
	 * @return new Configuration object without prefixes
	 */
	public Configuration hidePrefix()
	{
		if (this.prefix.isEmpty()) {
			return this;
		}
	    Configuration result = new Configuration();
	    result.prefix = "";
	    
	    // without next "." character
	    final int len = this.prefix.length() + 1;
	    for (final Object key : keySet()) {
	    	Logger.warn("prefix:%s key:%s", this.prefix, key);
	    	if (key.toString().equals(this.prefix)) {
	    		result.put("", getProperty(key.toString()));
	    	} else {
	    		result.put(key.toString().substring(len), getProperty(key.toString()));
	    	}
	    }
	    return result;
	}
	
	/**
	 * Pushes prefix stack
	 * 
	 * @param stacking prefix string
	 * @return result prefix string
	 */
	private String pushPrefix(final String prefix)
	{
	    if (prefix != null && !prefix.isEmpty()) {
	        this.prefix += "." + prefix;
	    }
	    if (this.prefix.startsWith(".")) {
	    	this.prefix = this.prefix.replaceFirst("\\.", "");
	    }
	    return this.prefix;
	}
	
	/**
	 * configuration Sub-Label utility class
	 * 
	 * <p>'Sub-Label' is string to grouping configurations.</p>
	 * <p>For example, DB connection settings can define like below.
	 * <pre>db.url=jdbc://localhost/test
	 * db[readonly].url=jdbc://localhost/test?readonly=true</pre>
	 * </p>
	 */
	public static class SubLabel
	{
		/**
		 * 
		 * @param subLabel
		 * @return
		 */
		public static String format(final String subLabel)
		{
			return format("", subLabel);
		}
		
		/**
		 * building sub-label key prefix
		 * 
		 * @param key
		 * @param subLabel
		 * @return build result string
		 */
		public static String format(final String key, final String subLabel)
		{
			return key + "[" + subLabel + "]";
		}
		
		/**
		 * 
		 * @param key
		 * @return
		 */
		public static boolean exists(final String key)
		{
			return exists(key, "");
		}
		
		/**
		 * is this key having sublabel?
		 * 
		 * @param key
		 * @param prefix
		 * @return
		 */
		public static boolean exists(final String key, final String prefix)
		{
			return key.startsWith(prefix + "[") && key.contains("]");
		}
		
		/**
		 * 
		 * @param key
		 * @return
		 */
		public static String get(final String key)
		{
			return get(key, "");
		}
		
		/**
		 * 
		 * @param key
		 * @param prefix
		 * @return
		 */
		public static String get(final String key, final String prefix)
		{
			try {
				String src = key.substring(prefix.length() + 1);
				return src.substring(0, src.indexOf("]"));
				
			} catch (Exception e) {
				return null;
			}
		}
	}
	
	/**
	 * filtering specified group prefix
	 * 
	 * @param prefix
	 * @return
	 */
	public Configuration group(final String prefix)
	{
		// checking arg
		if (prefix == null || prefix.trim().isEmpty()) {
			return this;
		}
		
		// filtering group
	    Configuration result = new Configuration();
	    result.prefix = this.prefix;
	    final String _prefix = result.pushPrefix(prefix);
	    Logger.info("New configuration prefix is %s.", result.prefix);
        for (final Object key : keySet()) {
        	final String _key = key.toString();
            if (_key.equals(_prefix)
            		|| _key.startsWith(_prefix + ".")) {
                result.put(key, getProperty(_key));
                
            } else if (SubLabel.exists(_key, _prefix)) {
            	final String sublabel = SubLabel.get(_key, _prefix);
            	if (sublabel != null && !result.subLabels.contains(sublabel)) {
            		result.subLabels.add(sublabel);
            	}
            }
        }
        Logger.debug("Prefix:%s SIZE:%d.", _prefix, result.size());
        return result;
	}
	
	/**
	 * filtering group with sub-label
	 * 
	 * @param prefix
	 * @param subLabel
	 * @return
	 */
	public Configuration group(final String prefix, final String subLabel)
	{
	    return group(SubLabel.format(prefix, subLabel));
	}
	
	/**
	 * 
	 * @return
	 */
	public List<String> getSubLables()
	{
		return this.subLabels;
	}
	
	/**
	 * 
	 * @param pattern
	 * @return
	 */
	public Configuration filterValue(final String pattern)
    {
		// checking arg
		if (pattern == null || pattern.trim().isEmpty()) {
			return this;
		}
		
		// filtering value
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
	
	/**
	 * 
	 * @param pattern
	 * @return
	 */
    public Configuration filterValue(final Pattern pattern)
    {
        return filterValue(pattern.pattern());
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
	public void read(final VirtualFile filename)
	{
		if (checkIfAlreadyLoaded(filename.getName())) {
			Logger.debug("config file %s has been already loaded", filename);
			return;
		}

		seenFileNames.add(filename.getName());
		this.read(IO.readUtf8Properties(filename.inputstream()));
	}
	
	/**
	 * 
	 * @return
	 */
	public Object getProperty()
	{
		return this.getProperty("");
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
	
	/**
	 * 
	 * @param key
	 * @param value
	 */
	public void putIfNotExists(final Object key, final Object value)
	{
		if (!this.contains(key)) {
			this.put(key, value);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	private Set<Object> findIncludeKeys()
	{
		return this.group("@include").keySet();
	}
	
	/**
	 * 
	 */
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
        			Logger.debug("@include %s found.", filename);
                    this.read(this.baseDir.child(filename));
                    
                } catch (Exception ex) {
                	ex.printStackTrace();
                    Logger.warn("Missing include: %s", key);
                }
        	}
        }
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 */
	@Deprecated
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
	
	/**
	 * 
	 * @param id
	 */
	private void detectProfile(final String id)
	{
        for (final Object key : this.keySet()) {
            Matcher matcher = PROFILE_KEY_PATTERN.matcher(key.toString());
            if (matcher.matches() && matcher.group(1).equals(id)) {
            	this.put(matcher.group(2), this.get(key).toString().trim());
            }
        }
	}

	/**
	 * 
	 * @throws ConfigurationException
	 */
	private void resolveKeyword() throws ConfigurationException
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
                	if (this.containsKey(jp)) {
                		if (key.toString().trim().equals(jp)) {
                			Logger.fatal("Keyword must not be itself-loop, exit. (%s=%s)", key, value);
                			throw new ConfigurationException("Self keyword replacement loop occurred.");
                		}
                		r = getProperty(jp);
                		
                	} else {
                		Logger.warn("finding system property %s", jp);
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
	
	public <T> T toObject(final Class<T> cls)
	{
		return null;
	}
	
	public <T> T toObject(final Class<T> cls, T object)
	{
		return object;
	}
}

