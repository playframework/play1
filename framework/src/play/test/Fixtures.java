package play.test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import org.yaml.snakeyaml.Yaml;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.db.jpa.JPASupport;

public class Fixtures {

    static Pattern keyPattern = Pattern.compile("([^(]+)\\(([^)]+)\\)");
    
    public static int jpql(String query) {
        return JPA.execute(query);
    }
    
    public static void delete(Class type) {
        for(Object o : JPA.getEntityManager().createQuery("select o from "+type.getName()+" o").getResultList()) {
            JPA.getEntityManager().remove(o);
        }
    }

    public static void load(String name) {
        try {
            InputStream is = Play.classloader.getResourceAsStream(name);
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
                        if(idCache.containsKey(type + "-" + id)) {
                            throw new RuntimeException("Cannot load fixture " + name + ", duplicate id '"+id+"' for type "+type);
                        }
                        Map<String, String[]> params = new HashMap();
                        serialize((Map) objects.get(key), "object", params);
                        Class cType = Play.classloader.loadClass(type);
                        resolveDependencies(cType, params, idCache);
                        Object model = JPASupport.create(cType, "object", params);
                        JPA.getEntityManager().persist(model);
                        idCache.put(type + "-" + id, findKey(model));
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class " + e.getMessage() + " was not found", e);
        } catch(RuntimeException e) {
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
                List l = (List)value;
                String[] r = new String[l.size()];
                int i=0;
                for(Object el : l) {
                    r[i++] = el.toString();
                }
                serialized.put(prefix + "." + key.toString(), r);
            } else {
                serialized.put(prefix + "." + key.toString(), new String[]{value.toString()});
            }
        }
    }

    static Object findKey(Object entity) {
        try {
            Class c = entity.getClass();
            while (c.isAnnotationPresent(Entity.class) || c.isAnnotationPresent(MappedSuperclass.class)) {
                for (Field field : c.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Id.class)) {
                        field.setAccessible(true);
                        return field.get(entity);
                    }
                }
                c = c.getSuperclass();
            }
        } catch (Exception e) {
            Logger.error(e, "Error while determining the object @Id for an object of tyoe " + entity.getClass());
        }
        return null;
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
                        if(!idCache.containsKey(id)) {
                            throw new RuntimeException("No previous reference found for object of type "+relation+" with id "+ids[i]);
                        }
                        ids[i] = idCache.get(id).toString();
                    }
                }
            }
        }
    }
}
