package play.data.binding;

import org.junit.Before;
import org.junit.Test;
import play.PlayBuilder;
import play.data.validation.Validation;
import play.data.validation.ValidationPlugin;

import java.lang.annotation.Annotation;
import java.util.*;

import static org.fest.assertions.Assertions.assertThat;


public class BinderTest {

    final Annotation[] noAnnotations = new Annotation[]{};

    // provider of generic typed collection
    private class GenericListProvider {
        private List<Data2> listOfData2 = new ArrayList<Data2>();
    }

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

     @Test
	    public void verify_binding_of_simple_bean_collections() throws Exception {

	        Map<String, String[]> params = new HashMap<String, String[]>();

	        Data2 data2;
	        List<Data2> lst = new ArrayList<Data2>();
			// build the parameters
	        params.put("data2[0].a", new String[] { "a0" });
	        params.put("data2[1].a", new String[] { "a1" });
	        params.put("data2[2].a", new String[] { "a2" });
	        params.put("data2[3].a", new String[] { "a3" });
	        params.put("data2[4].a", new String[] { "a4" });
	        params.put("data2[5].a", new String[] { "a5" });
	        params.put("data2[6].a", new String[] { "a6" });
	        params.put("data2[7].a", new String[] { "a7" });
	        params.put("data2[8].a", new String[] { "a8" });
	        params.put("data2[9].a", new String[] { "a9" });
	        params.put("data2[10].a", new String[] { "a10" });
	        params.put("data2[12].a", new String[] { "a12" });

	        RootParamNode rootParamNode = ParamNode.convert(params);

	        lst = (List<Data2>) Binder.bind(rootParamNode, "data2", lst.getClass(), GenericListProvider.class.getDeclaredFields()[0].getGenericType(),
	                noAnnotations);
			//check the size and the order
	        assertThat(lst.size()).isEqualTo(13);
	        assertThat(lst.get(0).a).isEqualTo("a0");
	        assertThat(lst.get(1).a).isEqualTo("a1");
	        assertThat(lst.get(9).a).isEqualTo("a9");
	        assertThat(lst.get(10).a).isEqualTo("a10");
	        assertThat(lst.get(10).a).isEqualTo("a10");
	        assertThat(lst.get(11)).isNull(); //check for null item
	        assertThat(lst.get(12).a).isEqualTo("a12");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void verify_binding_of_root_parameters() throws Exception {
        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put("a", new String[] {"foo"});
        params.put("b", new String[] {"2"});

        RootParamNode rootParamNode = ParamNode.convert(params);
        Data1 data1 = new Data1();
        Binder.bindBean(rootParamNode, "", data1);

        assertThat(data1.a).isEqualTo("foo");
        assertThat(data1.b).isEqualTo(2);

        // Also test with the old deprecated but shorter form
        data1 = new Data1();
        Binder.bind(data1, null, params);
        assertThat(data1.a).isEqualTo("foo");
        assertThat(data1.b).isEqualTo(2);

    }

    @Test
    @SuppressWarnings("deprecation")
    public void verify_validation_error_of_root_parameters() throws Exception {
        // Initialize Validation.current()
        new ValidationPlugin().beforeInvocation();

        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put("a", new String[] {"foo"});
        params.put("b", new String[] {"bar"});

        RootParamNode rootParamNode = ParamNode.convert(params);
        Data1 data1 = new Data1();
        Binder.bindBean(rootParamNode, null, data1);

        assertThat(Validation.error("a")).isNull();
        assertThat(Validation.error("b")).isNotNull();

        // Also test with the old deprecated but shorter form
        data1 = new Data1();
        Binder.bind(data1, null, params);
        assertThat(Validation.error("a")).isNull();
        assertThat(Validation.error("b")).isNotNull();
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


