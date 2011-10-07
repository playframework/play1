package play.db;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.Modifier;
import java.lang.annotation.Annotation;
import java.beans.PropertyDescriptor;
import java.util.List;
import play.Play;
import play.exceptions.UnexpectedException;

public interface Model {

    public void _save();
    public void _delete();
    public Object _key();

    public static abstract class Property {

        public String name;
        public Class<?> type;
        public boolean isSearchable;
        public boolean isMultiple;
        public boolean isRelation;
        public boolean isGenerated;
        public Class<?> relationType;
        public Choices choices;

        public abstract Class<?> getType();

        public abstract Type getGenericType();

        public abstract boolean isAnnotationPresent(Class<? extends Annotation> annotation);

        public abstract <T extends Annotation> T getAnnotation(Class<T> annotation);

        public abstract int getModifiers();

        public abstract String getName();
    }

    public static class FieldProperty extends Property {
        public Field field;

        @Override
        public Class<?> getType() {
            return field.getType();
        }

        @Override
        public Type getGenericType() {
            return field.getGenericType();
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
            return field.isAnnotationPresent(annotation);
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotation) {
            return field.getAnnotation(annotation);
        }
    
        @Override
        public int getModifiers() {
            return field.getModifiers();
        }

        @Override
        public String getName() {
            return field.getName();
        }

        public FieldProperty(Field field) {
            this.field = field;
        }
    }

    public static class AccessorProperty extends Property {
        public PropertyDescriptor accessor;

        @Override
        public Class<?> getType() {
            return accessor.getPropertyType();
        }

        @Override
        public Type getGenericType() {
            Method readMethod = accessor.getReadMethod(), writeMethod = accessor.getWriteMethod();
            if (readMethod != null) {
                return readMethod.getGenericReturnType();
            }
            if (writeMethod != null) {
                Type[] types = writeMethod.getGenericParameterTypes();
                if (types.length == 1) {
                    return types[0];
                }
            }
            throw new UnexpectedException("Neither getter nor setter provides a generic type.");
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotation) {
            Method readMethod = accessor.getReadMethod(), writeMethod = accessor.getWriteMethod();
            if (readMethod != null && readMethod.isAnnotationPresent(annotation)) {
                return true;
            }
            if (writeMethod != null && writeMethod.isAnnotationPresent(annotation)) {
                return true;
            }
            return false;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotation) {
            Method readMethod = accessor.getReadMethod(), writeMethod = accessor.getWriteMethod();
            if (readMethod != null) {
                T retval = readMethod.getAnnotation(annotation);
                if (retval != null) {
                    return retval;
                }
            }
            if (writeMethod != null) {
                T retval = writeMethod.getAnnotation(annotation);
                if (retval != null) {
                    return retval;
                }
            }
            return null;
        }

        private int pppMaskToValue(int pppMask) {
            int retval = 2;
            if (Modifier.isPublic(pppMask)) {
                retval = 0;
            } else if (Modifier.isProtected(pppMask)) {
                retval = 1;
            } else if (Modifier.isPrivate(pppMask)) {
                retval = 3;
            }
            return retval;
        }

        private int pppValueToMask(int pppValue) {
            switch (pppValue) {
            case 0:
                return Modifier.PUBLIC;
            case 1:
                return Modifier.PROTECTED;
            case 2:
                return 0;
            case 3:
                return Modifier.PRIVATE;
            }
            return 0;
        }

        @Override
        public int getModifiers() {
            Method readMethod = accessor.getReadMethod(), writeMethod = accessor.getWriteMethod();
            int readMethodModifiers = Modifier.PRIVATE, writeMethodModifiers = Modifier.PRIVATE;
            if (readMethod != null) {
                readMethodModifiers = readMethod.getModifiers();
            }
            if (writeMethod != null) {
                writeMethodModifiers = writeMethod.getModifiers();
            }
            return pppValueToMask(
                Math.max(pppMaskToValue(readMethodModifiers),
                         pppMaskToValue(writeMethodModifiers)));
        }

        @Override
        public String getName() {
            Method readMethod = accessor.getReadMethod(), writeMethod = accessor.getWriteMethod();
            return accessor.getName();
        }

        public AccessorProperty(PropertyDescriptor accessor) {
            this.accessor = accessor;
        }
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
