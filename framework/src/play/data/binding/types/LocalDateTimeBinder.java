package play.data.binding.types;

import play.data.binding.TypeBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;

public class LocalDateTimeBinder implements TypeBinder<LocalDateTime> {

    @Override
    public LocalDateTime bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) {
        return value != null && !value.trim().isEmpty() ? LocalDateTime.parse(value) : null;
    }
}
