package play.data.binding;

import org.junit.Before;
import org.junit.Test;
import play.PlayBuilder;

import java.lang.annotation.Annotation;
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
        RootParamNode root = ParamNode.convert(r2);
        assertThat(Binder.bind(root, "myInt", Integer.class, null, null)).isEqualTo(myInt);
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
        RootParamNode root = ParamNode.convert(r2);
        Object bindResult = Binder.bind(root, "data1", Data1.class, null, null);
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
        RootParamNode root = ParamNode.convert(r2);
        assertThat(Binder.bind(root, "data2", Data2.class, null, null)).isEqualTo(data2);

    }


    @Test
    public void verify_unbinding_and_binding_of_nestedBeanList() throws Exception {

        Data4 data4 = new Data4();
        data4.a = "aaa";
        data4.b = false;
        data4.c = 12;
        
        Data1 data1_1 = new Data1();
        data1_1.a = "aAaA";
        data1_1.b = 13;

        Data1 data1_2 = new Data1();
        data1_2.a = "bBbB";
        data1_2.b = 14;
        
        data4.dataList = new ArrayList<Data1>(2);
        data4.dataList.add(data1_1);
        data4.dataList.add(data1_2);



        Map<String, Object> r = new HashMap<String, Object>();
        Unbinder.unBind(r, data4, "data4", noAnnotations);
        Map<String, String[]> r2 = fromUnbindMap2BindMap(r);
        RootParamNode root = ParamNode.convert(r2);
        assertThat(Binder.bind(root, "data4", Data4.class, null, null)).isEqualTo(data4);

    }
     @Test
    public void verifyBindingOfStringMaps() throws Exception {
        Map<String, String[]> params = new HashMap<String, String[]>();

        Map<String, String> specialCaseMap = new HashMap<String,String>();
        params.put("specialCaseMap.a", new String[] {"AA"});
        params.put("specialCaseMap.b", new String[] {"BB"});

        Data3 data3;

        params.put("data3.a", new String[] {"aAaA"});
        params.put("data3.map[abc]", new String[] {"ABC"});
        params.put("data3.map[def]", new String[] {"DEF"});

        RootParamNode rootParamNode = ParamNode.convert(params);
        specialCaseMap = (Map<String, String>)Binder.bind(rootParamNode, "specialCaseMap", specialCaseMap.getClass(), specialCaseMap.getClass(), noAnnotations);

        assertThat(specialCaseMap.size()).isEqualTo(2);
        assertThat(specialCaseMap.get("a")).isEqualTo("AA");
        assertThat(specialCaseMap.get("b")).isEqualTo("BB");

        data3 = (Data3) Binder.bind(rootParamNode, "data3", Data3.class, Data3.class, noAnnotations);

        assertThat(data3.a).isEqualTo("aAaA");
        assertThat(data3.map.size()).isEqualTo(2);
        assertThat(data3.map.get("abc")).isEqualTo("ABC");
        assertThat(data3.map.get("def")).isEqualTo("DEF");
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


