/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package play.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;



public class OrderSafeProperties extends java.util.Properties {

    private static final long serialVersionUID = 4112578634029874840L;

    protected OrderSafeProperties defaults;
    private static final int NONE = 0, SLASH = 1, UNICODE = 2, CONTINUE = 3,
            KEY_DONE = 4, IGNORE = 5;
    private LinkedHashMap<Object, Object> _data = new LinkedHashMap<Object, Object>();

    public OrderSafeProperties() {
        super();
    }

    public OrderSafeProperties(OrderSafeProperties properties) {
        defaults = properties;
    }

    @SuppressWarnings("unused")
    private void dumpString(StringBuilder buffer, String string, boolean key) {
        int i = 0;
        if (!key && i < string.length() && string.charAt(i) == ' ') {
            buffer.append("\\ "); //$NON-NLS-1$
            i++;
        }

        for (; i < string.length(); i++) {
            char ch = string.charAt(i);
            switch (ch) {
                case '\t':
                    buffer.append("\\t"); //$NON-NLS-1$
                    break;
                case '\n':
                    buffer.append("\\n"); //$NON-NLS-1$
                    break;
                case '\f':
                    buffer.append("\\f"); //$NON-NLS-1$
                    break;
                case '\r':
                    buffer.append("\\r"); //$NON-NLS-1$
                    break;
                default:
                    if ("\\#!=:".indexOf(ch) >= 0 || (key && ch == ' ')) {
                        buffer.append('\\');
                    }
                    if (ch >= ' ' && ch <= '~') {
                        buffer.append(ch);
                    } else {
                        String hex = Integer.toHexString(ch);
                        buffer.append("\\u"); //$NON-NLS-1$
                        for (int j = 0; j < 4 - hex.length(); j++) {
                            buffer.append("0"); //$NON-NLS-1$
                        }
                        buffer.append(hex);
                    }
            }
        }
    }

    @Override
    public String getProperty(String name) {
        Object result = _data.get(name);
        String property = result instanceof String ? (String) result : null;
        if (property == null && defaults != null) {
            property = defaults.getProperty(name);
        }
        return property;
    }

    @Override
    public String getProperty(String name, String defaultValue) {
        Object result = _data.get(name);
        String property = result instanceof String ? (String) result : null;
        if (property == null && defaults != null) {
            property = defaults.getProperty(name);
        }
        if (property == null) {
            return defaultValue;
        }
        return property;
    }

    @Override
    public void list(PrintStream out) {
        if (out == null) {
            throw new NullPointerException();
        }
        StringBuilder buffer = new StringBuilder(80);
        Enumeration<?> keys = propertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            buffer.append(key);
            buffer.append('=');
            String property = (String) _data.get(key);
            OrderSafeProperties def = defaults;
            while (property == null) {
                property = (String) def.get(key);
                def = def.defaults;
            }
            if (property.length() > 40) {
                buffer.append(property.substring(0, 37));
                buffer.append("..."); //$NON-NLS-1$
            } else {
                buffer.append(property);
            }
            out.println(buffer.toString());
            buffer.setLength(0);
        }
    }

    @Override
    public void list(PrintWriter writer) {
        if (writer == null) {
            throw new NullPointerException();
        }
        StringBuilder buffer = new StringBuilder(80);
        Enumeration<?> keys = propertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            buffer.append(key);
            buffer.append('=');
            String property = (String) _data.get(key);
            OrderSafeProperties def = defaults;
            while (property == null) {
                property = (String) def.get(key);
                def = def.defaults;
            }
            if (property.length() > 40) {
                buffer.append(property.substring(0, 37));
                buffer.append("..."); //$NON-NLS-1$
            } else {
                buffer.append(property);
            }
            writer.println(buffer.toString());
            buffer.setLength(0);
        }
    }

    /**
     * Loads properties from the specified {@code InputStream}. The encoding is
     * ISO8859-1.
     *
     * @param in
     *            the {@code InputStream}.
     * @throws IOException
     *             if error occurs during reading from the {@code InputStream}.
     */
    @Override
    public synchronized void load(InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException();
        }
        BufferedInputStream bis = new BufferedInputStream(in);
        bis.mark(Integer.MAX_VALUE);
        boolean isEbcdic = isEbcdic(bis);
        bis.reset();

        if (!isEbcdic) {
            load(new InputStreamReader(bis, "ISO8859-1")); //$NON-NLS-1$
        } else {
            load(new InputStreamReader(bis)); //$NON-NLS-1$
        }
    }

    private boolean isEbcdic(BufferedInputStream in) throws IOException {
        byte b;
        while ((b = (byte) in.read()) != -1) {
            if (b == 0x23 || b == 0x0a || b == 0x3d) {//ascii: newline/#/=
                return false;
            }
            if (b == 0x15) {//EBCDIC newline
                return true;
            }
        }
        //we found no ascii newline, '#', neither '=', relative safe to consider it
        //as non-ascii, the only exception will be a single line with only key(no value and '=')
        //in this case, it should be no harm to read it in default charset
        return false;
    }

    @Override
    public synchronized void load(Reader reader) throws IOException {
        int mode = NONE, unicode = 0, count = 0;
        char nextChar, buf[] = new char[40];
        int offset = 0, keyLength = -1, intVal;
        boolean firstChar = true;
        BufferedReader br = new BufferedReader(reader);

        while (true) {
            intVal = br.read();
            if (intVal == -1) {
                break;
            }
            nextChar = (char) intVal;

            if (offset == buf.length) {
                char[] newBuf = new char[buf.length * 2];
                System.arraycopy(buf, 0, newBuf, 0, offset);
                buf = newBuf;
            }
            if (mode == UNICODE) {
                int digit = Character.digit(nextChar, 16);
                if (digit >= 0) {
                    unicode = (unicode << 4) + digit;
                    if (++count < 4) {
                        continue;
                    }
                } else if (count <= 4) {
                    // luni.09=Invalid Unicode sequence: illegal character
                    throw new IllegalArgumentException("luni.09");
                }
                mode = NONE;
                buf[offset++] = (char) unicode;
                if (nextChar != '\n' && nextChar != '\u0085') {
                    continue;
                }
            }
            if (mode == SLASH) {
                mode = NONE;
                switch (nextChar) {
                    case '\r':
                        mode = CONTINUE; // Look for a following \n
                        continue;
                    case '\u0085':
                    case '\n':
                        mode = IGNORE; // Ignore whitespace on the next line
                        continue;
                    case 'b':
                        nextChar = '\b';
                        break;
                    case 'f':
                        nextChar = '\f';
                        break;
                    case 'n':
                        nextChar = '\n';
                        break;
                    case 'r':
                        nextChar = '\r';
                        break;
                    case 't':
                        nextChar = '\t';
                        break;
                    case 'u':
                        mode = UNICODE;
                        unicode = count = 0;
                        continue;
                }
            } else {
                switch (nextChar) {
                    case '#':
                    case '!':
                        if (firstChar) {
                            while (true) {
                                intVal = br.read();
                                if (intVal == -1) {
                                    break;
                                }
                                nextChar = (char) intVal; // & 0xff
                                // not
                                // required
                                if (nextChar == '\r' || nextChar == '\n' || nextChar == '\u0085') {
                                    break;
                                }
                            }
                            continue;
                        }
                        break;
                    case '\n':
                        if (mode == CONTINUE) { // Part of a \r\n sequence
                            mode = IGNORE; // Ignore whitespace on the next line
                            continue;
                        }
                    // fall into the next case
                    case '\u0085':
                    case '\r':
                        mode = NONE;
                        firstChar = true;
                        if (offset > 0 || (offset == 0 && keyLength == 0)) {
                            if (keyLength == -1) {
                                keyLength = offset;
                            }
                            String temp = new String(buf, 0, offset);
                            _data.put(temp.substring(0, keyLength), temp.substring(keyLength));
                        }
                        keyLength = -1;
                        offset = 0;
                        continue;
                    case '\\':
                        if (mode == KEY_DONE) {
                            keyLength = offset;
                        }
                        mode = SLASH;
                        continue;
                    case ':':
                    case '=':
                        if (keyLength == -1) { // if parsing the key
                            mode = NONE;
                            keyLength = offset;
                            continue;
                        }
                        break;
                }
                if (Character.isWhitespace(nextChar)) {
                    if (mode == CONTINUE) {
                        mode = IGNORE;
                    }
                    // if key length == 0 or value length == 0
                    if (offset == 0 || offset == keyLength || mode == IGNORE) {
                        continue;
                    }
                    if (keyLength == -1) { // if parsing the key
                        mode = KEY_DONE;
                        continue;
                    }
                }
                if (mode == IGNORE || mode == CONTINUE) {
                    mode = NONE;
                }
            }
            firstChar = false;
            if (mode == KEY_DONE) {
                keyLength = offset;
                mode = NONE;
            }
            buf[offset++] = nextChar;
        }
        if (mode == UNICODE && count <= 4) {
            // luni.08=Invalid Unicode sequence: expected format \\uxxxx
            throw new IllegalArgumentException("luni.08");
        }
        if (keyLength == -1 && offset > 0) {
            keyLength = offset;
        }
        if (keyLength >= 0) {
            String temp = new String(buf, 0, offset);
            String key = temp.substring(0, keyLength);
            String value = temp.substring(keyLength);
            if (mode == SLASH) {
                value += "\u0000";
            }
            _data.put(key, value);
        }
    }

    @Override
    public Enumeration<?> propertyNames() {
        Hashtable<Object, Object> selected = new Hashtable<Object, Object>();
        selectProperties(selected, false);
        return selected.keys();
    }

    @Override
    public Set<String> stringPropertyNames() {
        Hashtable<String, String> stringProperties = new Hashtable<String, String>();
        selectProperties(stringProperties, true);
        return Collections.unmodifiableSet(stringProperties.keySet());
    }

    @SuppressWarnings("unchecked")
    private void selectProperties(Hashtable selectProperties, final boolean isStringOnly) {
        if (defaults != null) {
            defaults.selectProperties(selectProperties, isStringOnly);
        }

        Iterator<?> keys = _data.keySet().iterator();
        Object key, value;
        while (keys.hasNext()) {
            key = keys.next();
            if (isStringOnly) {
                // Only select property with string key and value
                if (key instanceof String) {
                    value = _data.get(key);
                    if (value instanceof String) {
                        selectProperties.put(key, value);
                    }
                }
            } else {
                value = _data.get(key);
                selectProperties.put(key, value);
            }
        }
    }

    @Override
    public Object setProperty(String name, String value) {
        return _data.put(name, value);
    }

    @Override
    public synchronized void store(OutputStream out, String comment) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void store(Writer writer, String comment) throws IOException {
    }

    @Override
    public synchronized void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeToXML(OutputStream os, String comment) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void storeToXML(OutputStream os, String comment, String encoding) throws IOException {
        throw new UnsupportedOperationException();
    }

    // Override Hastable API

    @Override
    public synchronized String toString() {
        return _data.toString();
    }

    @Override
    public void clear() {
        _data.clear();
    }

    @Override
    public boolean contains(Object value) {
        return _data.containsKey(value);
    }

    @Override
    public boolean containsKey(Object key) {
        return _data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return _data.containsValue(value);
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return _data.entrySet();
    }

    @Override
    public boolean isEmpty() {
        return _data.isEmpty();
    }

    @Override
    public Enumeration<Object> keys() {
        return new Hashtable<Object, Object>(_data).keys();
    }

    @Override
    public Set<Object> keySet() {
        return _data.keySet();
    }

    @Override
    public synchronized Object get(Object key) {
        return _data.get(key);
    }

    @Override
    public Object put(Object key, Object value) {
        return _data.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putAll(Map t) {
        _data.putAll(t);
    }

    @Override
    public int size() {
        return _data.size();
    }

    @Override
    public Collection<Object> values() {
        return _data.values();
    }
}
