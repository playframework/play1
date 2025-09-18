package play.utils;

import org.apache.commons.text.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.*;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Custom impl of java.util.properties that preserves the key-order from the file
 * and that reads the properties-file in utf-8
 */
public class OrderSafeProperties extends java.util.Properties {

    // set used to preserve key order
    private final LinkedHashSet<Object> keys = new LinkedHashSet<>();

    @Override
    public void load(InputStream inputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // read all lines from file as utf-8
        try (var r = new BufferedReader(new InputStreamReader(inputStream, UTF_8))) {
            // escape "special-chars" (to utf-16 on the format \\uxxxx) in lines and store as iso-8859-1
            // see info about escaping - http://download.oracle.com/javase/1.5.0/docs/api/java/util/Properties.html - "public void load(InputStream inStream)"
            r.lines().forEach(line -> {
                // due to "...by the rule above, single and double quote characters preceded
                // by a backslash still yield single and double quote characters, respectively."
                // we must transform \" => " and \' => ' before escaping to prevent escaping the backslash
                line = line.replaceAll("\\\\\"", "\"").replaceAll("(^|[^\\\\])(\\\\')", "$1'");

                String escapedLine = StringEscapeUtils.escapeJava( line ) + '\n';
                // remove escaped backslashes
                escapedLine = escapedLine.replaceAll("\\\\\\\\","\\\\");
                try {
                    out.write(escapedLine.getBytes(ISO_8859_1));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
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
        Set<Map.Entry<Object, Object>> entrySet = new LinkedHashSet<>(keys.size());
        for (Object key : keys) {
            entrySet.add(Map.entry(key, get(key)));
        }

        return entrySet;
    }

}
