package play.data.binding;

import org.fest.assertions.Assert;
import org.junit.Before;
import org.junit.Test;
import play.PlayBuilder;
import play.data.validation.Validation;
import play.data.validation.ValidationBuilder;

import javax.lang.model.type.TypeVariable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static org.fest.assertions.Assertions.assertThat;


public class BinderTest {

    final Annotation[] noAnnotations = new Annotation[]{};


    @Before
    public void setup() {
        new PlayBuilder().build();
    }

    @Test
    public void verify_and_show_how_unbind_and_bind_work() throws Exception {

        Map<String, Object> r = new HashMap<String, Object>();

        Integer myInt = 12;
        Unbinder.unBind(r, myInt, "myInt", noAnnotations);
        Map<String, String[]> r2 = fromUnbindMap2BindMap(r);
        assertThat(Binder.bind("myInt", Integer.class, null, noAnnotations, r2)).isEqualTo(myInt);
        int a = 0;
    }

    @Test
    public void verify_unbinding_and_binding_of_simple_Bean() throws Exception {

        Data1 data1 = new Data1();
        data1.a = "aAaA";
        data1.b = 13;



        Map<String, Object> r = new HashMap<String, Object>();
        Data1.myStatic = 1;

        Unbinder.unBind(r, data1, "data1", noAnnotations);
        // make sure we only have info about the properties we want..
        assertThat(r.keySet()).containsOnly("data1.a", "data1.b");

        Map<String, String[]> r2 = fromUnbindMap2BindMap( r);

        Data1.myStatic = 2;
        Object bindResult = Binder.bind("data1", Data1.class, null, noAnnotations, r2);
        assertThat(bindResult).isEqualTo(data1);
        assertThat(Data1.myStatic).isEqualTo(2);
    }


    @Test
    public void verify_unbinding_and_binding_of_nestedBeans() throws Exception {

        Data2 data2 = new Data2();
        data2.a = "aaa";
        data2.b = false;
        data2.c = 12;

        data2.data1 = new Data1();
        data2.data1.a = "aAaA";
        data2.data1.b = 13;



        Map<String, Object> r = new HashMap<String, Object>();
        Unbinder.unBind(r, data2, "data2", noAnnotations);
        Map<String, String[]> r2 = fromUnbindMap2BindMap(r);
        assertThat(Binder.bind("data2", Data2.class, null, noAnnotations, r2)).isEqualTo(data2);

    }

    /**
     * Transforms map from Unbinder to Binder
     * @param r map filled by Unbinder
     * @return map used as input to Binder
     */
    private Map<String, String[]> fromUnbindMap2BindMap(Map<String, Object> r) {
        Map<String, String[]> r2 = new HashMap<String, String[]>();
        for (Map.Entry<String, Object> e : r.entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            if (v instanceof String) {
                r2.put(key, new String[]{(String)v});
            } else if (v instanceof String[]) {
                r2.put(key, (String[])v);
            } else {
                throw new RuntimeException("error");
            }
        }
        return r2;
    }

}


class Data1 {

    public static int myStatic;

    private final String f = "final"; 

    public String a;

    public int b;

    public void abc(Integer a) {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data1 data1 = (Data1) o;

        if (b != data1.b) return false;
        if (a != null ? !a.equals(data1.a) : data1.a != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + b;
        return result;
    }
}


class Data2 {
    public String a;
    public Boolean b;
    public int c;

    /**
     * Tried first with arrays and lists but the Unbinder fails in such situations.
     */

    public Data1 data1;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Data2 data2 = (Data2) o;

        if (c != data2.c) return false;
        if (a != null ? !a.equals(data2.a) : data2.a != null) return false;
        if (b != null ? !b.equals(data2.b) : data2.b != null) return false;
        if (data1 != null ? !data1.equals(data2.data1) : data2.data1 != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        result = 31 * result + c;
        result = 31 * result + (data1 != null ? data1.hashCode() : 0);
        return result;
    }
}