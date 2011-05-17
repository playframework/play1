package play.db;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

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
        public boolean isKey;
        public Class<?> relationType;
        public Choices choices;

    }

    public static interface Choices {

        public List<Object> list();

    }

    public static interface Factory {

    	/**
    	 * Returns the list of properties for this factory's type.
    	 */
    	public List<Model.Property> listProperties();
    	/**
    	 * Returns the list of key properties for this factory's type.
    	 */
        public List<Model.Property> listKeys();
        /**
         * @deprecated this only works for single non-composite keys. It will throw an exception in every other case. Use listKeys().
         */
        @Deprecated
        public String keyName();
        /**
         * Returns the type of key. For a single key it will be the key's field's type. For a composite key this will be the type
         * of the composite key (as specified by @IdClass)
         */
        public Class<?> keyType();
        /**
         * Returns the key value. For a single key it will return the key's field. For a composite key this will return an instance of the type
         * of the composite key (as specified by @IdClass)
         */
        public Object keyValue(Model m);
        /**
         * Makes a key valid for this factory's type, with all the given components of this key taken from a map.
         */
        public Object makeKey(Map<String, Object> id);
        public Model findById(Object id);
        public List<Model> fetch(int offset, int length, String orderBy, String orderDirection, List<String> properties, String keywords, String where);
        public Long count(List<String> properties, String keywords, String where);
        public void deleteAll();
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
