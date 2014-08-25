/**
 * Copyright 2010 Bing Ran<bing_ran@hotmail.com> 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package cn.bran.japid.tags;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Iterator;

/**
 * a built-in tag for looping, with helping attributes such as, index odd/even
 * line, isFirst, isLast, etc. Since we know this tag does not have a layout,
 * the super's layout->doLayout pathway is short-cut in the render().
 * 
 * @author Bing Ran<bing_ran@hotmail.com>
 * 
 */
public class Each extends SimpleTag {
	public static final String sourceTemplate = "tag/Each.html";

	public Each(StringBuilder out) {
		super(out);
	}

	public void render(Iterable it, DoBody body) {
		Iterator itor = it.iterator();
		itBody(body, itor, -1);
	}

	public void render(Collection collection, DoBody body) {
		Iterator itor = collection.iterator();
		itBody(body, itor, collection.size());
	}
	
	public void render(Iterator it, DoBody body) {
		itBody(body, it, -1);
	}

	public void render(Object[] it, DoBody body) {
		int start = 0;
		int i = 0;
		for (Object o : it) {
			i++;
			boolean isOdd = i % 2 == 1;
			try {
				body.render(o, it.length, i, isOdd, isOdd ? "odd" : "even", i == start + 1, i == it.length);
			} 
			catch (BreakLoop e) {
				break;
			}
			catch (ContinueLoop e) {
				continue;
			}
		}
	}

	public void render(int[] it, DoBody body) {
		int start = 0;
		int i = 0;
		for (int  o : it) {
			i++;
			boolean isOdd = i % 2 == 1;
			try {
				body.render(o, it.length, i, isOdd, isOdd ? "odd" : "even", i == start + 1, i == it.length);
			} 
			catch (BreakLoop e) {
				break;
			}
			catch (ContinueLoop e) {
				continue;
			}
		}
	}
	
	public void render(float[] it, DoBody body) {
		int start = 0;
		int i = 0;
		for (float o : it) {
			i++;
			boolean isOdd = i % 2 == 1;
			try {
				body.render(o, it.length, i, isOdd, isOdd ? "odd" : "even", i == start + 1, i == it.length);
			} 
			catch (BreakLoop e) {
				break;
			}
			catch (ContinueLoop e) {
				continue;
			}
		}
	}
	
	public void render(long[] it, DoBody body) {
		int start = 0;
		int i = 0;
		for (long o : it) {
			i++;
			boolean isOdd = i % 2 == 1;
			try {
				body.render(o, it.length, i, isOdd, isOdd ? "odd" : "even", i == start + 1, i == it.length);
			} 
			catch (BreakLoop e) {
				break;
			}
			catch (ContinueLoop e) {
				continue;
			}
		}
	}
	
	public void render(double[] it, DoBody body) {
		int start = 0;
		int i = 0;
		for (double o : it) {
			i++;
			boolean isOdd = i % 2 == 1;
			try {
				body.render(o, it.length, i, isOdd, isOdd ? "odd" : "even", i == start + 1, i == it.length);
			} 
			catch (BreakLoop e) {
				break;
			}
			catch (ContinueLoop e) {
				continue;
			}
		}
	}
	
	public void render(char[] it, DoBody body) {
		int start = 0;
		int i = 0;
		for (char o : it) {
			i++;
			boolean isOdd = i % 2 == 1;
			try {
				body.render(o, it.length, i, isOdd, isOdd ? "odd" : "even", i == start + 1, i == it.length);
			} 
			catch (BreakLoop e) {
				break;
			}
			catch (ContinueLoop e) {
				continue;
			}
		}
	}
	
	public void render(boolean[] it, DoBody body) {
		int start = 0;
		int i = 0;
		for (boolean o : it) {
			i++;
			boolean isOdd = i % 2 == 1;
			try {
				body.render(o, it.length, i, isOdd, isOdd ? "odd" : "even", i == start + 1, i == it.length);
			} 
			catch (BreakLoop e) {
				break;
			}
			catch (ContinueLoop e) {
				continue;
			}
		}
	}
	

	// TODO: more polymorphic renders

	/**
	 * @param body
	 * @param it
	 */
	private void itBody(DoBody body, Iterator it, int size) {
		int start = 0;
		int i = 0;
		while (it.hasNext()) {
			i++;
			Object o = it.next();
			boolean isOdd = i % 2 == 1;
			try {
				body.render(o, size, i, isOdd, isOdd ? "odd" : "even", i == start + 1, !it.hasNext());
			} 
			catch (BreakLoop e) {
				break;
			}
			catch (ContinueLoop e) {
				continue;
			}
		}
	}

	public static interface DoBody<E> {
		// the _parity is for quick use as a CSS class, or one can use
		// "${_isOdd?"odd":"even"}"
		void render(final E e, final int _size, final int _index, final boolean _isOdd, final String _parity, final boolean _isFirst, final boolean _isLast);
		void setBuffer(StringBuilder sb);
		void resetBuffer();

	}
	
	/**
	 * used to signal to break from the each loop
	 * @author bran
	 *
	 */
	public static class BreakLoop extends RuntimeException {}
	
	/**
	 * used to signal to continue wit the next iteration of the loop 
	 * @author bran
	 *
	 */
	public static class ContinueLoop extends RuntimeException {}
}
