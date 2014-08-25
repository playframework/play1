/**
 * 
 */
package utils;

import org.junit.Test;

/**
 * @author bran
 *
 */
public class ForFor {
	@Test
	public void testForinFor() {
		new Runnable() {public void run() {
				foo();
				int size = 0;
				new Runnable() {public void run() {
						foo();
						int size = 0;
				}}.run();
		}}.run();
	}
	
	void foo() {}
}
