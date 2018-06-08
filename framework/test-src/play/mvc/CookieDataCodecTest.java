package play.mvc;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.CookieDataCodec.decode;
import static play.mvc.CookieDataCodec.encode;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.handler.codec.http.cookie.Cookie;
import org.jboss.netty.handler.codec.http.cookie.ServerCookieDecoder;
import org.junit.Test;

public class CookieDataCodecTest {

    @Test
    public void flash_cookies_should_bake_in_a_header_and_value() throws UnsupportedEncodingException {
        Map<String, String> inMap = new HashMap<>(1);
        inMap.put("a", "b");
        String data = encode(inMap);
        Map<String, String> outMap = new HashMap<>(1);
        decode(outMap, data);
        assertThat(outMap.size()).isEqualTo(1);
        assertThat(outMap.get("a")).isEqualTo("b");
    }

    @Test
    public void bake_in_multiple_headers_and_values() throws UnsupportedEncodingException {
        Map<String, String> inMap = new HashMap<>(2);
        inMap.put("a", "b");
        inMap.put("c", "d");
        String data = encode(inMap);
        Map<String, String> outMap = new HashMap<>(1);
        decode(outMap, data);
        assertThat(outMap.size()).isEqualTo(2);
        assertThat(outMap.get("a")).isEqualTo("b");
        assertThat(outMap.get("c")).isEqualTo("d");
    }

    @Test
    public void bake_in_a_header_an_empty_value() throws UnsupportedEncodingException {
        Map<String, String> inMap = new HashMap<>(1);
        inMap.put("a", "");
        String data = encode(inMap);
        Map<String, String> outMap = new HashMap<>(1);
        decode(outMap, data);
        assertThat(outMap.size()).isEqualTo(1);
        assertThat(outMap.get("a")).isEqualTo("");
    }

    @Test
    public void bake_in_a_header_a_Unicode_value() throws UnsupportedEncodingException {
        Map<String, String> inMap = new HashMap<>(1);
        inMap.put("a", "\u0000");
        String data = encode(inMap);
        Map<String, String> outMap = new HashMap<>(1);
        decode(outMap, data);
        assertThat(outMap.size()).isEqualTo(1);
        assertThat(outMap.get("a")).isEqualTo("\u0000");
    }

    @Test
    public void bake_in_an_empty_map() throws UnsupportedEncodingException {
        Map<String, String> inMap = new HashMap<>(0);
        String data = encode(inMap);
        Map<String, String> outMap = new HashMap<>(1);
        decode(outMap, data);
        assertThat(outMap.isEmpty());
    }

    @Test
    public void encode_values_such_that_no_extra_keys_can_be_created() throws UnsupportedEncodingException {
        Map<String, String> inMap = new HashMap<>(1);
        inMap.put("a", "b&c=d");
        String data = encode(inMap);
        Map<String, String> outMap = new HashMap<>(1);
        decode(outMap, data);
        assertThat(outMap.size()).isEqualTo(1);
        assertThat(outMap.get("a")).isEqualTo("b&c=d");
    }

    @Test
    public void specifically_exclude_control_chars() throws UnsupportedEncodingException {
        for (int i = 0; i < 32; ++i) {
            Map<String, String> inMap = new HashMap<>(1);
            String s = Character.toChars(i).toString();
            inMap.put("a", s);
            String data = encode(inMap);
            assertThat(data).doesNotContain(s);
            Map<String, String> outMap = new HashMap<>(1);
            decode(outMap, data);
            assertThat(outMap.size()).isEqualTo(1);
            assertThat(outMap.get("a")).isEqualTo(s);
        }
    }

    @Test
    public void specifically_exclude_special_cookie_chars() throws UnsupportedEncodingException {
        Map<String, String> inMap = new HashMap<>(1);
        inMap.put("a", " \",;\\");
        String data = encode(inMap);
        assertThat(data).doesNotContain(" ");
        assertThat(data).doesNotContain("\"");
        assertThat(data).doesNotContain(",");
        assertThat(data).doesNotContain(";");
        assertThat(data).doesNotContain("\\");
        Map<String, String> outMap = new HashMap<>(1);
        decode(outMap, data);
        assertThat(outMap.size()).isEqualTo(1);
        assertThat(outMap.get("a")).isEqualTo(" \",;\\");
    }

    private String oldEncoder(Map<String, String> out) throws UnsupportedEncodingException {
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
        Map<String, String> inMap = new HashMap<>(2);
        inMap.put("a", "b");
        inMap.put("c", "d");
        String data = oldEncoder(inMap);
        Map<String, String> outMap = new HashMap<>(0);
        decode(outMap, data);
        assertThat(outMap.isEmpty());
    }

    @Test
    public void decode_values_of_the_previously_supported_format_with_the_new_delimiters_in_them() throws UnsupportedEncodingException {
        Map<String, String> inMap = new HashMap<>(1);
        inMap.put("a", "b&=");
        String data = oldEncoder(inMap);
        Map<String, String> outMap = new HashMap<>(0);
        decode(outMap, data);
        assertThat(outMap.isEmpty());
    }

    @Test
    public void decode_values_with_gibberish_in_them() throws UnsupportedEncodingException {
        String data = "asfjdlkasjdflk";
        Map<String, String> outMap = new HashMap<>(1);
        decode(outMap, data);
        assertThat(outMap.isEmpty());
    }

    @Test
    public void decode_values_with_dollar_in_them() throws UnsupportedEncodingException {
        String data = "%00$Name= %3Avalue%00";
        Map<String, String> outMap = new HashMap<>(1);
        decode(outMap, data);
        assertThat(outMap.isEmpty());
    }

    @Test
    public void decode_values_with_dollar_in_them2() throws UnsupportedEncodingException {
        String data = "$Name: value";
        Map<String, String> outMap = new HashMap<>(1);
        decode(outMap, data);
        assertThat(outMap.isEmpty());

        Set<Cookie> cookieSet = ServerCookieDecoder.STRICT.decode(data);
        if (cookieSet != null) {
            for (Cookie cookie : cookieSet) {
                Http.Cookie playCookie = new Http.Cookie();
                playCookie.name = cookie.name();
                playCookie.path = cookie.path();
                playCookie.domain = cookie.domain();
                playCookie.secure = cookie.isSecure();
                playCookie.value = cookie.value();
                playCookie.httpOnly = cookie.isHttpOnly();
            }
        }

    }

}