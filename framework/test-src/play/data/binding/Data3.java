package play.data.binding;


import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Data3 {
    public String a;
    public Map<String, String> map = new HashMap<>();
    @As(binder = TestGenericTypeBinder.class)
    public List<GenericType<Long>> genericTypeList = new ArrayList<>();


    public static class GenericType<T> {
        T value;
    }

    public static class TestGenericTypeBinder implements TypeBinder<GenericType<?>> {

        @Override
        public Object bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            if (!Long.class.equals(pt.getActualTypeArguments()[0])) {
                throw new IllegalArgumentException("Wrong generic type passed. Does not match class declaration.");
            }

            Long longValue = Long.valueOf(value);
            GenericType<Long> gt = new GenericType<>();
            gt.value = longValue;
            return gt;
        }
    }
}
