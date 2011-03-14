package play.db;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import play.Play;
import play.exceptions.UnexpectedException;

public interface Model {

    public void _save();
    public void _delete();
    public Object _key();

    public static class Property {

        public String name;
        public Class<?> type;
        public Field field;
        public boolean isSearchable;
        public boolean isMultiple;
        public boolean isRelation;
        public boolean isGenerated;
        public Class<?> relationType;
        public Choices choices;

    }

    public static interface Choices {

        public List<Object> list();

    }

    public static interface Factory {

        public String keyName();
        public Class<?> keyType();
        public Object keyValue(Model m);
        public Model findById(Object id);
        public List<Model> fetch(int offset, int length, String orderBy, String orderDirection, List<String> properties, String keywords, String where);
        public Long count(List<String> properties, String keywords, String where);
        public void deleteAll();
        public List<Model.Property> listProperties();

    }

    public static class Manager {

        public static Model.Factory factoryFor(Class<? extends Model> clazz) {
            if(Model.class.isAssignableFrom(clazz)) {
                Model.Factory factory = Play.pluginCollection.modelFactory(clazz);
                if( factory != null) {
                    return factory;
                }
            }
            throw new UnexpectedException("Model " + clazz.getName() + " is not managed by any plugin");
        }

    }

    public static interface BinaryField {

        public InputStream get();
        public void set(InputStream is, String type);
        public long length();
        public String type();
        public boolean exists();

    }

}
