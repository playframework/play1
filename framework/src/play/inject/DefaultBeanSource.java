package play.inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class DefaultBeanSource implements BeanSource {
  @Override
  public <T> T getBeanOfType(Class<T> clazz) {
    try {
      Constructor<T> constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      throw new RuntimeException("Cannot instantiate " + clazz, e);
    }
  }
}
