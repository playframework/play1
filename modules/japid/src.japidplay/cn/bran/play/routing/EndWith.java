/**
 * 
 */
package cn.bran.play.routing;

/**
 * 
 * for an example: "/controller/action/p1/p2" would become "/controller/action/p1/p2.html"
 * 
 *  This may help to put on some weight to the url in the eyes of search engines. 
 *  
 *   The default value is ".html". 
 *   
 * @author bran
 *
 */
@java.lang.annotation.Target(value={java.lang.annotation.ElementType.METHOD})
@java.lang.annotation.Retention(value=java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface EndWith {
	String value() default ".html";
}
