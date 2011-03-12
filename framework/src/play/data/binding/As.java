package play.data.binding;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface As {

    String[] value() default {""};
    String[] lang() default {"*"};
    String[] format() default {"html"};
    Class<? extends TypeBinder<?>> binder() default DEFAULT.class;

    public static final class DEFAULT implements TypeBinder<Object> {
        public Object bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) throws Exception {
            throw new UnsupportedOperationException("Not supported.");
        }
    }
    
}
