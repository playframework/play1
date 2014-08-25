/**
 * 
 */
package cn.bran.play;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * to indicate a class is not to be enhanced by play runtime. 
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface NoEnhance {

}
