package play.inject;

public interface BeanSource {
    
    <T> T getBeanOfType(Class<T> clazz);

}
