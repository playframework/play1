package play.test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import org.yaml.snakeyaml.Yaml;
import play.Logger;
import play.Play;
import play.db.DB;
import play.db.DBPlugin;
import play.db.jpa.JPA;
import play.db.jpa.JPASupport;

public class Fixtures {

    static Pattern keyPattern = Pattern.compile("([^(]+)\\(([^)]+)\\)");

    public static int jpql(String query) {
        return JPA.execute(query);
    }

    public static void delete(Class... types) {
        if (getForeignKeyToggleStmt(false) != null) {
            DB.execute(getForeignKeyToggleStmt(false));
        }
        for (Class type : types) {
            JPA.getEntityManager().createQuery("delete from " + type.getName()).executeUpdate();
        }
        if (getForeignKeyToggleStmt(true) != null) {
            DB.execute(getForeignKeyToggleStmt(true));
        }
        JPA.getEntityManager().clear();
    }

    public static void delete(List<Class> classes) {
        Class[] types = new Class[classes.size()];
        for (int i = 0; i < types.length; i++) {
            types[i] = classes.get(i);
        }
        delete(types);
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
            List<String> names = new ArrayList();
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
            if(JPA.isEnabled()) {
                JPA.getEntityManager().clear();
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot delete all table data : " + e.getMessage(), e);
        }
    }

    public static void load(String name) {
        try {
            InputStream is = Play.classloader.getResourceAsStream(name);
            if(is == null) {
                throw new RuntimeException("Cannot load fixture " + name + ", the file was not found");
            }
            Yaml yaml = new Yaml();
            Object o = yaml.load(is);
            if (o instanceof LinkedHashMap) {
                LinkedHashMap objects = (LinkedHashMap) o;
                Map<String, Object> idCache = new HashMap();
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
                        Map<String, String[]> params = new HashMap();
                        serialize((Map) objects.get(key), "object", params);
                        Class cType = Play.classloader.loadClass(type);
                        resolveDependencies(cType, params, idCache);
                        Object model = JPASupport.create(cType, "object", params);
                        JPA.getEntityManager().persist(model);
                        idCache.put(type + "-" + id, JPASupport.findKey(model));
                    }
                }
            }
            JPA.getEntityManager().clear();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class " + e.getMessage() + " was not found", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Cannot load fixture " + name, e);
        }
    }

    static void serialize(Map values, String prefix, Map<String, String[]> serialized) {
        for (Object key : values.keySet()) {
            Object value = values.get(key);
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
            } else {
                serialized.put(prefix + "." + key.toString(), new String[]{value.toString()});
            }
        }
    }
    
    static void resolveDependencies(Class type, Map<String, String[]> serialized, Map<String, Object> idCache) {
        for (Field field : type.getDeclaredFields()) {
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
            }
        }
    }
}
