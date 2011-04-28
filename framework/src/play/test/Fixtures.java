package play.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.scanner.ScannerException;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.data.binding.Binder;
import play.data.binding.types.DateBinder;
import play.db.DB;
import play.db.DBPlugin;
import play.db.Model;
import play.db.Model.Property;
import play.exceptions.UnexpectedException;
import play.exceptions.YAMLException;
import play.vfs.VirtualFile;

public class Fixtures {

    static Pattern keyPattern = Pattern.compile("([^(]+)\\(([^)]+)\\)");

    public static void delete(Class<? extends Model>... types) {
        disableForeignKeyConstraints();
        for (Class<? extends Model> type : types) {
            Model.Manager.factoryFor(type).deleteAll();
        }
        enableForeignKeyConstraints();
    }

    public static void delete(List<Class<? extends Model>> classes) {
        @SuppressWarnings("unchecked")
        Class<? extends Model>[] types = new Class[classes.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = classes.get(i);
        }
        delete(types);
    }

    @SuppressWarnings("unchecked")
    public static void deleteAllModels() {
        List<Class<? extends Model>> classes = new ArrayList<Class<? extends Model>>();
        for (ApplicationClasses.ApplicationClass c : Play.classes.getAssignableClasses(Model.class)) {
            classes.add((Class<? extends Model>)c.javaClass);
        }
        Fixtures.delete(classes);
    }

    private static void disableForeignKeyConstraints() {
        if (DBPlugin.url.startsWith("jdbc:oracle:")) {
            DB.execute("begin\n" +
                    "for i in (select constraint_name, table_name from user_constraints where constraint_type ='R'\n" +
                    "and status = 'ENABLED') LOOP\n" +
                    "execute immediate 'alter table '||i.table_name||' disable constraint '||i.constraint_name||'';\n" +
                    "end loop;\n" +
                    "end;");
            return;
        }

        if (DBPlugin.url.startsWith("jdbc:hsqldb:")) {
            DB.execute("SET REFERENTIAL_INTEGRITY FALSE");
            return;
        }

        if (DBPlugin.url.startsWith("jdbc:mysql:")) {
            DB.execute("SET foreign_key_checks = 0;");
            return;
        }

        // Maybe Log a WARN for unsupported DB ?
        Logger.warn("Fixtures : unable to disable constraints, unsupported database : " + DBPlugin.url);
    }

    private static void enableForeignKeyConstraints() {
        if (DBPlugin.url.startsWith("jdbc:oracle:")) {
             DB.execute("begin\n" +
                     "for i in (select constraint_name, table_name from user_constraints where constraint_type ='R'\n" +
                     "and status = 'DISABLED') LOOP\n" +
                     "execute immediate 'alter table '||i.table_name||' enable constraint '||i.constraint_name||'';\n" +
                     "end loop;\n" +
                     "end;");
            return;
        }

        if (DBPlugin.url.startsWith("jdbc:hsqldb:")) {
            DB.execute("SET REFERENTIAL_INTEGRITY TRUE");
            return;
        }

        if (DBPlugin.url.startsWith("jdbc:mysql:")) {
            DB.execute("SET foreign_key_checks = 1;");
            return;
        }

        // Maybe Log a WARN for unsupported DB ?
        Logger.warn("Fixtures : unable to enable constraints, unsupported database : " + DBPlugin.url);
    }


    static String getDeleteTableStmt(String name) {
        if (DBPlugin.url.startsWith("jdbc:mysql:") ) {
            return "TRUNCATE TABLE " + name;
        } else if (DBPlugin.url.startsWith("jdbc:postgresql:")) {
            return "TRUNCATE TABLE " + name + " cascade";
        } else if (DBPlugin.url.startsWith("jdbc:oracle:")) {
            return "TRUNCATE TABLE " + name;
        }
        return "DELETE FROM " + name;
    }

    public static void deleteAll() {
        try {
            List<String> names = new ArrayList<String>();
            ResultSet rs = DB.getConnection().getMetaData().getTables(null, null, null, new String[]{"TABLE"});
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                names.add(name);
            }
            disableForeignKeyConstraints();
            for (String name : names) {
                Logger.trace("Dropping content of table %s", name);
                DB.execute(getDeleteTableStmt(name) + ";");
            }
            enableForeignKeyConstraints();
            for(PlayPlugin plugin : Play.plugins) {
                plugin.afterFixtureLoad();
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot delete all table data : " + e.getMessage(), e);
        }
    }

    public static void load(String name){
        VirtualFile yamlFile = null;
        for (VirtualFile vf : Play.javaPath) {
            yamlFile = vf.child(name);
            if (yamlFile != null && yamlFile.exists()) {
                break;
            }
        }
        InputStream is = Play.classloader.getResourceAsStream(name);
        if (is == null) {
            throw new RuntimeException("Cannot load fixture " + name + ", the file was not found");
        }
    	load(new InputStreamReader(is), yamlFile);
    }
    
    public static void load(Reader reader) {
    	load(reader, null);
    }
    
    public static void load(Reader reader, VirtualFile yamlFile) {
        try {
            Yaml yaml = new Yaml();
            Object o = yaml.load(reader);
            if (o instanceof LinkedHashMap<?, ?>) {
                @SuppressWarnings("unchecked") LinkedHashMap<Object, Map<?, ?>> objects = (LinkedHashMap<Object, Map<?, ?>>) o;
                Map<String, Object> idCache = new HashMap<String, Object>();
                for (Object key : objects.keySet()) {
                    Matcher matcher = keyPattern.matcher(key.toString().trim());
                    if (matcher.matches()) {
                        String type = matcher.group(1);
                        String id = matcher.group(2);
                        if (!type.startsWith("models.")) {
                            type = "models." + type;
                        }
                        if (idCache.containsKey(type + "-" + id)) {
                            throw new RuntimeException("Cannot load fixture, duplicate id '" + id + "' for type " + type);
                        }
                        Map<String, String[]> params = new HashMap<String, String[]>();
                        if (objects.get(key) == null) {
                            objects.put(key, new HashMap<Object, Object>());
                        }
                        serialize(objects.get(key), "object", params);
                        @SuppressWarnings("unchecked")
                        Class<Model> cType = (Class<Model>)Play.classloader.loadClass(type);
                        resolveDependencies(cType, params, idCache);
                        Model model = (Model)Binder.bind("object", cType, cType, null, params);
                        for(Field f : model.getClass().getFields()) {
                            // TODO: handle something like FileAttachment
                            if (f.getType().isAssignableFrom(Map.class)) {
                                f.set(model, objects.get(key).get(f.getName()));
                            }

                        }
                        try{
                        	model._save();
                        }catch(Exception x){
                        	throw new UnexpectedException("Failed to load fixture: problem while saving object "+id, x);
                        }
                        Class<?> tType = cType;
                        // FIXME: this is most probably wrong since superclasses might share IDs implemented by disjoint
                        // subclasses. Besides why create the key for each if it's supposed to be the same value???
                        while (!tType.equals(Object.class)) {
                            idCache.put(tType.getName() + "-" + id, Model.Manager.factoryFor(cType).keyValue(model));
                            tType = tType.getSuperclass();
                        }
                    }
                }
            }
            // Most persistence engine will need to clear their state
            for(PlayPlugin plugin : Play.plugins) {
                plugin.afterFixtureLoad();
            }
        }catch (UnexpectedException x){
        	throw x;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class " + e.getMessage() + " was not found", e);
        } catch (ScannerException e) {
        	if(yamlFile != null)
        		throw new YAMLException(e, yamlFile);
        	throw new RuntimeException(e);
        } catch (Throwable e) {
            throw new RuntimeException("Cannot load fixture: " + e.getMessage(), e);
        }
    }

    static void serialize(Map<?, ?> values, String prefix, Map<String, String[]> serialized) {
        for (Object key : values.keySet()) {
            Object value = values.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof Map<?, ?>) {
                serialize((Map<?, ?>) value, prefix + "." + key, serialized);
            } else if (value instanceof Date) {
                serialized.put(prefix + "." + key.toString(), new String[]{new SimpleDateFormat(DateBinder.ISO8601).format(((Date) value))});
            } else if (value instanceof List<?>) {
                List<?> l = (List<?>) value;
                String[] r = new String[l.size()];
                int i = 0;
                for (Object el : l) {
                    r[i++] = el.toString();
                }
                serialized.put(prefix + "." + key.toString(), r);
            } else if (value instanceof String && value.toString().matches("<<<\\s*\\{[^}]+}\\s*")) {
                Matcher m = Pattern.compile("<<<\\s*\\{([^}]+)}\\s*").matcher(value.toString());
                m.find();
                String file = m.group(1);
                VirtualFile f = Play.getVirtualFile(file);
                if (f != null && f.exists()) {
                    serialized.put(prefix + "." + key.toString(), new String[]{f.contentAsString()});
                }
            } else {
                serialized.put(prefix + "." + key.toString(), new String[]{value.toString()});
            }
        }
    }

    protected static void resolveDependencies(Class<? extends Model> type, Map<String, String[]> serialized, Map<String, Object> idCache) throws Exception {
        for (Model.Property field : Model.Manager.factoryFor(type).listProperties()) {
            if (field.isRelation) {
            	String prefix = "object." + field.name;
                String[] ids = serialized.get(prefix);
                Object[] persistedIds = null;
                if (ids != null) {
                	persistedIds = new Object[ids.length];
                    for (int i = 0; i < ids.length; i++) {
                        String id = ids[i];
                        id = field.relationType.getName() + "-" + id;
                        if (!idCache.containsKey(id)) {
                            throw new RuntimeException("No previous reference found for object of type " + field.name + " with key " + ids[i]);
                        }
                        persistedIds[i] = idCache.get(id);
                    }
                }
                serialized.remove(prefix);
                if(persistedIds != null)
                	serializeKey(prefix, field.relationType, persistedIds, serialized, idCache);
             }
        }
    }

    private static void serializeKey(String prefix,
    		Class<?> relationType, Object[] persistedIds, Map<String, String[]> serialized, Map<String, Object> idCache) throws Exception {
        @SuppressWarnings("unchecked")
		List<Model.Property> keys = Model.Manager.factoryFor((Class<? extends Model>) relationType).listKeys();
        // serialise each ID into as many keys
        for(Property key : keys){
        	String fieldName = prefix + "." + key.name; 
        	if(key.isRelation){
        		// get that part of the key
        		Object[] idParts = new Object[persistedIds.length];
        		for (int i = 0; i < idParts.length; i++) {
        			idParts[i] = PropertyUtils.getSimpleProperty(persistedIds[i], key.name);
        		}
        		serializeKey(fieldName, key.relationType, idParts, serialized, idCache);
        	}else{
        		// not composite so it must be serialisable as string
        		String[] ids= new String[persistedIds.length];
        		for (int i = 0; i < ids.length; i++) {
        			ids[i] = persistedIds[i].toString();
        		}
        		serialized.put(fieldName, ids);
        	}
        }
	}

	public static void deleteDirectory(String path) {
        try {
            FileUtils.deleteDirectory(Play.getFile(path));
        } catch (IOException ex) {
            throw new UnexpectedException(ex);
        }
    }

}
