package play.test;

import java.io.IOException;
import java.io.InputStream;
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
            classes.add((Class<? extends Model>) c.javaClass);
        }
        Fixtures.delete(classes);
    }

    private static void disableForeignKeyConstraints() {
        if (DBPlugin.url.startsWith("jdbc:oracle:")) {
            DB
                    .execute("begin\n"
                            + "for i in (select constraint_name, table_name from user_constraints where constraint_type ='R'\n"
                            + "and status = 'ENABLED') LOOP\n"
                            + "execute immediate 'alter table '||i.table_name||' disable constraint '||i.constraint_name||'';\n"
                            + "end loop;\n" + "end;");
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
        Logger.warn("Fixtures : unable to disable constraints, unsupported database : "
                + DBPlugin.url);
    }

    private static void enableForeignKeyConstraints() {
        if (DBPlugin.url.startsWith("jdbc:oracle:")) {
            DB
                    .execute("begin\n"
                            + "for i in (select constraint_name, table_name from user_constraints where constraint_type ='R'\n"
                            + "and status = 'DISABLED') LOOP\n"
                            + "execute immediate 'alter table '||i.table_name||' enable constraint '||i.constraint_name||'';\n"
                            + "end loop;\n" + "end;");
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
        Logger.warn("Fixtures : unable to enable constraints, unsupported database : "
                + DBPlugin.url);
    }

    static String getDeleteTableStmt(String name) {
        if (DBPlugin.url.startsWith("jdbc:mysql:")) {
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
            ResultSet rs = DB.getConnection().getMetaData().getTables(null, null, null,
                    new String[] { "TABLE" });
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
            for (PlayPlugin plugin : Play.plugins) {
                plugin.afterFixtureLoad();
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot delete all table data : " + e.getMessage(), e);
        }
    }

    private static class UnresolvedField {
        public Model object;
        public List<Model.Property> fields;
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
                throw new RuntimeException("Cannot load fixture " + name
                        + ", the file was not found");
            }
            Yaml yaml = new Yaml();
            Object o = yaml.load(is);
            if (o instanceof LinkedHashMap<?, ?>) {
                @SuppressWarnings("unchecked")
                LinkedHashMap<Object, Map<?, ?>> objects = (LinkedHashMap<Object, Map<?, ?>>) o;
                loadObjects(name, objects);
            }
            // Most persistence engine will need to clear their state
            for (PlayPlugin plugin : Play.plugins) {
                plugin.afterFixtureLoad();
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class " + e.getMessage() + " was not found", e);
        } catch (ScannerException e) {
            throw new YAMLException(e, yamlFile);
        } catch (Throwable e) {
            throw new RuntimeException("Cannot load fixture " + name + ": " + e.getMessage(), e);
        }
    }

    private static void loadObjects(String fileName, LinkedHashMap<Object, Map<?, ?>> objects)
            throws ClassNotFoundException, IllegalAccessException {

        Map<String, List<String>> resolved = new HashMap<String, List<String>>();
        Map<String, UnresolvedField> unresolved = new HashMap<String, UnresolvedField>();
        Map<String, Object> idCache = new HashMap<String, Object>();

        for (Object key : objects.keySet()) {
            Matcher matcher = keyPattern.matcher(key.toString().trim());
            if (matcher.matches()) {
                // eg Farmer
                String type = matcher.group(1);
                // eg bob
                String id = matcher.group(2);
                if (!type.startsWith("models.")) {
                    type = "models." + type;
                }

                // eg Farmer-bob
                String idCacheKey = type + "-" + id;
                if (idCache.containsKey(idCacheKey)) {
                    throw new RuntimeException("Cannot load fixture " + fileName
                            + ", duplicate id '" + id + "' for type " + type);
                }

                if (objects.get(key) == null) {
                    objects.put(key, new HashMap<Object, Object>());
                }

                //
                // Convert the YAML object fields into serialized
                // strings
                // eg
                // {
                // object.name => ["Bob"],
                // object.fruits => ["pear", "apple", ...]
                // object.created => ["ISO8601:2010-10-01T12:56:32Z"]
                // ...
                // }
                //
                Map<String, String[]> params = new HashMap<String, String[]>();
                serialize(objects.get(key), "object", params);

                //
                // Convert YAML ids into Model ids
                // eg
                // object.fruits.id => [7, 12]
                //
                @SuppressWarnings("unchecked")
                Class<Model> cType = (Class<Model>) Play.classloader.loadClass(type);
                List<Property> unresolvedFields = new ArrayList<Property>();
                resolveDependencies(idCacheKey, cType, params, idCache, resolved, unresolvedFields);

                // Create the model object using Play's binding mechanism
                Model model = (Model) Binder.bind("object", cType, cType, null, params);
                for (Field f : model.getClass().getFields()) {
                    // TODO: handle something like FileAttachment
                    if (f.getType().isAssignableFrom(Map.class)) {
                        f.set(model, objects.get(key).get(f.getName()));
                    }

                }
                model._save();

                // Keep track of unresolved object fields so that we can resolve
                // them later
                if (unresolvedFields.size() > 0) {
                    UnresolvedField unresolvedRef = new UnresolvedField();
                    unresolvedRef.object = model;
                    unresolvedRef.fields = unresolvedFields;
                    unresolved.put(idCacheKey, unresolvedRef);
                }

                // Now that the model has been created, add its id to the id
                // cache
                // eg { Farmer-bob => 7, Farmer-jim => 9 ... }
                Class<?> tType = cType;
                while (!tType.equals(Object.class)) {
                    idCache.put(tType.getName() + "-" + id, Model.Manager.factoryFor(cType)
                            .keyValue(model));
                    tType = tType.getSuperclass();
                }
            }
        }

        // Resolve any references that could not be resolved in the first
        // pass
        String objName = "object";
        for (String idCacheKey : unresolved.keySet()) {
            UnresolvedField ref = unresolved.get(idCacheKey);
            Map<String, String[]> bindParams = new HashMap<String, String[]>();
            for (Property field : ref.fields) {
                // eg Farmer->Fruit-pear
                String refTarget = field.relationType.getName() + "->" + idCacheKey;

                // Convert from YAML ids to Model ids
                List<String> yamlIds = resolved.get(refTarget);
                if (yamlIds != null) {
                    String[] modelIds = new String[yamlIds.size()];
                    for (int i = 0; i < yamlIds.size(); i++) {
                        modelIds[i] = idCache.get(yamlIds.get(i)).toString();
                    }

                    // Use play's binding mechanism to resolve the model objects
                    String fieldName = objName + "." + field.name;
                    String idFieldName = getIdFieldName(field, fieldName);

                    // eg object.farmers=[5, 7]
                    bindParams.put(idFieldName, modelIds);
                }
            }

            Binder.bind(ref.object, objName, bindParams);
            ref.object._save();
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
                serialized.put(prefix + "." + key.toString(), new String[] { new SimpleDateFormat(
                        DateBinder.ISO8601).format(((Date) value)) });
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
                    serialized.put(prefix + "." + key.toString(), new String[] { f
                            .contentAsString() });
                }
            } else {
                serialized.put(prefix + "." + key.toString(), new String[] { value.toString() });
            }
        }
    }

    static void resolveDependencies(String idCacheKey, Class<Model> type,
            Map<String, String[]> serialized, Map<String, Object> idCache,
            Map<String, List<String>> resolved, List<Property> unresolvedDeps) {
        for (Model.Property field : Model.Manager.factoryFor(type).listProperties()) {
            if (field.isRelation) {
                String fieldName = "object." + field.name;
                String[] modelIds = null;
                String[] yamlIds = serialized.get(fieldName);

                if (yamlIds != null) {
                    modelIds = new String[yamlIds.length];

                    // Convert from YAML id into model id
                    // eg from ["bob", "jim"] into ["7", "9"]
                    for (int i = 0; i < yamlIds.length; i++) {
                        String yamlId = yamlIds[i];
                        String relatedIdCacheKey = field.relationType.getName() + "-" + yamlId;
                        if (!idCache.containsKey(relatedIdCacheKey)) {
                            throw new RuntimeException(
                                    "No previous reference found for object of type " + field.name
                                            + " with key " + yamlId);
                        }

                        modelIds[i] = idCache.get(relatedIdCacheKey).toString();

                        // Keep track of which source objects reference which
                        // target objects
                        // eg Fruit-pear is referred to by Farmer-bob and
                        // Farmer-jim

                        // eg "Farmer->Fruit-pear"
                        String refTarget = type.getName() + "->" + relatedIdCacheKey;
                        List<String> refSources = resolved.get(refTarget);
                        if (refSources == null) {
                            refSources = new ArrayList<String>();
                            resolved.put(refTarget, refSources);
                        }
                        // eg
                        // {
                        // "Farmer->Fruit-pear" => ["Farmer-bob", "Farmer-jim"],
                        // "Farmer->Fruit-kiwi" => ["Farmer-jim", "Farmer-dan"],
                        // ...
                        // }
                        refSources.add(idCacheKey);
                    }

                } else {
                    // We need to save the field as being unresolved so we can
                    // resolve it later once the model object has been created
                    unresolvedDeps.add(field);
                }
                serialized.remove(fieldName);
                serialized.put(getIdFieldName(field, fieldName), modelIds);
            }
        }
    }

    private static String getIdFieldName(Model.Property field, String fieldName) {
        return fieldName + "."
                + Model.Manager.factoryFor((Class<? extends Model>) field.relationType).keyName();
    }

    public static void deleteDirectory(String path) {
        try {
            FileUtils.deleteDirectory(Play.getFile(path));
        } catch (IOException ex) {
            throw new UnexpectedException(ex);
        }
    }

}
