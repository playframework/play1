package play.data.binding.urlencoded;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class Binder extends play.data.binding.Binder {

    @Override
    public <T> T bindFromRequest(Class<T> type, Type gtype, Annotation[] annotations) {
        return null;
    }

}
