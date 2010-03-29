package play.db;

import java.lang.reflect.Field;
import java.util.List;

public interface Model {

    public void _save();
    public void _delete();
    public Object _getKey();
    public Class _getKeyType();

    public void _loader();

    public static interface Loader {

        public Model findById(Object id);
        public List<Model> fetch(int offset, int size, String orderBy, String orderDirection);
        public Long count();
        public List<Model> search(List<String> properties, String keywords, int offset, int size, String orderBy, String orderDirection);
        public Long countSearch(List<String> properties, String keywords);

    }

    public List<Property> _properties();

    public static class Property {

        public String name;
        public Class type;
        public Field field;
        public boolean isRelation;
        public boolean isMultiple;
        public List<Object> choices;

    }

}
