package play.data.binding;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import play.Logger;
import play.data.binding.types.CalendarBinder;
import play.data.binding.types.DateBinder;
import play.data.binding.types.LocaleBinder;
import play.data.binding.types.SupportedType;
import play.utils.Utils;

/**
 * The DirectBinder know how to bind simple text value to simple java types.
 * It does not operate on complex objects nor List nor Arrays  
 */
class DirectBinder {

    static Map<Class<?>, SupportedType<?>> supportedTypes = new HashMap<Class<?>, SupportedType<?>>();

    static {
        supportedTypes.put(Date.class, new DateBinder());
        supportedTypes.put(Calendar.class, new CalendarBinder());
        supportedTypes.put(Locale.class, new LocaleBinder());
    }

    public static Object directBind(String value, Class<?> clazz, Annotation[] annotations) throws Exception {
        Logger.trace("directBind: value [" + value + "] annotation [" + Utils.toString(annotations) + "] Class [" + clazz + "]");

        if (clazz.equals(String.class)) {
            return value;
        }

        boolean nullOrEmpty = value == null || value.trim().length() == 0;

        if (supportedTypes.containsKey(clazz)) {
            return nullOrEmpty ? null : supportedTypes.get(clazz).bind(annotations, value);
        }

        // int or Integer binding
        if (clazz.getName().equals("int") || clazz.equals(Integer.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0 : null;
            }

            return Integer.parseInt(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // long or Long binding
        if (clazz.getName().equals("long") || clazz.equals(Long.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0l : null;
            }

            return Long.parseLong(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // byte or Byte binding
        if (clazz.getName().equals("byte") || clazz.equals(Byte.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? (byte) 0 : null;
            }

            return Byte.parseByte(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // short or Short binding
        if (clazz.getName().equals("short") || clazz.equals(Short.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? (short) 0 : null;
            }

            return Short.parseShort(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // float or Float binding
        if (clazz.getName().equals("float") || clazz.equals(Float.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0f : null;
            }

            return Float.parseFloat(value);
        }

        // double or Double binding
        if (clazz.getName().equals("double") || clazz.equals(Double.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0d : null;
            }

            return Double.parseDouble(value);
        }

        // BigDecimal binding
        if (clazz.equals(BigDecimal.class)) {
            if (nullOrEmpty) {
                return null;
            }

            return new BigDecimal(value);
        }

        // boolean or Boolean binding
        if (clazz.getName().equals("boolean") || clazz.equals(Boolean.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? false : null;
            }

            if (value.equals("1") || value.toLowerCase().equals("on") || value.toLowerCase().equals("yes")) {
                return true;
            }

            return Boolean.parseBoolean(value);
        }

        return null;
    }
}
