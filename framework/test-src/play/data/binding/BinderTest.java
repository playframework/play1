package play.data.binding;

import org.junit.Before;
import org.junit.Test;
import play.PlayBuilder;
import play.data.validation.Validation;
import play.data.validation.ValidationPlugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static java.math.BigDecimal.TEN;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class BinderTest {

    final Annotation[] noAnnotations = new Annotation[]{};

    // provider of generic typed collection
    private class GenericListProvider {
        private List<Data2> listOfData2 = new ArrayList<>();
    }

    @Before
    public void setup() {
        new PlayBuilder().build();
    }

    @Test
    public void verify_and_show_how_unbind_and_bind_work() throws Exception {

        Map<String, Object> r = new HashMap<>();

        Integer myInt = 12;
        Unbinder.unBind(r, myInt, "myInt", noAnnotations);
        Map<String, String[]> r2 = fromUnbindMap2BindMap(r);
        RootParamNode root = ParamNode.convert(r2);
        assertThat(Binder.bind(root, "myInt", Integer.class, null, null)).isEqualTo(myInt);
    }

    @Test
    public void verify_unbinding_and_binding_of_simple_Bean() throws Exception {

        Data1 data1 = new Data1();
        data1.a = "aAaA";
        data1.b = 13;



        Map<String, Object> r = new HashMap<>();
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
        
        Data1 data1_1 = new Data1();
        data1_1.a = "aAaA";
        data1_1.b = 13;

        Data1 data1_2 = new Data1();
        data1_2.a = "bBbB";
        data1_2.b = 14;
        
        data2.data1 = data1_1;
        data2.datas = new ArrayList<>(2);
        data2.datas.add(data1_1);
        data2.datas.add(data1_2);



        Map<String, Object> r = new HashMap<>();
        Unbinder.unBind(r, data2, "data2", noAnnotations);
        Map<String, String[]> r2 = fromUnbindMap2BindMap(r);
        RootParamNode root = ParamNode.convert(r2);
        assertThat(Binder.bind(root, "data2", Data2.class, null, null)).isEqualTo(data2);

    }


     @Test
    public void verifyBindingOfStringMaps() throws Exception {
        Map<String, String[]> params = new HashMap<>();

        Map<String, String> specialCaseMap = new HashMap<>();
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

         Map<String, String[]> params = new HashMap<>();

         List<Data2> lst = new ArrayList<>();
         // build the parameters
         params.put("data2[0].a", new String[]{"a0"});
         params.put("data2[1].a", new String[]{"a1"});
         params.put("data2[2].a", new String[]{"a2"});
         params.put("data2[3].a", new String[]{"a3"});
         params.put("data2[4].a", new String[]{"a4"});
         params.put("data2[5].a", new String[]{"a5"});
         params.put("data2[6].a", new String[]{"a6"});
         params.put("data2[7].a", new String[]{"a7"});
         params.put("data2[8].a", new String[]{"a8"});
         params.put("data2[9].a", new String[]{"a9"});
         params.put("data2[10].a", new String[]{"a10"});
         params.put("data2[12].a", new String[]{"a12"});

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
        Map<String, String[]> params = new HashMap<>();
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

        Map<String, String[]> params = new HashMap<>();
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

    @Test
    public void verify_binding_collections_of_generic_types() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("data.genericTypeList", new String[]{"1", "2", "3"});

        RootParamNode rootParamNode = ParamNode.convert(params);
        Data3 result = (Data3) Binder.bind(rootParamNode, "data", Data3.class,
                Data3.class, noAnnotations);

        assertThat(result.genericTypeList).hasSize(3);

        for (int i = 1; i < 3; i++) {
            assertThat(result.genericTypeList.get(i - 1).value).isEqualTo(Long.valueOf(i));
        }
    }

    @Test
    public void test_unbinding_of_collection_of_complex_types() {
        Data1 d1 = new Data1();
        d1.a = "a";
        d1.b = 1;

        Data1 d2 = new Data1();
        d2.a = "b";
        d2.b = 2;

        Data1 d3 = new Data1();
        d3.a = "c";
        d3.b = 3;

        Data1[] datasArray = {d1, d2};
        List<Data1> datas = Arrays.asList(new Data1[]{d2, d1, d3});

        Map<String, Data1> mapData = new HashMap<>();
        mapData.put(d1.a, d1);
        mapData.put(d2.a, d2);
        mapData.put(d3.a, d3);

        Data4 original = new Data4();
        original.s = "some";
        original.datas = datas;
        original.datasArray = datasArray;
        original.mapDatas = mapData;

        Map<String, Object> result = new HashMap<>();
        Unbinder.unBind(result, original, "data", noAnnotations);

        Map<String, String[]> r2 = fromUnbindMap2BindMap(result);
        RootParamNode root = ParamNode.convert(r2);

        Object binded = Binder.bind(root, "data", Data4.class, Data4.class, noAnnotations);
        assertThat(binded).isEqualTo(original);
    }

    @Test
    public void test_enum_set_binding() {
        Data5 data = new Data5();
        data.s = "test";
        data.testEnumSet = EnumSet.of(Data5.TestEnum.A, Data5.TestEnum.B, Data5.TestEnum.C);

        Map<String, String[]> params = new HashMap<>();
        params.put("data.testEnumSet", new String[]{"A", "B", "C"});

        RootParamNode rootParamNode = ParamNode.convert(params);

        Data5 binded = (Data5) Binder.bind(rootParamNode, "data", Data5.class, Data5.class, noAnnotations);
        assertThat(binded.testEnumSet).isEqualTo(data.testEnumSet);
    }

    @Test
    public void test_binding_class_with_private_constructor() {
        Map<String, String[]> params = new HashMap<>();
        params.put("user.name", new String[]{"john"});

        RootParamNode rootParamNode = ParamNode.convert(params);

        Data6 binded = (Data6) Binder.bind(rootParamNode, "user", Data6.class, Data6.class, noAnnotations);
        assertThat(binded.name).isEqualTo("john");
    }

    /**
     * Transforms map from Unbinder to Binder
     * @param r map filled by Unbinder
     * @return map used as input to Binder
     */
    private Map<String, String[]> fromUnbindMap2BindMap(Map<String, Object> r) {
        Map<String, String[]> r2 = new HashMap<>();
        for (Map.Entry<String, Object> e : r.entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            System.out.println(key + " " + v + " " ) ;
            if (v instanceof String) {
                r2.put(key, new String[]{(String)v});
            } else if (v instanceof String[]) {
                r2.put(key, (String[])v);
            } else if (v instanceof Collection) {
                Object[] array = ((Collection) v).toArray();
                r2.put(key, Arrays.copyOf(array, array.length, String[].class));
            } else {
                throw new RuntimeException("error");
            }
        }
        return r2;
    }

    @Test
    public void applicationCanRegisterAndUnregisterCustomBinders() {
        Binder.register(BigDecimal.class, new MyBigDecimalBinder());
        assertNotNull(Binder.supportedTypes.get(BigDecimal.class));

        Binder.unregister(BigDecimal.class);
        assertNull(Binder.supportedTypes.get(BigDecimal.class));
    }

    private static class MyBigDecimalBinder implements TypeBinder<BigDecimal> {
        @Override
        public Object bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) throws Exception {
            return new BigDecimal(value).add(TEN);
        }
    }

    @Test
    public void verify_binding_of_BigInteger() {
        Map<String, Object> r = new HashMap<>();

        BigInteger myBigInt = new BigInteger("12");
        Integer myBigIntAsInteger = 12;
        Unbinder.unBind(r, myBigIntAsInteger, "myBigInt", noAnnotations);
        Map<String, String[]> r2 = fromUnbindMap2BindMap(r);
        RootParamNode root = ParamNode.convert(r2);
        assertThat(Binder.bind(root, "myBigInt", BigInteger.class, null, null)).isEqualTo(myBigInt);
    }
}


