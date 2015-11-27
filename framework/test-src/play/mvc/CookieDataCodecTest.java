package play.mvc;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.CookieDataCodec.decode;
import static play.mvc.CookieDataCodec.encode;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class CookieDataCodecTest {

    @Test
    public void flash_cookies_should_bake_in_a_header_and_value() throws UnsupportedEncodingException {
        final Map<String, String> inMap = new HashMap<String, String>(1);
        inMap.put("a", "b");
        final String data = encode(inMap);
        final Map<String, String> outMap = new HashMap<String, String>(1);
        decode(outMap, data);
        assertThat(outMap.size()).isEqualTo(1);
        assertThat(outMap.get("a")).isEqualTo("b");
    }

    @Test
    public void bake_in_multiple_headers_and_values() throws UnsupportedEncodingException {
        final Map<String, String> inMap = new HashMap<String, String>(2);
        inMap.put("a", "b");
        inMap.put("c", "d");
        final String data = encode(inMap);
        final Map<String, String> outMap = new HashMap<String, String>(1);
        decode(outMap, data);
        assertThat(outMap.size()).isEqualTo(2);
        assertThat(outMap.get("a")).isEqualTo("b");
        assertThat(outMap.get("c")).isEqualTo("d");
    }

    @Test
    public void bake_in_a_header_an_empty_value() throws UnsupportedEncodingException {
        final Map<String, String> inMap = new HashMap<String, String>(1);
        inMap.put("a", "");
        final String data = encode(inMap);
        final Map<String, String> outMap = new HashMap<String, String>(1);
        decode(outMap, data);
        assertThat(outMap.size()).isEqualTo(1);
        assertThat(outMap.get("a")).isEqualTo("");
    }

    @Test
    public void bake_in_a_header_a_Unicode_value() throws UnsupportedEncodingException {
        final Map<String, String> inMap = new HashMap<String, String>(1);
        inMap.put("a", "\u0000");
        final String data = encode(inMap);
        final Map<String, String> outMap = new HashMap<String, String>(1);
        decode(outMap, data);
        assertThat(outMap.size()).isEqualTo(1);
        assertThat(outMap.get("a")).isEqualTo("\u0000");
    }

    @Test
    public void bake_in_an_empty_map() throws UnsupportedEncodingException {
        final Map<String, String> inMap = new HashMap<String, String>(0);
        final String data = encode(inMap);
        final Map<String, String> outMap = new HashMap<String, String>(1);
        decode(outMap, data);
        assertThat(outMap.isEmpty());
    }

    @Test
    public void encode_values_such_that_no_extra_keys_can_be_created() throws UnsupportedEncodingException {
        final Map<String, String> inMap = new HashMap<String, String>(1);
        inMap.put("a", "b&c=d");
        final String data = encode(inMap);
        final Map<String, String> outMap = new HashMap<String, String>(1);
        decode(outMap, data);
        assertThat(outMap.size()).isEqualTo(1);
        assertThat(outMap.get("a")).isEqualTo("b&c=d");
    }

    @Test
    public void specifically_exclude_control_chars() throws UnsupportedEncodingException {
        for (int i = 0; i < 32; ++i) {
            final Map<String, String> inMap = new HashMap<String, String>(1);
            final String s = Character.toChars(i).toString();
            inMap.put("a", s);
            final String data = encode(inMap);
            assertThat(data).doesNotContain(s);
            final Map<String, String> outMap = new HashMap<String, String>(1);
            decode(outMap, data);
            assertThat(outMap.size()).isEqualTo(1);
            assertThat(outMap.get("a")).isEqualTo(s);
        }
    }

    @Test
    public void specifically_exclude_special_cookie_chars() throws UnsupportedEncodingException {
        final Map<String, String> inMap = new HashMap<String, String>(1);
        inMap.put("a", " \",;\\");
        final String data = encode(inMap);
        assertThat(data).doesNotContain(" ");
        assertThat(data).doesNotContain("\"");
        assertThat(data).doesNotContain(",");
        assertThat(data).doesNotContain(";");
        assertThat(data).doesNotContain("\\");
        final Map<String, String> outMap = new HashMap<String, String>(1);
        decode(outMap, data);
        assertThat(outMap.size()).isEqualTo(1);
        assertThat(outMap.get("a")).isEqualTo(" \",;\\");
    }

    private String oldEncoder(final Map<String, String> out) throws UnsupportedEncodingException {
        StringBuilder flash = new StringBuilder();
        for (String key : out.keySet()) {
            if (out.get(key) == null)
                continue;
            flash.append("\u0000");
            flash.append(key);
            flash.append(":");
            flash.append(out.get(key));
            flash.append("\u0000");
        }
        return URLEncoder.encode(flash.toString(), "utf-8");

    }

    @Test
    public void decode_values_of_the_previously_supported_format() throws UnsupportedEncodingException {
        final Map<String, String> inMap = new HashMap<String, String>(2);
        inMap.put("a", "b");
        inMap.put("c", "d");
        final String data = oldEncoder(inMap);
        final Map<String, String> outMap = new HashMap<String, String>(0);
        decode(outMap, data);
        assertThat(outMap.isEmpty());
    }

    @Test
    public void decode_values_of_the_previously_supported_format_with_the_new_delimiters_in_them() throws UnsupportedEncodingException {
        final Map<String, String> inMap = new HashMap<String, String>(1);
        inMap.put("a", "b&=");
        final String data = oldEncoder(inMap);
        final Map<String, String> outMap = new HashMap<String, String>(0);
        decode(outMap, data);
        assertThat(outMap.isEmpty());
    }

    @Test
    public void decode_values_with_gibberish_in_them() throws UnsupportedEncodingException {
        final String data = "asfjdlkasjdflk";
        final Map<String, String> outMap = new HashMap<String, String>(1);
        decode(outMap, data);
        assertThat(outMap.isEmpty());
    }

    @Test
    public void decode_values_with_dollar_in_them() throws UnsupportedEncodingException {
        final String data = "%00$Name= %3Avalue%00";
        final Map<String, String> outMap = new HashMap<String, String>(1);
        decode(outMap, data);
        assertThat(outMap.isEmpty());
    }

}