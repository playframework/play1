package play.data.binding;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import play.PlayBuilder;
import play.data.validation.ValidationBuilder;

public class BeanWrapperTest {

    public static class Bean {
        public String a = "a";
        public String b = "b";
        public int i = 1;

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }
    }

    @AttributeStripping
    public static class StrippingBean {
        public String value;

        public String toNull;

        @AttributeStripping(strip = false, nullify = false)
        public String intact;

        @AttributeStripping(squish = true)
        public String squish;

        public int num;

        public String getToNull() {
            return toNull;
        }

        public void setToNull(String toNull) {
            this.toNull = toNull;
        }

    }

    @Test
    public void testBind() throws Exception {

        new PlayBuilder().build();
        ValidationBuilder.build();
        Map<String, String[]> m = new HashMap<String, String[]>();
        m.put("b.a", new String[]{"a1"});
        m.put("b.b", new String[]{"b1"});
        m.put("b.i", new String[]{"2"});

        Bean b = new Bean();
        new BeanWrapper(Bean.class).bind("b", null, m, "", b, null);
        assertThat(b.a).isEqualTo("a1");
        assertThat(b.b).isEqualTo("b1");
        assertThat(b.i).isEqualTo(2);

        b = new Bean();
        new BeanWrapper(Bean.class).bind("", null, m, "b", b, null);
        assertThat(b.a).isEqualTo("a1");
        assertThat(b.b).isEqualTo("b1");
        assertThat(b.i).isEqualTo(2);

        b = (Bean)new BeanWrapper(Bean.class).bind("b", null, m, "", null);
        assertThat(b.a).isEqualTo("a1");
        assertThat(b.b).isEqualTo("b1");
        assertThat(b.i).isEqualTo(2);
    }

    @Test
    public void testStripping() throws Exception {
        new PlayBuilder().build();
        ValidationBuilder.build();

        StrippingBean b = new StrippingBean();
        Map<String, String[]> m = new HashMap<String, String[]>();
        m.put("b.value", new String[]{" a  bc "});
        m.put("b.toNull", new String[]{"   "});
        m.put("b.intact", new String[]{"   "});
        m.put("b.squish", new String[]{"  a_ b   c "});
        m.put("b.num", new String[]{"  123 "});

        new BeanWrapper(StrippingBean.class).bind("b", null, m, "", b, null);
        assertThat(b.value).isEqualTo("a  bc");
        assertThat(b.getToNull()).isEqualTo(null);
        assertThat(b.intact).isEqualTo("   ");
        assertThat(b.num).isEqualTo(123);
    }
}
