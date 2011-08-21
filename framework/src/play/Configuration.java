package play;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

/**
 * play framework configuration class
 * 
 * <p>Feature:
 * <ul>
 * <li>keyword resolving is not only system property, other configuration key can be in-line keyword.
 * and keyword resolving works in other(included) configuration.</li>
 * <li>grouping configurations having same prefix.
 * for example, filtering with prefix 'db' gets configurations their starts with 'db.'.</li>
 * <li>in grouping feature, they can specify sub label.
 * for example, filtering with prefix 'db' and sub label 'readonly',
 * gets configurations their starts with 'db[readonly].'.</li>
 * <li>can create java bean object from configuration.
 * bean class should have public field or public setter method,
 * and the configuration having same name with them copies into bean instance.</li>
 * </ul></p>
 */
public class Configuration extends Properties {

	private static final long serialVersionUID = 1L;
	
	/** default directory of find configration file */
	private static final String DEFAULT_BASE_DIR = Configuration.class.getResource("/").getPath();
	
	/** max count of include file */
	private static final int MAX_INCLUDE_FILES = 16;
	
	/** max count of resolving include and keyword */
	private static final int MAX_RESOLVE_LOOP_COUNT = 100;
	
	/** pattern of profile key */
	private static final Pattern PROFILE_KEY_PATTERN = Pattern.compile("^%([a-zA-Z0-9_\\-]+)\\.(.*)$");
	
	/** set of configuration file already read */
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
	 * @param VirtualFile instance references root configuration file.
	 * @throws ConfigurationException
	 */
	public Configuration(final VirtualFile file) throws ConfigurationException
	{
		super();
		this.baseDir = file.child("..");
		Logger.debug("loading %s...", file.getName());
		
		// Builtin configurations
		try {
			this.putIfNotExists("application.path", Play.applicationPath.getAbsolutePath());
		} catch (NullPointerException e) {
			play.Logger.error("Could not load `Play.applicationPath` : %s", e.toString());
		}
		try {
			this.putIfNotExists("play.path", Play.frameworkPath.getAbsolutePath());
		} catch (NullPointerException e) {
			play.Logger.error("Could not load `Play.frameworkPath` : %s", e.toString());
		}
		
		final String profile = Play.id;
		try {
			this.read(file);
			Logger.debug("read %s...", file.getName());
			int preIncludes = 0, postIncludes = 0;
			int preProfile = 0, postProfile = 0;
			int loop = 0;
			
			// loops while new configuration keys found
			do {
				if (MAX_RESOLVE_LOOP_COUNT < loop++) {
					throw new RuntimeException("Too many configuration loops occurred.");
				}
				
				// count before
				preProfile = findProfileKeys(profile).size();
				preIncludes = findIncludeKeys().size();
				
				// resolve
				resolveProfile(profile);
				resolveInclude();

				// count again
				postProfile = findProfileKeys(profile).size();
				postIncludes = findIncludeKeys().size();
				
				// trace
				Logger.trace("Configuration file [%s] processing loop:%d", file.getName(), loop);
				
			} while (preIncludes != postIncludes || preProfile != postProfile);
			
			// Keyword
			resolveKeyword();
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
	 * 
	 * @param VirtualFile instance references parent directory of configuration file
	 * @param file name of configuration file
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
	 * this settings have sub label `readonly` with prefix `db`.
	 * </p>
	 */
	public static class SubLabel
	{
		/**
		 * building sub label key prefix
		 * 
		 * @param subLabel sub label
		 * @return result string
		 */
		public static String format(final String subLabel)
		{
			return format("", subLabel);
		}
		
		/**
		 * building sub label key prefix
		 * 
		 * @param key main label
		 * @param subLabel sub label
		 * @return result string
		 */
		public static String format(final String key, final String subLabel)
		{
			return key + "[" + subLabel + "]";
		}
		
		/**
		 * is this key having sub label?
		 * 
		 * @param key the original key string
		 * @return true:exists false:not exists
		 */
		public static boolean exists(final String key)
		{
			return exists(key, "");
		}
		
		/**
		 * is this key having sub label?
		 * 
		 * @param key the original key string
		 * @param prefix main label string
		 * @return true:exists false:not exists
		 */
		public static boolean exists(final String key, final String prefix)
		{
			return key.startsWith(prefix + "[") && key.contains("]");
		}
		
		/**
		 * split sub label from key string without main label
		 * 
		 * @param key the original key string
		 * @return only sub label string
		 */
		public static String get(final String key)
		{
			return get(key, "");
		}
		
		/**
		 * split sub label from key string
		 * 
		 * @param key the original key string
		 * @param prefix main label string
		 * @return only sub label string
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
	 * @param prefix main label
	 * @return new Configuration instance having the prefix
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
	    Logger.debug("New configuration prefix is %s.", result.prefix);
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
	 * @param prefix main label
	 * @param subLabel sub label
	 * @return new Configuration instance having the prefix and sublabel
	 */
	public Configuration group(final String prefix, final String subLabel)
	{
	    return group(SubLabel.format(prefix, subLabel));
	}
	
	/**
	 * get list of sublabel
	 * 
	 * @return list of sublabel in current grouping
	 */
	public List<String> getSubLables()
	{
		return this.subLabels;
	}
	
	/**
	 * filter with value
	 * 
	 * @param pattern Regex pattern string of filter
	 * @return new Configuration instance having matches the pattern
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
	 * filter with value
	 * 
	 * @param pattern Regex pattern of filter
	 * @return new Configuration instance having matches the pattern
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
	 * @param file VirtualFile instance references configuration file
	 */
	public void read(final VirtualFile file)
	{
		if (checkIfAlreadyLoaded(file.getName())) {
			Logger.debug("config file %s has been already loaded", file);
			return;
		}

		seenFileNames.add(file.getName());
		this.read(IO.readUtf8Properties(file.inputstream()));
	}
	
	/**
	 * get property with blank key
	 * 
	 * when excuted group() and hidePrefix(), configuration has no key name
	 * if that has just same name with group() parameter.
	 * this method is for it.
	 * 
	 * @return root configuration value
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
	 * put value if the key has not exists
	 * 
	 * @param key configuration key
	 * @param value configuration value
	 */
	public void putIfNotExists(final Object key, final Object value)
	{
		if (!this.contains(key)) {
			this.put(key, value);
		}
	}
	
	/**
	 * get list of configuration include definition
	 * 
	 * @return keys starts with "@include."
	 */
	private Set<Object> findIncludeKeys()
	{
		return this.group("@include").keySet();
	}
	
	/**
	 * process configuration include
	 */
	private void resolveInclude()
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
	 * count profiled keys
	 * 
	 * @param id playframework application profile
	 * @return the count of keys having same profile with param.
	 */
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
	 * process the profiled keys
	 * 
	 * @param id playframework application profile
	 */
	private void resolveProfile(final String id)
	{
        for (final Object key : this.keySet()) {
            Matcher matcher = PROFILE_KEY_PATTERN.matcher(key.toString());
            if (matcher.matches() && matcher.group(1).equals(id)) {
            	this.put(matcher.group(2), this.get(key).toString().trim());
            }
        }
	}

	/**
	 * replace keyword in configuration values
	 * 
	 * <p>replaces keywords written in values with <code>${...}</code> pattern.
	 * replaceable keywords are the key of loaded configuration key or 
	 * system property key.</p>
	 * 
	 * <p>loaded configuration has higher priority than system property.</p>
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
                String r = null;
            	if (this.containsKey(jp)) {
            		if (key.toString().trim().equals(jp)) {
            			Logger.fatal("Keyword must not be itself-loop, exit. (%s=%s)", key, value);
            			throw new ConfigurationException("Self keyword replacement loop occurred.");
            		}
            		r = getProperty(jp);
            		
            	} else {
            		Logger.debug("finding system property %s", jp);
            		r = System.getProperty(jp);
            		
            	}
                if (r == null) {
                    Logger.warn("Cannot replace %s in configuration (%s=%s)", jp, key, value);
                    continue;
                }
                matcher.appendReplacement(newValue, r.replaceAll("\\\\", "\\\\\\\\"));
            }
            matcher.appendTail(newValue);
            setProperty(key.toString(), newValue.toString());
        }
	}
	
	/**
	 * create POJO instance having configuration values in their fields
	 * 
	 * @param Instance of the bean class.
	 * @param Class of having values written in configuration file.
	 * @return An instance of class <code>cls</code> has field with value written in configuration file.<br/>
	 *         return null if failed to create instance of class <code>cls</code>.
	 */
	@SuppressWarnings("unchecked")
	public <T> T toBean(T object, final Class<T> cls)
	{
		return (T)toBean(object);
	}
	
	/**
	 * create POJO instance having configuration values in their fields
	 * 
	 * @param Class of having values written in configuration file.
	 * @return An instance has field with value written in configuration file.<br/>
	 *         return null if failed to create instance of class <code>cls</code>.
	 */
	public Object toBean(Object object)
	{
		final Class<?> cls = object.getClass();
		Configuration target = this.hidePrefix();
		for (final Object key : target.keySet()) {
			if (key.toString().contains(".")) {
				// skip recursive definition
				continue;
			}
			
			// setter
			final String setterName = toSetterName(key.toString());
			try {
				cls.getMethod(setterName, String.class).invoke(object,
						target.getProperty(key.toString()));

			} catch (NoSuchMethodException e) {
				play.Logger.debug("%s %s", e.toString(), e.getMessage());

			} catch (InvocationTargetException e) {
				play.Logger.debug("%s %s", e.toString(), e.getMessage());

			} catch (IllegalArgumentException e) {
				play.Logger.debug("%s %s", e.toString(), e.getMessage());

			} catch (SecurityException e) {
				play.Logger.debug("%s %s", e.toString(), e.getMessage());

			} catch (IllegalAccessException e) {
				play.Logger.debug("%s %s", e.toString(), e.getMessage());

			}

			// public field
			try {
				// only public field
				cls.getField(key.toString()).set(object, target.getProperty(key.toString()));
				continue;
				
			} catch (NoSuchFieldException e) {
				play.Logger.debug("%s %s", e.toString(), e.getMessage());
				
			} catch (IllegalAccessException e) {
				play.Logger.debug("%s %s", e.toString(), e.getMessage());
			}
		}
		return object;
	}
	
	/**
	 * create setter method name from field name
	 * 
	 * @param name of the field
	 * @return setter method name
	 */
	public static String toSetterName(final String field)
	{
		if (field == null || field.isEmpty()) {
			return null;
		}
		return "set" + field.substring(0, 1).toUpperCase() + field.substring(1);
	}
}
