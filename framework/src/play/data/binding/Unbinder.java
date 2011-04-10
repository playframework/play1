package play.data.binding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import play.Logger;
import play.Play;
import play.data.binding.types.DateBinder;
import play.libs.I18N;
import play.utils.Utils;

/**
 * Try to unbind an object to a Map<String,String>
 */
public class Unbinder {

    public static void unBind(Map<String, Object> result, Object src, String name, Annotation[] annotations) {
        if (src == null) {
            return;
        }
        if (src instanceof  Class) {
            return ;
        }
        unBind(result, src, src.getClass(), name, annotations);
    }

    private static void unBind(Map<String, Object> result, Object src, Class<?> srcClazz, String name, Annotation[] annotations) {
        Map<String, Object> r = Play.pluginCollection.unBind(src, name);
        if (r != null) {
            result.putAll(r);
            return;
        }

        if (isDirect(srcClazz) || src == null) {
            if (!result.containsKey(name)) {
                result.put(name, src != null ? src.toString() : null);
            } else {
                @SuppressWarnings("unchecked")
                List<Object> objects = (List<Object>) result.get(name);
                objects.add(src != null ? src.toString() : null);
            }
        } else if (src.getClass().isArray()) {
            List<Object> objects = new ArrayList<Object>();
            result.put(name, objects);
            Class<?> clazz = src.getClass().getComponentType();
            int size = Array.getLength(src);
            for (int i = 0; i < size; i++) {
                unBind(result, Array.get(src, i), clazz, name, annotations);
            }
        } else if (Collection.class.isAssignableFrom(src.getClass())) {
            if (Map.class.isAssignableFrom(src.getClass())) {
                throw new UnsupportedOperationException("Unbind won't work with maps yet");
            } else {
                Collection<?> c = (Collection<?>) src;
                List<Object> objects = new ArrayList<Object>();
                result.put(name, objects);
                for (Object object : c) {
                    unBind(result, object, object.getClass(), name, annotations);
                }
            }
        } else if (Date.class.isAssignableFrom(src.getClass()) || Calendar.class.isAssignableFrom(src.getClass())) {
            // We should use the @As annotation if there is one
            // Get the date format from the controller
            boolean isAsAnnotation = false;
            try {
                if (annotations != null) {
                    for (Annotation annotation : annotations) {
                        if (annotation.annotationType().equals(As.class)) {
                            if (Calendar.class.isAssignableFrom(src.getClass())) {
                                result.put(name, new SimpleDateFormat(((As) annotation).value()[0]).format(((Calendar) src).getTime()));
                            } else {
                                result.put(name, new SimpleDateFormat(((As) annotation).value()[0]).format((Date) src));
                            }
                            isAsAnnotation = true;
                        }
                    }
                }

            } catch (Exception e) {
                // Ignore
            }


            if (!isAsAnnotation) {
                // We want to use that one so when redirecting it looks ok. We could as well use the DateBinder.ISO8601 but the url looks terrible
                if (Calendar.class.isAssignableFrom(src.getClass())) {
                    result.put(name, new SimpleDateFormat(I18N.getDateFormat()).format(((Calendar) src).getTime()));
                } else {
                    result.put(name, new SimpleDateFormat(I18N.getDateFormat()).format((Date) src));
                }
            }
        } else {
            Field[] fields = src.getClass().getDeclaredFields();
            for (Field field : fields) {

                if ((field.getModifiers() & BeanWrapper.notwritableField) != 0) {
                    // skip fields that cannot be bound by BeanWrapper
                    continue;
                }

                String newName = name + "." + field.getName();
                boolean oldAcc = field.isAccessible();
                field.setAccessible(true);
                try {
                    unBind(result, field.get(src), field.getType(), newName, annotations);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Object" + src.getClass() + " won't unbind field " + field.getName(), e);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Object" + src.getClass() + " won't unbind field " + field.getName(), e);
                }
                field.setAccessible(oldAcc);
            }
        }
    }

    public static boolean isDirect(Class<?> clazz) {
        return clazz.equals(String.class) || clazz.equals(Integer.class) || Enum.class.isAssignableFrom(clazz) || clazz.equals(Boolean.class) || clazz.equals(Long.class) || clazz.equals(Double.class) || clazz.equals(Float.class) || clazz.equals(Short.class) || clazz.equals(BigDecimal.class) || clazz.isPrimitive() || clazz.equals(Class.class);
    }
}
