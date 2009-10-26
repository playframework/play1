package play.inject;

public interface BeanSource {
    
    public <T> T getBeanOfType(Class<T> clazz);

}
