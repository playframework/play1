package play.data.binding;

import org.junit.Test;
import play.PlayBuilder;
import play.data.validation.ValidationBuilder;

import java.util.HashMap;
import java.util.Map;
import static org.fest.assertions.Assertions.assertThat;

public class BeanWrapperTest {

    private static class Bean {
        public String a = "a";
        public String b = "b";
        int i = 1;

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

    @Test
    public void testBind() throws Exception {

        new PlayBuilder().build();
        ValidationBuilder.build();
        Map<String, String[]> m = new HashMap<>();
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
}
