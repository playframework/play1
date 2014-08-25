/**
 * 
 */
package cn.bran.play;

/**
 * @author bran
 * 
 */
public class NoCopyStringBuilder {
	private final static int defaultSize = 1000;
	private int endp = 0;
	private int capacity = defaultSize;
	private Object[] buffer;
	private int upper; 
	
	public NoCopyStringBuilder() {
		this(defaultSize);
	}
	
	public NoCopyStringBuilder(int cap) {
		capacity = (cap >= 2 ? cap : 2);
		buffer = new Object[capacity];
		upper = capacity - 1;
	}
	
	public NoCopyStringBuilder append(String o) {
		if (endp == upper)
			((NoCopyStringBuilder) buffer[endp]).append(o);
		else {
			buffer[endp++] = o;
			if (endp == upper) {
				buffer[endp] = new NoCopyStringBuilder();
			}
		}
		return this;
	}
	
	// XXX seems I cannot avoid copying the data twice, once to the StringBuilder, another to the new String
	// the ctors for string always copy to remain immutable
//	
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//	}
}
