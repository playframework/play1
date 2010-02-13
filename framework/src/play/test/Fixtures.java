package play.test;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.scanner.ScannerException;

import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses;
import play.db.DB;
import play.db.DBPlugin;
import play.db.jpa.FileAttachment;
import play.db.jpa.JPA;
import play.db.jpa.JPASupport;
import play.exceptions.YAMLException;
import play.vfs.VirtualFile;

public class Fixtures {

    static Pattern keyPattern = Pattern.compile("([^(]+)\\(([^)]+)\\)");

    public static void delete(Class... types) {
        if (getForeignKeyToggleStmt(false) != null) {
            DB.execute(getForeignKeyToggleStmt(false));
        }
        for (Class type : types) {
            JPA.em().createQuery("delete from " + type.getName()).executeUpdate();
        }
        if (getForeignKeyToggleStmt(true) != null) {
            DB.execute(getForeignKeyToggleStmt(true));
        }
        JPA.em().clear();
    }

    public static void delete(List<Class> classes) {
        Class[] types = new Class[classes.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = classes.get(i);
        }
        delete(types);
    }

    public static void deleteAllEntities() {
        List<Class> classes = new ArrayList<Class>();
        for (ApplicationClasses.ApplicationClass c :
                Play.classes.getAnnotatedClasses(Entity.class)) {
            classes.add(c.javaClass);
        }
        Fixtures.delete(classes);
    }

    static String getForeignKeyToggleStmt(boolean enable) {
        if (DBPlugin.url.startsWith("jdbc:hsqldb:")) {
            return "SET REFERENTIAL_INTEGRITY " + (enable ? "TRUE" : "FALSE");
        }
        if (DBPlugin.url.startsWith("jdbc:mysql:")) {
            return "SET foreign_key_checks = " + (enable ? "1" : "0") + ";";
        }
        return null;
    }

    static String getDeleteTableStmt(String name) {
        if (DBPlugin.url.startsWith("jdbc:mysql:")) {
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
            if (getForeignKeyToggleStmt(false) != null) {
                DB.execute(getForeignKeyToggleStmt(false));
            }
            for (String name : names) {
                Logger.trace("Dropping content of table %s", name);
                DB.execute(getDeleteTableStmt(name));
            }
            if (getForeignKeyToggleStmt(true) != null) {
                DB.execute(getForeignKeyToggleStmt(true));
            }
            if (JPA.isEnabled()) {
                JPA.em().clear();
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot delete all table data : " + e.getMessage(), e);
        }
    }

    public static void load(String name) {
        VirtualFile yamlFile = null;
        try {
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
            Yaml yaml = new Yaml();
            Object o = yaml.load(is);
            if (o instanceof LinkedHashMap) {
                LinkedHashMap objects = (LinkedHashMap) o;
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
                            throw new RuntimeException("Cannot load fixture " + name + ", duplicate id '" + id + "' for type " + type);
                        }
                        Map<String, String[]> params = new HashMap<String, String[]>();
                        serialize((Map) objects.get(key), "object", params);
                        Class cType = Play.classloader.loadClass(type);
                        resolveDependencies(cType, params, idCache);
                        JPASupport model = JPASupport.create(cType, "object", params, null);
                        for(Field f : model.getClass().getFields()) {
                            if(f.getType().isAssignableFrom(FileAttachment.class)) {
                                String[] value = params.get("object."+f.getName());
                                if(value != null && value.length > 0) {
                                    VirtualFile vf = Play.getVirtualFile(value[0]);
                                    if(vf != null && vf.exists()) {
                                        FileAttachment fa = new FileAttachment();
                                        fa.set(vf.getRealFile());
                                        f.set(model, fa);
                                    }
                                }
                            }
                        }
                        model.save();
                        JPA.em().persist(model);
                        while (!cType.equals(JPASupport.class)) {
                            idCache.put(cType.getName() + "-" + id, JPASupport.findKey(model));
                            cType = cType.getSuperclass();
                        }
                        // Not very good for performance but will avoid outOfMemory
                        JPA.em().flush();
                        JPA.em().clear();
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class " + e.getMessage() + " was not found", e);
        } catch (ScannerException e) {
            throw new YAMLException(e, yamlFile);
        } catch (Throwable e) {
            throw new RuntimeException("Cannot load fixture " + name + ": " + e.getMessage(), e);
        }
    }

    static void serialize(Map values, String prefix, Map<String, String[]> serialized) {
        for (Object key : values.keySet()) {
            Object value = values.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof Map) {
                serialize((Map) value, prefix + "." + key, serialized);
            } else if (value instanceof Date) {
                serialized.put(prefix + "." + key.toString(), new String[]{new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").format(((Date) value))});
            } else if (value instanceof List) {
                List l = (List) value;
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
                if(f != null && f.exists()) {
                    serialized.put(prefix + "." + key.toString(), new String[]{f.contentAsString()});
                }
            } else {
                serialized.put(prefix + "." + key.toString(), new String[]{value.toString()});
            }
        }
    }

    static void resolveDependencies(Class type, Map<String, String[]> serialized, Map<String, Object> idCache) {
        Set<Field> fields = new HashSet<Field>();
        Class clazz = type;
        while (!clazz.equals(JPASupport.class)) {
            Collections.addAll(fields, clazz.getDeclaredFields());
            clazz = clazz.getSuperclass();
        }
        for (Field field : fields) {
            boolean isEntity = false;
            String relation = null;
            if (field.isAnnotationPresent(OneToOne.class) || field.isAnnotationPresent(ManyToOne.class)) {
                isEntity = true;
                relation = field.getType().getName();
            }
            if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class)) {
                Class fieldType = (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                isEntity = true;
                relation = fieldType.getName();
            }
            if (isEntity) {
                String[] ids = serialized.get("object." + field.getName());
                if (ids != null) {
                    for (int i = 0; i < ids.length; i++) {
                        String id = ids[i];
                        id = relation + "-" + id;
                        if (!idCache.containsKey(id)) {
                            throw new RuntimeException("No previous reference found for object of type " + relation + " with id " + ids[i]);
                        }
                        ids[i] = idCache.get(id).toString();
                    }
                }
                serialized.remove("object." + field.getName());
                serialized.put("object." + field.getName() + "@id", ids);
            }
        }
    }

    public static void deleteAttachmentsDir() {
        File atttachmentsDir = FileAttachment.getStore();
        try {
            if (atttachmentsDir.exists()) {
                FileUtils.deleteDirectory(atttachmentsDir);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
