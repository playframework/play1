package play.utils;

import net.sf.oval.internal.util.LinkedSet;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;

import java.beans.Encoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Custom impl of java.util.properties that preserves the key-order from the file
 * and that reads the properties-file in utf-8
 */
public class OrderSafeProperties extends java.util.Properties {

    // set used to preserve key order
    private final LinkedHashSet<Object> keys = new LinkedHashSet<Object>();

    @Override
    public void load(InputStream inputStream) throws IOException {

        // read all lines from file as utf-8
        List<String> lines = IOUtils.readLines(inputStream, "utf-8");
        IOUtils.closeQuietly(inputStream);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // escape "special-chars" (to utf-16 on the format \\uxxxx) in lines and store as iso-8859-1
        // see info about escaping - http://download.oracle.com/javase/1.5.0/docs/api/java/util/Properties.html - "public void load(InputStream inStream)"
        for (String line : lines) {

            // due to "...by the rule above, single and double quote characters preceded
            // by a backslash still yield single and double quote characters, respectively."
            // we must transform \" => " and \' => ' before escaping to prevent escaping the backslash
            line = line.replaceAll("\\\\\"", "\"").replaceAll("(^|[^\\\\])(\\\\')", "$1'");
            
            String escapedLine = StringEscapeUtils.escapeJava( line ) + "\n";
            // remove escaped backslashes
            escapedLine = escapedLine.replaceAll("\\\\\\\\","\\\\");
            out.write( escapedLine.getBytes("iso-8859-1"));
        }

        // read properties-file with regular java.util.Properties impl
        super.load( new ByteArrayInputStream( out.toByteArray()) );
    }

    @Override
    public Enumeration<Object> keys() {
        return Collections.<Object>enumeration(keys);
    }

    @Override
    public Set<Object> keySet() {
        return keys;
    }

    @Override
    public Object put(Object key, Object value) {
        keys.add(key);
        return super.put(key, value);
    }

    @Override
    public Object remove(Object o) {
        keys.remove(o);
        return super.remove(o);
    }

    @Override
    public void clear() {
        keys.clear();
        super.clear();
    }

    @Override
    public void putAll(Map<? extends Object, ? extends Object> map) {
        keys.addAll(map.keySet());
        super.putAll(map);
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        Set<Map.Entry<Object, Object>> entrySet = new LinkedSet<Map.Entry<Object, Object>>(keys.size());
        for (Object key : keys) {
            entrySet.add(new Entry(key, get(key)));
        }

        return entrySet;
    }

    static class Entry implements Map.Entry<Object, Object> {
        private final Object key;
        private final Object value;

        private Entry(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        public Object getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public Object setValue(Object o) {
            throw new IllegalStateException("not implemented");
        }
    }

}
