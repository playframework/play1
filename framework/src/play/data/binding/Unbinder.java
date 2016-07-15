package play.data.binding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import play.Play;
import play.libs.I18N;

/**
 * Try to unbind an object to a Map&lt;String,String&gt;
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
    
    private static void directUnbind(Map<String, Object> result, Object src, Class<?> srcClazz, String name, Annotation[] annotations) {
        if (!result.containsKey(name)) {
            result.put(name,  (src != null ? src.toString() : null));
        } 
    }
    
    private static void unbindArray(Map<String, Object> result, Object src, Class<?> srcClazz, String name, Annotation[] annotations) {
        if(src == null){
            directUnbind( result, src,  srcClazz,  name, annotations);
        }else{
            Class<?> clazz = src.getClass().getComponentType();
            int size = Array.getLength(src);
            for (int i = 0; i < size; i++) {
                unBind(result, Array.get(src, i), clazz, name + "[" + i + "]", annotations);
            }
        }
    }
        
    private static void unbindCollection(Map<String, Object> result, Object src, Class<?> srcClazz, String name, Annotation[] annotations) {
        if(src == null){
            directUnbind( result, src,  srcClazz,  name, annotations);
        } else {
            Collection<?> c = (Collection<?>) src;  
            if (Map.class.isAssignableFrom(src.getClass())) {
                throw new UnsupportedOperationException("Unbind won't work with maps yet");
            } else {
                 int i = 0;
                 // We cannot convert it to array, as the class of the array will be object instead of the real object class
                 // Moreover the list could contains different classes (all elements extends from a parent class)
                 for (Object object : c) {
                     unBind(result, object, object.getClass(), name + "[" + (i++) + "]", annotations);
                 }
            } 
        }
    }

    private static void unbindMap(Map<String, Object> result, Object src, Class<?> srcClazz, String name, Annotation[] annotations) {
        if(src == null){
            directUnbind( result, null,  srcClazz,  name, annotations);
        } else {
            Map<?,?> map = (Map<?,?>) src;

            for (Map.Entry<?,?> entry : map.entrySet()) {
                Object key = entry.getKey();
                if (!isDirect(key.getClass())) {
                    throw new UnsupportedOperationException("Unbind won't work with indirect map keys yet");
                }
                String paramKey = name + '.' + key.toString();
                Unbinder.unBind(result, entry.getValue(), paramKey, annotations);
            }
        }
    }

    private static void unbindDate(Map<String, Object> result, Object src, Class<?> srcClazz, String name, Annotation[] annotations) {
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
    }
    
    private static void internalUnbind(Map<String, Object> result, Object src, Class<?> srcClazz, String name, Annotation[] annotations) {             
        // Check for TypeBinder
        boolean isExtendedTypeBinder = false;
        try {
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    if (annotation.annotationType().equals(As.class)) {
                        // Check the unbinder param first
                        Class<? extends TypeUnbinder<?>> toInstantiate = (Class<? extends TypeUnbinder<?>>) ((As) annotation)
                                .unbinder();
                        if (!(toInstantiate.equals(As.DEFAULT.class))) {
                            TypeUnbinder<?> myInstance = toInstantiate.newInstance();
                            isExtendedTypeBinder = myInstance.unBind(result, src, srcClazz, name, annotations);
                        }else{
                            // unbinder is default, test if binder handle the unbinder too
                            Class<? extends TypeBinder<?>> toInstantiateBinder = ((As) annotation).binder();
                            if (!(toInstantiateBinder.equals(As.DEFAULT.class))
                                    && TypeUnbinder.class.isAssignableFrom(toInstantiateBinder)) {
                                TypeUnbinder<?> myInstance = (TypeUnbinder<?>) toInstantiateBinder.newInstance();
                                isExtendedTypeBinder = myInstance.unBind(result, src, srcClazz, name, annotations);
                            }    
                        }             
                    }
                }
            }

            if (!isExtendedTypeBinder) {
                unBind(result, src, srcClazz, name, annotations);
            }
        } catch (Exception e) {
            throw new RuntimeException("Object " + srcClazz + " won't unbind field " + name, e);
        }
    }

    private static void unBind(Map<String, Object> result, Object src, Class<?> srcClazz, String name, Annotation[] annotations) {
        Map<String, Object> r = Play.pluginCollection.unBind(src, name);
        if (r != null) {
            result.putAll(r);
            return;
        }
        
        if (isDirect(srcClazz) || src == null) {
            directUnbind(result, src, srcClazz, name, annotations);           
        }else if (src.getClass().isArray()) {
            unbindArray(result, src, src.getClass(), name, annotations);           
        } else if (Collection.class.isAssignableFrom(src.getClass())) {
            unbindCollection(result, src, src.getClass(), name, annotations);
        } else if (Map.class.isAssignableFrom(src.getClass())) {
            unbindMap(result, src, src.getClass(), name, annotations);
        } else if (Date.class.isAssignableFrom(src.getClass()) || Calendar.class.isAssignableFrom(src.getClass())) {
            unbindDate(result, src, src.getClass(), name, annotations);
        } else{    
            Field[] fields = src.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ((field.getModifiers() & BeanWrapper.notwritableField) != 0) {
                    // skip fields that cannot be bound by BeanWrapper
                    continue;
                }

                String newName = name + "." + field.getName();
                boolean oldAcc = field.isAccessible();
                field.setAccessible(true);

                // first we try with annotations resolved from property
                List<Annotation> allAnnotations = new ArrayList<>();
                if (annotations != null && annotations.length > 0) {
                    allAnnotations.addAll(Arrays.asList(annotations));
                }

                // Add entity field annotation
                Annotation[] propBindingAnnotations = field.getAnnotations();
                if (propBindingAnnotations != null && propBindingAnnotations.length > 0) {
                    allAnnotations.addAll(Arrays.asList(propBindingAnnotations));
                }

                try {
                    internalUnbind(result, field.get(src), field.getType(), newName, allAnnotations.toArray(new Annotation[0]));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException("Object " + field.getType() + " won't unbind field " + newName, e);
                } finally{
                    field.setAccessible(oldAcc);
                }
            }
        }
    }

    public static boolean isDirect(Class<?> clazz) {
        return clazz.equals(String.class) || clazz.equals(Integer.class) || Enum.class.isAssignableFrom(clazz) || clazz.equals(Boolean.class) || clazz.equals(Long.class) || clazz.equals(Double.class) || clazz.equals(Float.class) || clazz.equals(Short.class) || clazz.equals(BigDecimal.class) || clazz.isPrimitive() || clazz.equals(Class.class);
    }
}
