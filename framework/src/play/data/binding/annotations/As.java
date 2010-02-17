package play.data.binding.annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import play.data.binding.SupportedType;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface As {

    String[] value();
    String[] lang() default {"*"};
    String[] format() default {"html"};
    Class<? extends SupportedType> binder() default DEFAULT.class;

    public static final class DEFAULT implements SupportedType {
        public Object bind(Annotation[] annotations, String value) throws Exception {
            throw new UnsupportedOperationException("Not supported.");
        }
    }
}