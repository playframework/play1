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
package cn.bran.japid.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * basically
 * 
 * @author bran
 * 
 */
public class StringUtils {

	static CharsetEncoder ce;
	static {
		Charset cs = Charset.forName("UTF-8");
		ce = cs.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
	}

	/**
	 * try to encode a char[] as fast as possible for later use in outputstream
	 * 
	 * @param ca
	 * @param off
	 * @param len
	 * @return
	 */
	static public ByteBuffer encodeUTF8(String src) {
		// char[] ca = src.toCharArray();
		int len = src.length();
		int off = 0;
		int en = (int) (len * ce.maxBytesPerChar());
		byte[] ba = new byte[en];
		if (len == 0)
			return null;

		ce.reset();
		ByteBuffer bb = ByteBuffer.wrap(ba);
		CharBuffer cb = CharBuffer.wrap(src, off, len);
		try {
			CoderResult cr = ce.encode(cb, bb, true);
			if (!cr.isUnderflow())
				cr.throwException();
			cr = ce.flush(bb);
			if (!cr.isUnderflow())
				cr.throwException();
			return bb;
		} catch (CharacterCodingException x) {
			// Substitution is always enabled,
			// so this shouldn't happen
			throw new Error(x);
		}
	}

	/**
	 * 
	 * build valid http request querystrings from a hashmap that contains name value pairs
	 * 
	 * Copied from Play Router class
	 * 
	 * TODO: array support
	 * 
	 * @param paramMap
	 * @return
	 */
	public static String buildQuery(Map<String, Object> paramMap) {
		StringBuilder queryString = new StringBuilder();
		for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value != null) {
				if (List.class.isAssignableFrom(value.getClass())) {
					List<Object> vals = (List<Object>) value;
					for (Object e : vals) {
						try {
							queryString.append(URLEncoder.encode(key, "utf-8"));
							queryString.append("=");
							if (e.toString().startsWith(":")) {
								queryString.append(e.toString() + "");
							} else {
								queryString.append(URLEncoder.encode(e.toString() + "", "utf-8"));
							}
							queryString.append("&");
						} catch (UnsupportedEncodingException ex) {
						}
					}
				} else {
					try {
						queryString.append(URLEncoder.encode(key, "utf-8"));
						queryString.append("=");
						if (value.toString().startsWith(":")) {
							queryString.append(value.toString() + "");
						} else {
							queryString.append(URLEncoder.encode(value.toString() + "", "utf-8"));
						}
						queryString.append("&");
					} catch (UnsupportedEncodingException ex) {
					}
				}
			}
		}
		String qs = queryString.toString();
		if (qs.endsWith("&")) {
			qs = qs.substring(0, qs.length() - 1);
		}
		return qs;
	}

	public static boolean isEmpty(String charset) {
		return !WebUtils.asBoolean(charset);
	}

	// copied from Play's Utils.java

    public static <T> String join(Iterable<T> values, String separator) {
        if (values == null) {
            return "";
        }
        Iterator<T> iter = values.iterator();
        if (!iter.hasNext()) {
            return "";
        }
        StringBuffer toReturn = new StringBuffer(String.valueOf(iter.next()));
        while (iter.hasNext()) {
            toReturn.append(separator + String.valueOf(iter.next()));
        }
        return toReturn.toString();
    }

    public static String join(String[] values, String separator) {
        return (values == null) ? "" : join(Arrays.asList(values), separator);
    }

	public static String durationInMsFromNanos(long start, long now) {
		String t1 = "" + (now - start) / 100000;
		int __len = t1.length();
		t1 = t1.substring(0, __len - 1) + "." + t1.substring(__len - 1);
		return t1;
	}

}
