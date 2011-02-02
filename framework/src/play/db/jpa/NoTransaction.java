package play.db.jpa;


import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to be used on methods telling JPA
 * that it should not create a Transaction at all
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(value={ElementType.METHOD,ElementType.TYPE})
public @interface NoTransaction {

}

