package play.data.binding.annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import play.data.binding.SupportedType;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface As {
    
    String value() default "yyyy-MM-dd";
    String separator() default ",";
    Class<? extends SupportedType> implementation() default DEFAULT.class;

    public static final class DEFAULT implements SupportedType {
        public Object bind(Annotation[] annotations, String value) throws Exception {
            throw new UnsupportedOperationException("Not supported.");
        }
    }
}