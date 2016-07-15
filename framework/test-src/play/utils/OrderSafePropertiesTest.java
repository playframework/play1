package play.utils;

import org.junit.Test;
import play.Logger;
import play.libs.IO;

import java.io.InputStream;
import java.util.*;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;


public class OrderSafePropertiesTest {

    @Test
    public void verifyThatEscaping_properties_content_giveSameResultAs_java_util_properties() throws Exception {
        // see info about escaping - http://download.oracle.com/javase/1.5.0/docs/api/java/util/Properties.html - "public void load(InputStream inStream)"
        Properties javaP = new Properties();
        Properties playP = new OrderSafeProperties();
        javaP.load(getClass().getResourceAsStream("/play/utils/OrderSaferPropertiesTest2.properties"));
        playP.load(getClass().getResourceAsStream("/play/utils/OrderSaferPropertiesTest2.properties"));
        assertThat(playP.getProperty("a")).isEqualTo(javaP.getProperty("a"));
        Logger.info("playP.getProperty(\"a\"):" + playP.getProperty("a"));

    }

    @Test
    public void verifyCorrectOrder() throws Exception{
        InputStream in = getClass().getResourceAsStream("/play/utils/OrderSaferPropertiesTest.properties");
        assertThat(in).isNotNull();
        Properties p = new OrderSafeProperties();
        p.load(in);
        in.close();

        // check order using keyet
        int order = 0;
        for (Object _key : p.keySet()) {
            String key = (String)_key;
            if( !key.startsWith("_")) {

                String value = (String)p.get(key);

                int currentOrder = Integer.parseInt(value);
                order++;
                assertThat(currentOrder).isEqualTo(order);
            }
        }

        // check order using entrySet
        order = 0;
        for (Map.Entry<Object, Object> e : p.entrySet()) {
            String key = (String)e.getKey();
            String value = (String)e.getValue();
            if( !key.startsWith("_")) {

                int currentOrder = Integer.parseInt(value);
                order++;
                assertThat(currentOrder).isEqualTo(order);
            }
        }

    }

    @Test
    public void verifyUTF8() throws Exception {

        InputStream in = getClass().getResourceAsStream("/play/utils/OrderSaferPropertiesTest.properties");
        assertThat(in).isNotNull();
        Properties p = new OrderSafeProperties();
        p.load(in);
        in.close();

        verifyPropertiesContent(p);
    }

    @Test
    public void verifyUTF8_via_readUtf8Properties() throws Exception {

        InputStream in = getClass().getResourceAsStream("/play/utils/OrderSaferPropertiesTest.properties");
        assertThat(in).isNotNull();
        Properties p = IO.readUtf8Properties(in);


        verifyPropertiesContent(p);
    }

    private void verifyPropertiesContent(Properties p) {
        assertThat(p.getProperty("_check_1")).isEqualTo("æøåÆØÅ");
        assertThat(p.getProperty("_check_2")).isEqualTo("equal = % %%'\"");
        assertThat(p.getProperty("_check_3")).isEqualTo("z");
        assertThat(p.getProperty("_check_4")).isEqualTo("\"quoted string\"");
        assertThat(p.getProperty("_check_5")).isEqualTo("newLineString\n");
        assertThat(p.getProperty("_check_6")).isEqualTo("欢迎");
        assertThat(p.getProperty("_check_7.ยินดีต้อนรับ")).isEqualTo("ยินดีต้อนรับ");
        assertThat(p.getProperty("_check_8")).isEqualTo("х");// Unicode Character 'CYRILLIC SMALL LETTER HA' (U+0445)

        // test from Lyubomir Ivanov
        String cyrillic_bulgarian_caps  = "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЬЮЯ";
        String cyrillic_bulgarian_small = "абвгдежзийклмнопрстуфхцчшщъьюя";
        String cyrillic_bulgarian_old   = "ѣѢѫѪѭѬ";
        String cyrillic_russian         = "ЭЫэы";
        String cyrillic_serbian         = "ЉЊЂЋЏљњђћџ";
        assertThat(p.getProperty("_cyrillic_bulgarian_caps")).isEqualTo(cyrillic_bulgarian_caps);
        assertThat(p.getProperty("_cyrillic_bulgarian_small")).isEqualTo(cyrillic_bulgarian_small);
        assertThat(p.getProperty("_cyrillic_bulgarian_old")).isEqualTo(cyrillic_bulgarian_old);
        assertThat(p.getProperty("_cyrillic_russian")).isEqualTo(cyrillic_russian);
        assertThat(p.getProperty("_cyrillic_serbian")).isEqualTo(cyrillic_serbian);


    }
}
