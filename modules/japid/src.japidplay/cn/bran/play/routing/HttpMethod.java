/**
 * 
 */
package cn.bran.play.routing;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author bran
 *
 */
public interface HttpMethod {
	@java.lang.annotation.Target(value={METHOD})
	@java.lang.annotation.Retention(value=RUNTIME)
	public static  abstract @interface GET {String value() default "GET";}

	@java.lang.annotation.Target(value={METHOD})
	@java.lang.annotation.Retention(value=RUNTIME)
	public static  abstract @interface POST {String value() default "POST";}
	
	@java.lang.annotation.Target(value={METHOD})
	@java.lang.annotation.Retention(value=RUNTIME)
	public static  abstract @interface DELETE {String value() default "DELETE";}
	
	@java.lang.annotation.Target(value={METHOD})
	@java.lang.annotation.Retention(value=RUNTIME)
	public static  abstract @interface PUT {String value() default "PUT";}
	
	@java.lang.annotation.Target(value={METHOD})
	@java.lang.annotation.Retention(value=RUNTIME)
	public static  abstract @interface HEAD {String value() default "HEAD";}
	
	@java.lang.annotation.Target(value={METHOD})
	@java.lang.annotation.Retention(value=RUNTIME)
	public static  abstract @interface OPTIONS {String value() default "OPTIONS";}
}
