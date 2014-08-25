package cn.bran.japid.util;

import java.io.IOException;
import java.io.Writer;

/**
 * Copyright (c) 2000-2009 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * <a href="StringBundler.java.html"><b><i>View Source</i></b></a>
 * 
 * <p>
 * See http://issues.liferay.com/browse/LPS-6072.
 * </p>
 * 
 * @author Shuyang Zhou
 * @author Brian Wing Shun Chan
 */
public class StringBundler {

	public StringBundler() {
		_array = new String[_DEFAULT_ARRAY_CAPACITY];
	}

	public StringBundler(int initialCapacity) {
		if (initialCapacity <= 0) {
			throw new IllegalArgumentException();
		}

		_array = new String[initialCapacity];
	}

	public StringBundler(String s) {
		_array = new String[_DEFAULT_ARRAY_CAPACITY];

		_array[0] = s;

		_arrayIndex = 1;
	}

	public StringBundler append(boolean b) {
		if (b) {
			return append(_TRUE);
		} else {
			return append(_FALSE);
		}
	}

	public StringBundler append(double d) {
		return append(Double.toString(d));
	}

	public StringBundler append(float f) {
		return append(Float.toString(f));
	}

	public StringBundler append(int i) {
		return append(Integer.toString(i));
	}

	public StringBundler append(long l) {
		return append(Long.toString(l));
	}

	public StringBundler append(Object obj) {
		return append(String.valueOf(obj));
	}

	public StringBundler append(String s) {
		if (s == null) {
			// bran: don't want to see the null
			return this;
			// s = "null";
		}

		if (_arrayIndex >= _array.length) {
			expandCapacity();
		}

		_array[_arrayIndex++] = s;

		return this;
	}

	public int capacity() {
		return _array.length;
	}

	public int index() {
		return _arrayIndex;
	}

	public void setIndex(int newIndex) {
		if (newIndex < 0) {
			throw new ArrayIndexOutOfBoundsException(newIndex);
		}

		if (newIndex > _array.length) {
			String[] newArray = new String[newIndex];

			System.arraycopy(_array, 0, newArray, 0, _arrayIndex);

			_array = newArray;
		}

		if (_arrayIndex < newIndex) {
			for (int i = _arrayIndex; i < newIndex; i++) {
				_array[i] = "";
			}
		}

		if (_arrayIndex > newIndex) {
			for (int i = newIndex; i < _arrayIndex; i++) {
				_array[i] = null;
			}
		}

		_arrayIndex = newIndex;
	}

	public String stringAt(int index) {
		if (index >= _arrayIndex) {
			throw new ArrayIndexOutOfBoundsException();
		}

		return _array[index];
	}

	public String toString() {
		if (_arrayIndex == 0) {
			return "";
		}

		String s = null;

		if (_arrayIndex <= 3) {
			s = _array[0];

			for (int i = 1; i < _arrayIndex; i++) {
				s = s.concat(_array[i]);
			}
		} else {
			int length = 0;

			for (int i = 0; i < _arrayIndex; i++) {
				length += _array[i].length();
			}

			StringBuilder sb = new StringBuilder(length);

			for (int i = 0; i < _arrayIndex; i++) {
				sb.append(_array[i]);
			}

			s = sb.toString();
		}

		return s;
	}

	public void print(Writer w) throws IOException {
		if (_arrayIndex == 0) {
			return;
		}

		for (int i = 0; i < _arrayIndex; i++) {
			w.write(_array[i]);
		}
	}

	protected void expandCapacity() {
		String[] newArray = new String[_array.length << 1];

		System.arraycopy(_array, 0, newArray, 0, _arrayIndex);

		_array = newArray;
	}

	private static final int _DEFAULT_ARRAY_CAPACITY = 16;

	private static final String _FALSE = "false";

	private static final String _TRUE = "true";

	private String[] _array;
	private int _arrayIndex;

}